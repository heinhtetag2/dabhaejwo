package com.dabhaejwo.domain.impersonation.service;

import com.dabhaejwo.domain.impersonation.dto.request.ImpersonationRequest;
import com.dabhaejwo.domain.impersonation.dto.response.ImpersonationSessionResponse;
import com.dabhaejwo.domain.impersonation.entity.ImpersonationSession;
import com.dabhaejwo.domain.impersonation.repository.ImpersonationSessionRepository;
import com.dabhaejwo.domain.notification.service.NotificationEvents;
import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.entity.TenantStatus;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.global.audit.AuditAction;
import com.dabhaejwo.global.audit.AuditLogService;
import com.dabhaejwo.global.config.AppProperties;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.AuthPrincipal;
import com.dabhaejwo.global.security.CurrentAuth;
import com.dabhaejwo.global.security.JwtProvider;
import com.dabhaejwo.domain.bot.entity.Bot;
import com.dabhaejwo.domain.bot.repository.BotRepository;
import com.dabhaejwo.domain.bot.service.BotContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 대리 로그인. <b>편의보다 통제를 우선한다</b> — 남의 고객 데이터를 그대로 열람하는 행위다.
 *
 * <ol>
 *   <li>사유 없이는 실행되지 않는다. 공백만 입력해도 거부한다</li>
 *   <li>진입 즉시 감사 기록에 남고, 그 기록은 지울 수 없다</li>
 *   <li>세션은 30분이며 연장하려면 사유를 다시 입력한다</li>
 * </ol>
 *
 * <p>발급되는 토큰의 역할은 VIEWER 다. 운영자가 설정을 바꾸는 사고를 토큰 수준에서 막는다.
 */
@Service
public class ImpersonationOpsService {

    private final ImpersonationSessionRepository sessionRepository;
    private final TenantRepository tenantRepository;
    private final BotRepository botRepository;
    private final BotContext botContext;
    private final JwtProvider jwtProvider;
    private final AuditLogService auditLogService;
    private final NotificationEvents notificationEvents;
    private final AppProperties.Auth authProperties;

    public ImpersonationOpsService(ImpersonationSessionRepository sessionRepository,
                                   TenantRepository tenantRepository,
                                   JwtProvider jwtProvider,
                                   AuditLogService auditLogService,
                                   NotificationEvents notificationEvents,
                                   AppProperties properties,
                                   BotRepository botRepository,
                                   BotContext botContext) {
        this.sessionRepository = sessionRepository;
        this.tenantRepository = tenantRepository;
        this.botRepository = botRepository;
        this.botContext = botContext;
        this.jwtProvider = jwtProvider;
        this.auditLogService = auditLogService;
        this.notificationEvents = notificationEvents;
        this.authProperties = properties.auth();
    }

    @Transactional
    public ImpersonationSessionResponse start(UUID tenantId, ImpersonationRequest request) {
        AuthPrincipal.Operator operator = CurrentAuth.operator();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND));
        if (tenant.getStatus() == TenantStatus.CHURNED) {
            // 해지된 업체의 데이터는 유예 기간 뒤 삭제된다. 그 사이에도 들여다볼 이유가 없다.
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION,
                    "해지된 업체에는 대리 접속할 수 없습니다");
        }

        /*
         * 들어갈 서비스를 먼저 정한다. 지목하지 않았으면 기본 서비스다.
         *
         * **감사 기록에는 실제로 들어간 서비스를 남긴다** — 업체가 서비스를 여럿 운영하면
         * "이 업체에 들어갔다"만으로는 무엇을 봤는지 알 수 없다. 이 테이블은 3년 보존·
         * 수정 불가라 나중에 채울 수 없다.
         */
        Bot bot = request.botId() == null
                ? botContext.defaultBot(tenantId)
                : botRepository.findByIdAndTenantId(request.botId(), tenantId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.BOT_NOT_FOUND));

        ImpersonationSession session = sessionRepository.save(ImpersonationSession.start(
                tenantId, operator.operatorId(), request.reason().strip(),
                authProperties.impersonationTtlMinutes()));

        auditLogService.record(operator.operatorId(), AuditAction.IMPERSONATE, tenantId,
                request.reason(), Map.of("impersonationSessionId", session.getId().toString(),
                        "botId", bot.getId().toString(),
                        "botName", bot.getName()));

        // 사후 이력보다 실시간 알림이 훨씬 강한 신뢰 장치다 (tenant-plan.md §6.3).
        // 업체는 우리가 들어온 사실을 나중에 찾아보는 게 아니라 그 순간 안다.
        notificationEvents.impersonationStarted(tenantId, session.getReason());

        return ImpersonationSessionResponse.of(session, tenantRef(tenant),
                jwtProvider.issueImpersonationToken(session.getId(), operator.operatorId(), tenantId),
                // 경로를 서버가 만든다 — 두 프론트가 각자 규칙을 적으면 갈린다.
                "/app/s/" + bot.getId());
    }

    /**
     * 연장. 사유를 다시 받는 이유는 30분이 지나도록 무엇을 하고 있었는지가 새 사실이기 때문이다.
     * 새 사유로 감사 기록이 한 줄 더 남는다.
     */
    @Transactional
    public ImpersonationSessionResponse extend(UUID sessionId, ImpersonationRequest request) {
        AuthPrincipal.Operator operator = CurrentAuth.operator();
        ImpersonationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMPERSONATION_NOT_FOUND));

        if (!session.active(OffsetDateTime.now())) {
            // 끝난 세션은 되살리지 않는다. 다시 사유를 적고 새로 시작하게 한다 —
            // 되살리면 한 사유로 임의로 긴 접속이 가능해진다.
            throw new BusinessException(ErrorCode.IMPERSONATION_EXPIRED);
        }

        Tenant tenant = tenantRepository.findById(session.getTenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND));

        session.extend(request.reason().strip(), authProperties.impersonationTtlMinutes());

        auditLogService.record(operator.operatorId(), AuditAction.IMPERSONATE, session.getTenantId(),
                request.reason(), Map.of(
                        "impersonationSessionId", session.getId().toString(),
                        "extended", true));

        // 연장은 보던 화면을 이어 보는 것이다 — 진입 경로를 다시 주지 않는다.
        return ImpersonationSessionResponse.of(session, tenantRef(tenant),
                jwtProvider.issueImpersonationToken(session.getId(), operator.operatorId(),
                        session.getTenantId()),
                null);
    }

    /** 종료. 이미 끝난 세션을 또 끝내도 오류가 아니다 — 처음 끝난 시각이 그대로 남는다. */
    @Transactional
    public ImpersonationSessionResponse end(UUID sessionId) {
        ImpersonationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMPERSONATION_NOT_FOUND));
        Tenant tenant = tenantRepository.findById(session.getTenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND));

        session.end();

        // 종료에는 토큰을 주지 않는다. 응답을 그대로 다시 쓰면 끝난 세션으로 접속하게 된다.
        return ImpersonationSessionResponse.of(session, tenantRef(tenant), null, null);
    }

    private ImpersonationSessionResponse.TenantRef tenantRef(Tenant tenant) {
        return new ImpersonationSessionResponse.TenantRef(tenant.getId(), tenant.getName());
    }
}
