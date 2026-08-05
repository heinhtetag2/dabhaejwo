package com.dabhaejwo.domain.chat.service;

import com.dabhaejwo.domain.botsettings.entity.BotSettings;
import com.dabhaejwo.domain.botsettings.repository.BotSettingsRepository;
import com.dabhaejwo.domain.chat.dto.request.AskRequest;
import com.dabhaejwo.domain.chat.dto.request.FeedbackRequest;
import com.dabhaejwo.domain.chat.dto.request.LeadRequest;
import com.dabhaejwo.domain.chat.dto.response.AnswerResponse;
import com.dabhaejwo.domain.chat.dto.response.WidgetSessionResponse;
import com.dabhaejwo.domain.conversation.entity.Conversation;
import com.dabhaejwo.domain.conversation.entity.Message;
import com.dabhaejwo.domain.conversation.entity.MessageFeedback;
import com.dabhaejwo.domain.conversation.repository.ConversationRepository;
import com.dabhaejwo.domain.conversation.repository.MessageFeedbackRepository;
import com.dabhaejwo.domain.conversation.repository.MessageRepository;
import com.dabhaejwo.domain.faq.entity.Faq;
import com.dabhaejwo.domain.faq.repository.FaqRepository;
import com.dabhaejwo.domain.gap.entity.AnswerGap;
import com.dabhaejwo.domain.gap.entity.GapReason;
import com.dabhaejwo.domain.gap.repository.AnswerGapRepository;
import com.dabhaejwo.domain.guard.entity.CostGuard;
import com.dabhaejwo.domain.lead.entity.Lead;
import com.dabhaejwo.domain.lead.repository.LeadRepository;
import com.dabhaejwo.domain.notification.service.NotificationEvents;
import com.dabhaejwo.domain.plan.entity.Plan;
import com.dabhaejwo.domain.plan.repository.PlanRepository;
import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * 방문자 위젯의 진입점.
 *
 * <p>{@link AnswerService} 가 "답을 어떻게 만드나"라면 여기는 "누가 물어도 되나, 기록은 어디에
 * 남나"다. 안전장치·대화 관리·저장 답변 경로가 여기 있다.
 *
 * <p><b>저장 답변(공통 질문)은 모델을 거치지 않는다.</b> 이건 최적화가 아니라 제품의 약속이다 —
 * 업체가 직접 쓴 답은 정확하고 즉시 나가고 원가가 0이다. 가장 많이 묻는 질문일수록
 * 이 경로로 빠지므로, 여기가 무너지면 원가 구조 전체가 무너진다.
 */
