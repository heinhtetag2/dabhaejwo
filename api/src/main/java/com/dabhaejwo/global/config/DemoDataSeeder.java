package com.dabhaejwo.global.config;

import com.dabhaejwo.domain.billing.entity.BillingRecord;
import com.dabhaejwo.domain.billing.entity.BillingStatus;
import com.dabhaejwo.domain.billing.repository.BillingRecordRepository;
import com.dabhaejwo.domain.botsettings.entity.BotSettings;
import com.dabhaejwo.domain.botsettings.entity.PageScope;
import com.dabhaejwo.domain.botsettings.entity.WidgetPosition;
import com.dabhaejwo.domain.botsettings.repository.BotSettingsRepository;
import com.dabhaejwo.domain.conversation.entity.Conversation;
import com.dabhaejwo.domain.conversation.entity.Message;
import com.dabhaejwo.domain.conversation.repository.ConversationRepository;
import com.dabhaejwo.domain.conversation.repository.MessageRepository;
import com.dabhaejwo.domain.faq.entity.Faq;
import com.dabhaejwo.domain.faq.repository.FaqRepository;
import com.dabhaejwo.domain.gap.entity.AnswerGap;
import com.dabhaejwo.domain.gap.entity.GapReason;
import com.dabhaejwo.domain.gap.repository.AnswerGapRepository;
import com.dabhaejwo.domain.knowledge.entity.DocumentStatus;
import com.dabhaejwo.domain.knowledge.entity.KnowledgeDocument;
import com.dabhaejwo.domain.knowledge.entity.KnowledgeSource;
import com.dabhaejwo.domain.knowledge.entity.SourceType;
import com.dabhaejwo.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.dabhaejwo.domain.knowledge.repository.KnowledgeSourceRepository;
import com.dabhaejwo.domain.lead.entity.Lead;
import com.dabhaejwo.domain.lead.repository.LeadRepository;
import com.dabhaejwo.domain.member.entity.TenantMember;
import com.dabhaejwo.domain.member.repository.TenantMemberRepository;
import com.dabhaejwo.domain.plan.entity.Plan;
import com.dabhaejwo.domain.plan.repository.PlanRepository;
import com.dabhaejwo.domain.tenant.entity.AllowedOrigin;
import com.dabhaejwo.domain.tenant.repository.AllowedOriginRepository;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.domain.bot.service.BotProvisioner;
import com.dabhaejwo.global.security.BotScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 로컬 개발용 데모 데이터.
 *
 * <p>업체 대시보드 10개 화면이 "빈 화면"이 아닌 상태로 보이게 한다. 프로토타입
 * (docs/prototype/chatbot-tenant-dashboard.html)의 노르드하임 가구를 그대로 옮겼다.
 *
 * <p><b>{@code local} 프로파일에서만 돈다.</b> 운영에서 실행되면 없는 업체가 생긴다.
 * 이미 있으면 아무것도 하지 않으므로 재기동에 안전하다.
 *
 * <p>Flyway 마이그레이션에 넣지 않은 이유는, 마이그레이션은 모든 환경에서 실행되며
 * 되돌릴 수 없기 때문이다. 데모 데이터는 스키마가 아니다.
 */
