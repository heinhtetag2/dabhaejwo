package com.dabhaejwo.domain.chat.service;

import com.dabhaejwo.domain.guard.entity.CostGuard;
import com.dabhaejwo.domain.guard.repository.CostGuardRepository;
import com.dabhaejwo.domain.notification.service.NotificationEvents;
import com.dabhaejwo.domain.plan.entity.Plan;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.domain.usage.repository.AiUsageRepository;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.common.BusinessDay;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.SlidingWindowLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * 방문자 질문을 받기 전에 통과해야 하는 관문.
 *
 * <p>기획서 §4.7 — "안전장치는 선택이 아니라 필수". 챗봇은 <b>남이 트리거하는 원가</b>다.
 * 스크립트가 붙은 페이지에 봇이 몰리면 업체가 잠든 사이 하루치 예산이 날아간다.
 *
 * <p>막을 때는 <b>왜 막혔는지 구분되는 코드</b>를 준다. 전부 같은 오류로 뭉뚱그리면
 * 운영자가 한도를 올려야 할지 어뷰징을 막아야 할지 판단할 수 없다.
 */
@Component
public class ChatGuard {

    private static final Logger log = LoggerFactory.getLogger(ChatGuard.class);

    /** 전체 상한의 몇 %부터 미리 알릴지. 도달하고 나서 아는 건 이미 늦다. */
    private static final BigDecimal WARN_RATIO = new BigDecimal("0.8");

    private final CostGuardRepository costGuardRepository;
    private final AiUsageRepository usageRepository;
    private final TenantRepository tenantRepository;
    private final NotificationEvents notificationEvents;
    private final SlidingWindowLimiter perIpLimiter = new SlidingWindowLimiter(Duration.ofMinutes(1));

    public ChatGuard(CostGuardRepository costGuardRepository,
                     AiUsageRepository usageRepository,
                     TenantRepository tenantRepository,
                     NotificationEvents notificationEvents) {
        this.costGuardRepository = costGuardRepository;
        this.usageRepository = usageRepository;
        this.tenantRepository = tenantRepository;
        this.notificationEvents = notificationEvents;
    }

    public CostGuard settings() {
        return costGuardRepository.current();
    }

    /**
     * 한 IP 가 분당 던질 수 있는 질문 수.
     *
     * <p>IP 는 회사·학교에서 공유되므로 사람 단위가 아니다. 그래서 한도를 낮게 잡지 않는다 —
     * 어뷰징을 완벽히 막는 장치가 아니라 <b>폭주를 늦추는 장치</b>다. 진짜 상한은 원가 쪽이다.
     */
    public void requireWithinRate(String visitorIpHash, CostGuard guard) {
        if (!perIpLimiter.tryAcquire(visitorIpHash, guard.getIpQuestionsPerMin())) {
            throw new BusinessException(ErrorCode.RATE_LIMITED,
                    "질문이 너무 빠릅니다. 잠시 후 다시 물어봐 주세요");
        }
    }

    /**
     * 일일 원가 상한. 업체별과 전체를 <b>둘 다</b> 본다.
     *
     * <p>업체별만 보면 작은 업체 100곳이 동시에 한도까지 쓰는 상황을 못 막고,
     * 전체만 보면 한 업체가 전체 예산을 먹어도 개별로는 정상으로 보인다.
     *
     * <p>정확히는 "이미 쓴 금액"으로 판단하므로 상한을 살짝 넘겨서 멈춘다.
     * 호출 전에 원가를 알 수 없으니 어쩔 수 없고, 한 번의 초과분은 무시할 만하다.
     */
    public void requireWithinDailyCost(UUID tenantId, CostGuard guard) {
        // 상한도 업체의 하루를 따른다. UTC 로 끊으면 한국 시간 오전 9시에 초기화돼,
        // 업체는 "어제 한도가 찼는데 아침에도 안 풀린다"고 느낀다.
        OffsetDateTime from = BusinessDay.startOfToday();
        OffsetDateTime to = from.plusDays(1);

        BigDecimal tenantCost = usageRepository.sumCostKrw(tenantId, from, to);
        if (tenantCost.compareTo(BigDecimal.valueOf(guard.getTenantDailyCapKrw())) >= 0) {
            log.warn("업체 일일 원가 상한 도달 — tenant={}, 사용={}원, 상한={}원",
                    tenantId, tenantCost, guard.getTenantDailyCapKrw());
            // 이 요청은 곧 롤백된다. 알림을 같은 트랜잭션에 두면 "상한에 도달했다"는
            // 사실까지 되감겨 아무도 모르게 된다 — 그래서 분리된 트랜잭션으로 남긴다.
            notifyTenantCap(tenantId);
            throw new BusinessException(ErrorCode.COST_CAP_REACHED,
                    "오늘 이용 한도에 도달했습니다. 내일 다시 이용해 주세요");
        }

        BigDecimal globalCost = usageRepository.sumCostKrwAll(from, to);
        int globalCap = guard.getGlobalDailyCapKrw();
        if (globalCost.compareTo(BigDecimal.valueOf(globalCap)) >= 0) {
            // 전체 상한은 우리 쪽 사고다. 업체 잘못이 아니므로 로그 수준을 다르게 남긴다.
            log.error("전체 일일 원가 상한 도달 — 사용={}원, 상한={}원. 챗봇이 전 업체에서 멈춥니다",
                    globalCost, globalCap);
            notificationEvents.globalCostCap(100);
            throw new BusinessException(ErrorCode.COST_CAP_REACHED,
                    "일시적으로 답변할 수 없습니다. 잠시 후 다시 시도해 주세요");
        }
        // 아직 막지는 않지만 곧 닿는다. 상한에 부딪히고 나서 아는 것보다 낫다.
        if (globalCost.compareTo(BigDecimal.valueOf(globalCap).multiply(WARN_RATIO)) >= 0) {
            notificationEvents.globalCostCap(80);
        }
    }

    /** 업체명은 알림 문구에 쓰인다. 없으면 id 라도 남긴다 — 알림 자체를 포기하지 않는다. */
    private void notifyTenantCap(UUID tenantId) {
        String name = tenantRepository.findById(tenantId)
                .map(tenant -> tenant.getName())
                .orElse(tenantId.toString());
        notificationEvents.tenantCostCap(tenantId, name);
    }

    /**
     * 요금제 월 대화 한도.
     *
     * <p>넘었을 때의 동작은 운영자가 정한다({@code cost_guards.quota_exceeded_behavior}).
     * 기본은 중단 + 안내다 — 초과분을 조용히 받아주면 원가는 나가는데 매출은 안 늘고,
     * 업체는 자기가 한도를 넘긴 줄도 모른다.
     */
    public void requireWithinConversationQuota(long monthlyConversations, Plan plan, CostGuard guard) {
        if (monthlyConversations < plan.getConvLimit()) {
            return;
        }
        if (BEHAVIOR_ALLOW_OVERAGE.equals(guard.getQuotaExceededBehavior())) {
            log.info("대화 한도를 넘었지만 초과 허용 설정이라 계속 답합니다 — 사용={}, 한도={}",
                    monthlyConversations, plan.getConvLimit());
            return;
        }
        throw new BusinessException(ErrorCode.QUOTA_EXCEEDED,
                "이번 달 대화 한도를 모두 사용했습니다. 담당자에게 문의해 주세요");
    }

    /** {@code cost_guards.quota_exceeded_behavior} 값. 기본은 중단(STOP). */
    private static final String BEHAVIOR_ALLOW_OVERAGE = "ALLOW_OVERAGE";
}