@Service
public class WidgetChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MessageFeedbackRepository feedbackRepository;
    private final FaqRepository faqRepository;
    private final BotSettingsRepository botSettingsRepository;
    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;
    private final LeadRepository leadRepository;
    private final AnswerGapRepository gapRepository;
    private final AnswerService answerService;
    private final ChatGuard guard;
    private final NotificationEvents notificationEvents;

    public WidgetChatService(ConversationRepository conversationRepository,
                             MessageRepository messageRepository,
                             MessageFeedbackRepository feedbackRepository,
                             FaqRepository faqRepository,
                             BotSettingsRepository botSettingsRepository,
                             TenantRepository tenantRepository,
                             PlanRepository planRepository,
                             LeadRepository leadRepository,
                             AnswerGapRepository gapRepository,
                             AnswerService answerService,
                             ChatGuard guard,
                             NotificationEvents notificationEvents) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.feedbackRepository = feedbackRepository;
        this.faqRepository = faqRepository;
        this.botSettingsRepository = botSettingsRepository;
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
        this.leadRepository = leadRepository;
        this.gapRepository = gapRepository;
        this.answerService = answerService;
        this.guard = guard;
        this.notificationEvents = notificationEvents;
    }

    /**
     * 위젯이 열렸다. 대화를 만들고 첫 화면에 필요한 것을 한 번에 준다.
     *
     * <p>대화는 <b>패널을 열 때</b> 만든다. 질문할 때 만들면 "열어보고 안 물어본" 방문자가
     * 통계에서 사라지는데, 업체가 알아야 할 것은 그쪽이 더 많다.
     */
    @Transactional
    public WidgetSessionResponse start(UUID tenantId, String path, String visitorIpHash) {
        Tenant tenant = activeTenant(tenantId);
        BotSettings settings = settings(tenantId, tenant.getName());

        Conversation conversation = conversationRepository.save(
                Conversation.start(tenantId, path, null, visitorIpHash));

        List<WidgetSessionResponse.WidgetFaq> faqs =
                faqRepository.findAllByTenantIdAndShownTrueOrderBySortOrderAsc(tenantId).stream()
                        .map(faq -> new WidgetSessionResponse.WidgetFaq(faq.getId(), faq.getQuestion()))
                        .toList();

        return new WidgetSessionResponse(
                conversation.getId(), settings.getBotName(), settings.getGreeting(),
                settings.getBrandColor(), settings.getWidgetPosition(),
                settings.isLeadCaptureEnabled(), faqs);
    }

    /**
     * 자유 질문. 관문을 전부 통과해야 모델까지 간다.
     *
     * <p>순서가 중요하다 — <b>싼 검사부터</b>. 레이트 리밋(메모리) → 대화 한도(집계 1회)
     * → 원가 상한(집계 2회). 비싼 검사를 앞에 두면 봇이 두드릴 때마다 DB 를 긁는다.
     */
    @Transactional
    public AnswerResponse ask(UUID tenantId, AskRequest request, String visitorIpHash) {
        Tenant tenant = activeTenant(tenantId);
        CostGuard settings = guard.settings();

        guard.requireWithinRate(visitorIpHash, settings);
        Conversation conversation = conversation(tenantId, request.sessionId());

        Plan plan = planRepository.findById(tenant.getPlanId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));
        guard.requireWithinConversationQuota(monthlyConversations(tenantId), plan, settings);
        guard.requireWithinDailyCost(tenantId, settings);

        return answerService.answer(tenantId, conversation.getId(),
                request.question(), request.path(), settings);
    }

    /**
     * 저장 답변. <b>모델을 거치지 않고 {@code ai_usage} 에도 남지 않는다.</b>
     *
     * <p>안전장치도 원가 상한만 뺀다 — 여기서는 원가가 0이므로 막을 이유가 없다.
     * 레이트 리밋은 유지한다. 저장 답변이라도 DB 쓰기는 일어난다.
     */
    @Transactional
    public AnswerResponse answerFaq(UUID tenantId, UUID faqId, UUID sessionId, String visitorIpHash) {
        activeTenant(tenantId);
        guard.requireWithinRate(visitorIpHash, guard.settings());

        Conversation conversation = conversation(tenantId, sessionId);
        Faq faq = faqRepository.findByIdAndTenantId(faqId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FAQ_NOT_FOUND));

        faq.hit();

        // 저장 답변도 순서를 못 박는다 — 같은 순간에 찍으면 답이 질문보다 위에 나온다.
        OffsetDateTime askedAt = OffsetDateTime.now();
        messageRepository.save(
                Message.fromVisitor(tenantId, conversation.getId(), faq.getQuestion(), askedAt));
        Message bot = Message.fromBot(tenantId, conversation.getId(), faq.getAnswer(),
                true, true, faq.getId(), askedAt.plusNanos(1_000_000L));
        messageRepository.saveAndFlush(bot);

        return new AnswerResponse(true, true, faq.getAnswer(), faq.getLinks(), bot.getId());
    }

    /**
     * 👍👎.
     *
     * <p>👎 는 답변 개선 목록으로 올린다. 답변 실패({@code ANSWER_FAILED})와 이유를 구분해
     * 담는다 — 못 답한 것과 틀리게 답한 것은 업체가 취할 조치가 다르다.
     */
    @Transactional
    public void feedback(UUID tenantId, FeedbackRequest request) {
        Message message = messageRepository.findById(request.messageId())
                .filter(found -> found.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND));

        feedbackRepository.findById(message.getId())
                .ifPresentOrElse(
                        existing -> existing.change(request.helpful()),
                        () -> feedbackRepository.save(
                                MessageFeedback.of(message.getId(), tenantId, request.helpful())));

        if (Boolean.TRUE.equals(request.helpful())) {
            return;
        }
        // 이미 "못 답했다"고 기록된 답변이다. 여기서 또 올리면 한 번 물은 질문이 두 번으로 세어져
        // 업체가 보는 "몇 번 놓쳤나"가 부풀어 오른다. 👎 는 새 정보를 주지 않는다.
        if (Boolean.FALSE.equals(message.getAnswered())) {
            return;
        }
        questionBefore(tenantId, message).ifPresent(question ->
                gapRepository.findByTenantIdAndQuestionNorm(tenantId, AnswerGap.normalize(question))
                        .ifPresentOrElse(
                                gap -> gap.recur(question, null, message.getContent()),
                                () -> gapRepository.save(AnswerGap.of(tenantId, question,
                                        GapReason.THUMBS_DOWN, null, message.getContent()))));
    }

    /** 연락처 남기기. 답변 실패 뒤 위젯이 제안하는 경로다. */
    @Transactional
    public UUID lead(UUID tenantId, LeadRequest request) {
        activeTenant(tenantId);
        Conversation conversation = conversation(tenantId, request.sessionId());

        Lead lead = leadRepository.save(Lead.of(tenantId, conversation.getId(),
                request.name(), request.contact(), request.memo()));

        // 연락처는 시간이 지나면 값이 떨어진다. 방문자가 남긴 그 순간 업체가 알아야 한다.
        notificationEvents.leadReceived(tenantId, lead.getName());
        return lead.getId();
    }

    /** 이 봇 메시지 바로 앞의 방문자 질문. 👎 가 무엇에 대한 것인지 알아야 개선 목록에 올린다. */
    private java.util.Optional<String> questionBefore(UUID tenantId, Message botMessage) {
        List<Message> messages = messageRepository
                .findAllByTenantIdAndConversationIdOrderByCreatedAtAsc(tenantId, botMessage.getConversationId());
        String question = null;
        for (Message message : messages) {
            if (message.getId().equals(botMessage.getId())) {
                break;
            }
            if (message.getRole() == com.dabhaejwo.domain.conversation.entity.MessageRole.VISITOR) {
                question = message.getContent();
            }
        }
        return java.util.Optional.ofNullable(question);
    }

    /**
     * 이번 달 대화 수. 요금제 한도와 비교한다.
     *
     * <p>실시간 집계다 — {@code tenant_daily_usage} 는 시간당 배치라 최대 한 시간이 늦다.
     * 한도 판정을 늦은 값으로 하면 그 사이에 한도를 넘겨 쓴다.
     */
    private long monthlyConversations(UUID tenantId) {
        OffsetDateTime from = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1)
                .atStartOfDay().atOffset(ZoneOffset.UTC);
        return conversationRepository.countByTenantIdAndStartedAtBetween(tenantId, from, from.plusMonths(1));
    }

    private Conversation conversation(UUID tenantId, UUID sessionId) {
        return conversationRepository.findByIdAndTenantId(sessionId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
    }

    private BotSettings settings(UUID tenantId, String tenantName) {
        return botSettingsRepository.findById(tenantId)
                .orElseGet(() -> BotSettings.defaults(tenantId, tenantName));
    }

    /**
     * 해지·정지된 업체의 위젯은 답하지 않는다.
     *
     * <p>키가 살아 있다고 계속 답하면 돈을 안 내는 사이트에서 원가가 계속 나간다.
     */
    private Tenant activeTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND));
        if (!tenant.getStatus().servesVisitors()) {
            throw new BusinessException(ErrorCode.TENANT_INACTIVE,
                    "현재 상담을 이용할 수 없습니다");
        }
        return tenant;
    }
}