@Configuration
@Profile("local")
public class DemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final String DEMO_DOMAIN = "nordheim.co.kr";
    private static final String DEMO_KEY = "pk_live_a8f3k2m9x7q1";
    /** 데모 계정 비밀번호. 로컬 전용이라 문서와 코드에 그대로 둔다. */
    private static final String DEMO_PASSWORD = "demo1234";

    @Bean
    ApplicationRunner seedDemoTenant(DemoSeedWriter writer) {
        return args -> writer.seed();
    }

    /** 트랜잭션 경계를 잡기 위한 별도 빈. ApplicationRunner 람다에는 @Transactional 이 안 걸린다. */
    @Bean
    DemoSeedWriter demoSeedWriter(TenantRepository tenantRepository,
                                  BotProvisioner botProvisioner,
                                  PlanRepository planRepository,
                                  TenantMemberRepository memberRepository,
                                  AllowedOriginRepository allowedOriginRepository,
                                  BotSettingsRepository botSettingsRepository,
                                  KnowledgeSourceRepository sourceRepository,
                                  KnowledgeDocumentRepository documentRepository,
                                  FaqRepository faqRepository,
                                  AnswerGapRepository gapRepository,
                                  ConversationRepository conversationRepository,
                                  MessageRepository messageRepository,
                                  LeadRepository leadRepository,
                                  BillingRecordRepository billingRepository,
                                  PasswordEncoder passwordEncoder,
                                  JdbcTemplate jdbcTemplate) {
        return new DemoSeedWriter(tenantRepository, botProvisioner, planRepository, memberRepository,
                allowedOriginRepository, botSettingsRepository, sourceRepository, documentRepository,
                faqRepository, gapRepository, conversationRepository, messageRepository,
                leadRepository, billingRepository, passwordEncoder, jdbcTemplate);
    }

    static class DemoSeedWriter {

        private final TenantRepository tenantRepository;
        private final BotProvisioner botProvisioner;
        private final PlanRepository planRepository;
        private final TenantMemberRepository memberRepository;
        private final AllowedOriginRepository allowedOriginRepository;
        private final BotSettingsRepository botSettingsRepository;
        private final KnowledgeSourceRepository sourceRepository;
        private final KnowledgeDocumentRepository documentRepository;
        private final FaqRepository faqRepository;
        private final AnswerGapRepository gapRepository;
        private final ConversationRepository conversationRepository;
        private final MessageRepository messageRepository;
        private final LeadRepository leadRepository;
        private final BillingRecordRepository billingRepository;
        private final PasswordEncoder passwordEncoder;
        private final JdbcTemplate jdbcTemplate;

        DemoSeedWriter(TenantRepository tenantRepository, BotProvisioner botProvisioner,
                       PlanRepository planRepository,
                       TenantMemberRepository memberRepository,
                       AllowedOriginRepository allowedOriginRepository,
                       BotSettingsRepository botSettingsRepository,
                       KnowledgeSourceRepository sourceRepository,
                       KnowledgeDocumentRepository documentRepository,
                       FaqRepository faqRepository, AnswerGapRepository gapRepository,
                       ConversationRepository conversationRepository,
                       MessageRepository messageRepository, LeadRepository leadRepository,
                       BillingRecordRepository billingRepository, PasswordEncoder passwordEncoder,
                       JdbcTemplate jdbcTemplate) {
            this.tenantRepository = tenantRepository;
            this.botProvisioner = botProvisioner;
            this.planRepository = planRepository;
            this.memberRepository = memberRepository;
            this.allowedOriginRepository = allowedOriginRepository;
            this.botSettingsRepository = botSettingsRepository;
            this.sourceRepository = sourceRepository;
            this.documentRepository = documentRepository;
            this.faqRepository = faqRepository;
            this.gapRepository = gapRepository;
            this.conversationRepository = conversationRepository;
            this.messageRepository = messageRepository;
            this.leadRepository = leadRepository;
            this.billingRepository = billingRepository;
            this.passwordEncoder = passwordEncoder;
            this.jdbcTemplate = jdbcTemplate;
        }

        @Transactional
        void seed() {
            if (tenantRepository.findByPublishableKey(DEMO_KEY).isPresent()) {
                return;
            }
            Plan business = planRepository.findByCode("BUSINESS").orElse(null);
            if (business == null) {
                log.warn("데모 시드를 건너뜁니다 — BUSINESS 요금제가 없습니다. V1 seed 가 적용됐는지 확인하세요.");
                return;
            }

            UUID tenantId = insertTenant(business.getId());
            // 실제 가입과 같은 경로로 만든다 — 시더가 손수 만들면 둘이 갈린다.
            BotScope scope = botProvisioner
                    .provision(tenantId, "노르드하임 가구", DEMO_DOMAIN, DEMO_KEY, true)
                    .scope();

            seedMembers(tenantId);
            seedOrigins(scope);
            botSettingsRepository.save(nordheimSettings(scope));
            seedKnowledge(scope);
            seedFaqs(scope);
            seedGaps(scope);
            seedConversations(scope);
            seedLeads(scope);
            seedBilling(tenantId, business.getMonthlyFee());
            seedDailyUsage(tenantId);

            log.info("데모 업체를 만들었습니다 — {} / 로그인 owner@{} · 비밀번호 {}",
                    DEMO_DOMAIN, DEMO_DOMAIN, DEMO_PASSWORD);
        }

        /**
         * Tenant 는 setter 가 없고 생성 팩토리도 운영 콘솔 흐름을 전제한다.
         * 데모 데이터를 위해 도메인에 구멍을 내지 않고 SQL 로 넣는다.
         */
        private UUID insertTenant(UUID planId) {
            UUID id = UUID.randomUUID();
            jdbcTemplate.update("""
                    INSERT INTO tenants (id, name, primary_domain, publishable_key, plan_id, status,
                                         currency, next_billing_date, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, 'ACTIVE', 'KRW', ?, now() - interval '208 days', now())
                    """, id, "노르드하임 가구", DEMO_DOMAIN, DEMO_KEY, planId,
                    LocalDate.now().withDayOfMonth(28));
            return id;
        }

        private void seedMembers(UUID tenantId) {
            String hash = passwordEncoder.encode(DEMO_PASSWORD);
            memberRepository.save(TenantMember.active(tenantId, "owner@" + DEMO_DOMAIN, "정OO",
                    com.dabhaejwo.global.security.TenantMemberRole.OWNER, hash));
            memberRepository.save(TenantMember.active(tenantId, "cs@" + DEMO_DOMAIN, "한OO",
                    com.dabhaejwo.global.security.TenantMemberRole.EDITOR, hash));
            // 초대만 보낸 상태 — 비밀번호가 없어 로그인되지 않는다. 팀원 화면의 "수락 대기" 행이다.
            memberRepository.save(TenantMember.invite(tenantId, "md@" + DEMO_DOMAIN, null,
                    com.dabhaejwo.global.security.TenantMemberRole.VIEWER));
        }

        private void seedOrigins(BotScope scope) {
            // 대표 도메인은 BotProvisioner 가 이미 등록했다. 프로토타입의 나머지 둘만 더한다 —
            // 한 서비스의 여러 접속 경로(쇼핑몰 서브도메인 · 스테이징)를 보여주기 위한 것이다.
            allowedOriginRepository.save(AllowedOrigin.of(scope, "shop." + DEMO_DOMAIN));
            allowedOriginRepository.save(AllowedOrigin.of(scope, "nordheim-test.vercel.app"));
        }

        private BotSettings nordheimSettings(BotScope scope) {
            BotSettings settings = BotSettings.defaults(scope, "노르드");
            settings.editAppearance("노르드 도우미", "#17222E", "안녕하세요! 가구 고르시는 것 도와드릴게요.");
            settings.editTone(
                    "노르드하임 가구의 상담 직원입니다. 정중한 존댓말을 쓰고, 답변은 세 문장 안으로 짧게 합니다. "
                            + "제품을 추천할 때는 반드시 사이트에 있는 제품만 이야기합니다.",
                    "제가 확인하기 어려운 내용이네요. 상담원에게 연결해 드릴까요?",
                    List.of("타사 브랜드 비교", "할인 협상", "재고 수량"));
            settings.editFallback(true, "1588-0000", false, "평일 09:00–18:00");
            settings.editPlacement(WidgetPosition.BOTTOM_RIGHT, PageScope.ALL, List.of(), 15, true,
                    com.dabhaejwo.domain.botsettings.entity.LauncherSize.MEDIUM,
                    com.dabhaejwo.domain.botsettings.entity.LauncherBackground.BRAND);
            return settings;
        }

        /**
         * 프로토타입 기준 문서 248개 = 웹 230 + 파일 14 + 직접입력 4.
         * 상태는 학습완료 231 / 처리중 12 / 실패 5 로 맞춘다.
         */
        private void seedKnowledge(BotScope scope) {
            KnowledgeSource web = sourceRepository.save(
                    KnowledgeSource.of(scope, SourceType.WEBSITE, DEMO_DOMAIN, true));
            KnowledgeSource file = sourceRepository.save(
                    KnowledgeSource.of(scope, SourceType.FILE, "업로드 파일", false));
            KnowledgeSource manual = sourceRepository.save(
                    KnowledgeSource.of(scope, SourceType.MANUAL, "직접 입력", false));
            web.markCrawled();

            String[] webTitles = {"배송 및 반품 안내", "오크 3인 소파 — 노르드 라인", "원목 관리 방법",
                    "매장 안내", "조립 서비스", "회사 소개", "자주 묻는 질문", "이용약관"};
            // 웹 216 + 파일 11 + 직접입력 4 = 학습완료 231, 처리중 10+2 = 12, 실패 4+1 = 5
            for (int i = 0; i < 230; i++) {
                DocumentStatus status = i < 216 ? DocumentStatus.INDEXED
                        : i < 226 ? DocumentStatus.PROCESSING : DocumentStatus.FAILED;
                KnowledgeDocument doc = KnowledgeDocument.of(scope, web.getId(),
                        webTitles[i % webTitles.length] + (i < webTitles.length ? "" : " " + i),
                        "/page/" + (i + 1), status);
                if (status == DocumentStatus.INDEXED) {
                    // 데모 문서는 실제 조각이 없다. 출처를 남겨 두면 뒤에 진짜 학습분과 구분된다.
                    doc.markIndexed(4 + (i % 9), "STUB", "demo-seed");
                } else if (status == DocumentStatus.FAILED) {
                    doc.markFailed("fetch_timeout");
                }
                documentRepository.save(doc);
            }
            for (int i = 0; i < 14; i++) {
                DocumentStatus status = i < 11 ? DocumentStatus.INDEXED
                        : i < 13 ? DocumentStatus.PROCESSING : DocumentStatus.FAILED;
                KnowledgeDocument doc = KnowledgeDocument.of(scope, file.getId(),
                        "2026_카탈로그_v" + (i + 1) + ".pdf", null, status);
                if (status == DocumentStatus.INDEXED) {
                    doc.markIndexed(20 + i, "STUB", "demo-seed");
                } else if (status == DocumentStatus.FAILED) {
                    doc.markFailed("pdf_parse_timeout");
                }
                documentRepository.save(doc);
            }
            String[] manualTitles = {"제주·도서 배송 정책", "A/S 접수 절차", "쿠폰 중복 사용 규정", "매장 주차 안내"};
            for (String title : manualTitles) {
                KnowledgeDocument doc = KnowledgeDocument.of(scope, manual.getId(), title, null,
                        DocumentStatus.INDEXED);
                doc.markIndexed(2, "STUB", "demo-seed");
                documentRepository.save(doc);
            }
        }

        private void seedFaqs(BotScope scope) {
            faqRepository.save(Faq.of(scope, "배송은 며칠 걸리나요?",
                    "주문 후 영업일 기준 5~7일 내 배송됩니다. 조립 설치를 함께 신청하시면 하루 정도 더 걸릴 수 있습니다.",
                    List.of("배송 및 반품 안내"), true, 1));
            faqRepository.save(Faq.of(scope, "소파 재질이 궁금해요",
                    "노르드 라인 소파는 북미산 화이트 오크 원목에 친환경 수성 도료를 사용합니다.",
                    List.of("오크 3인 소파 — 노르드 라인", "원목 관리 방법"), true, 2));
            faqRepository.save(Faq.of(scope, "매장 위치 알려주세요",
                    "서울 성동구 연무장길 00, 노르드하임 쇼룸입니다. 매일 11:00–20:00 운영합니다.",
                    List.of("매장 안내"), true, 3));
            faqRepository.save(Faq.of(scope, "조립 서비스 비용",
                    "조립 설치는 품목당 30,000원이며, 100만원 이상 주문 시 무료입니다.",
                    List.of("조립 서비스"), true, 4));
            faqRepository.save(Faq.of(scope, "반품 기간이 언제까지죠",
                    "수령일로부터 14일 이내 반품 가능합니다. 단순 변심은 왕복 배송비가 부과됩니다.",
                    List.of("배송 및 반품 안내"), false, 5));
            faqRepository.save(Faq.of(scope, "원목 관리는 어떻게 하나요",
                    "직사광선을 피하고 습도 40~60%를 유지해 주세요. 마른 천으로 결 방향을 따라 닦습니다.",
                    List.of("원목 관리 방법"), false, 6));
        }

        private void seedGaps(BotScope scope) {
            gapRepository.save(gap(scope, "제주도까지 배송되나요? 추가 비용 있어요?", GapReason.ANSWER_FAILED,
                    "/product/1204", "죄송합니다, 해당 내용은 확인이 어렵습니다. 매장으로 문의해 주세요.", 7));
            gapRepository.save(gap(scope, "A/S 신청은 어디서 하나요", GapReason.THUMBS_DOWN,
                    "/support", "고객센터를 통해 접수하실 수 있습니다.", 4));
            gapRepository.save(gap(scope, "지금 매장에 재고 있는지 확인 가능한가요", GapReason.ANSWER_FAILED,
                    "/", "재고 정보는 제공하고 있지 않습니다.", 6));
            gapRepository.save(gap(scope, "쿠폰 두 개 같이 쓸 수 있나요", GapReason.ANSWER_FAILED,
                    "/cart", "죄송합니다, 해당 내용은 확인이 어렵습니다.", 3));
        }

        private AnswerGap gap(BotScope scope, String question, GapReason reason, String path,
                              String botAnswer, int occurrences) {
            AnswerGap gap = AnswerGap.of(scope, question, reason, path, botAnswer);
            for (int i = 1; i < occurrences; i++) {
                gap.recur(question, path, botAnswer);
            }
            return gap;
        }

        private void seedConversations(BotScope scope) {
            String[][] scripts = {
                    {"서울", "/product/1204", "안녕하세요", "오크 3인 소파 지금 주문하면 언제 오나요?",
                            "제주도까지 배송되나요? 추가 비용 있어요?"},
                    {"부산", "/product/882", "오크 소파 폭이 몇 cm인가요"},
                    {"대구", "/guide/return", "반품하려면 어떻게 하나요"},
                    {"인천", "/support", "A/S 신청은 어디서 하나요"},
                    {"서울", "/store", "매장 주차 되나요?"},
            };
            for (String[] script : scripts) {
                Conversation conversation = conversationRepository.save(
                        Conversation.start(scope, script[1], script[0], "demo-hash"));
                for (int i = 2; i < script.length; i++) {
                    messageRepository.save(Message.fromVisitor(scope, conversation.getId(), script[i]));
                    boolean lastAndFailed = i == script.length - 1 && script[0].equals("서울")
                            && script[1].startsWith("/product");
                    messageRepository.save(Message.fromBot(scope, conversation.getId(),
                            lastAndFailed
                                    ? "죄송합니다, 해당 내용은 확인이 어렵습니다. 매장으로 문의해 주세요."
                                    : "네, 안내해 드리겠습니다.",
                            !lastAndFailed, false, null));
                }
            }
        }

        private void seedLeads(BotScope scope) {
            leadRepository.save(Lead.of(scope, null, "김OO", "010-2847-3391", "조립 서비스 신청 문의"));
            leadRepository.save(Lead.of(scope, null, "이OO", "010-5512-7712", "대량 구매 견적 요청"));
            Lead contacted = leadRepository.save(
                    Lead.of(scope, null, "박OO", "010-9930-2205", "제주 배송 관련"));
            contacted.changeStatus(com.dabhaejwo.domain.lead.entity.LeadStatus.CONTACTED);
        }

        /** 결제는 업체 단위다 — 서비스가 여럿이어도 청구는 하나다. */
        private void seedBilling(UUID tenantId, int monthlyFee) {
            LocalDate base = LocalDate.now().withDayOfMonth(1);
            billingRepository.save(BillingRecord.of(tenantId, base.minusMonths(1), monthlyFee, BillingStatus.PAID));
            billingRepository.save(BillingRecord.of(tenantId, base.minusMonths(2), monthlyFee, BillingStatus.PAID));
            billingRepository.save(BillingRecord.of(tenantId, base.minusMonths(3), 39000, BillingStatus.PAID));
        }

        /**
         * 이번 달 대화·원가 집계. 목록·홈이 tenant_daily_usage 를 읽으므로 여기 없으면 0 으로 보인다.
         * TenantDailyUsage 는 복합키 엔티티라 SQL 로 넣는다.
         */
        private void seedDailyUsage(UUID tenantId) {
            LocalDate today = LocalDate.now();
            LocalDate day = today.withDayOfMonth(1);
            while (!day.isAfter(today)) {
                int conv = 30 + (day.getDayOfMonth() * 7) % 90;
                jdbcTemplate.update("""
                        INSERT INTO tenant_daily_usage (tenant_id, day, conv_count, saved_count,
                                                        doc_count, tokens_in, tokens_out, cost_krw, aggregated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, now())
                        ON CONFLICT (tenant_id, day) DO NOTHING
                        """, tenantId, day, conv, conv / 3, 248, conv * 1200L, conv * 190L,
                        java.math.BigDecimal.valueOf(conv * 18.4));
                day = day.plusDays(1);
            }
        }
    }
}
