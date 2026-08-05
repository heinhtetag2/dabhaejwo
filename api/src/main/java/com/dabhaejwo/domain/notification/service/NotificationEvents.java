package com.dabhaejwo.domain.notification.service;

import com.dabhaejwo.domain.notification.entity.NotificationType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 알림 문구와 이동 경로를 만드는 곳.
 *
 * <p>발행 지점(가입 서비스·챗 가드·워커…)에 문구가 흩어지면 같은 사건을 두 곳에서
 * 다르게 부르게 된다. 트리거는 "무슨 일이 있었다"만 알리고 <b>문구는 여기서 한 번에</b> 만든다.
 *
 * <p>{@code targetPath} 는 프론트 라우트다. 누르면 바로 그 대상에 도달해야 한다 —
 * 이름만 확인하고 다시 검색하게 만들지 않는다 (admin-console-plan.md §4.1).
 */
@Service
public class NotificationEvents {

    private final NotificationPublisher publisher;

    public NotificationEvents(NotificationPublisher publisher) {
        this.publisher = publisher;
    }

    // ── 운영자 ──────────────────────────────────────────────

    public void tenantSignedUp(UUID tenantId, String tenantName, String planName) {
        publisher.publish(NotificationType.TENANT_SIGNED_UP, null,
                tenantName + " 님이 가입했습니다",
                planName + "으로 시작했습니다",
                "/tenants?tenantId=" + tenantId,
                // 가입은 매번 새로운 사건이라 중복 억제를 걸지 않는다.
                null);
    }

    public void ticketOpened(UUID tenantId, String tenantName, long ticketId, String subject) {
        publisher.publish(NotificationType.TICKET_OPENED, null,
                tenantName + " 의 새 문의가 접수됐습니다",
                subject,
                "/tickets",
                "TICKET:" + ticketId);
    }

    /**
     * 전체 일일 원가가 상한에 근접했다.
     *
     * <p>{@code publishDetached} 인 이유 — 100% 도달은 요청을 거절하며 트랜잭션이 롤백된다.
     * 같이 되감기면 "상한에 도달했다"는 사실까지 사라져 아무도 모르게 된다.
     */
    public void globalCostCap(int percent) {
        publisher.publishDetached(NotificationType.GLOBAL_COST_CAP_WARNING, null,
                "오늘 전체 원가가 상한의 " + percent + "% 입니다",
                percent >= 100
                        ? "상한에 도달해 전 업체의 챗봇이 멈췄습니다"
                        : "이대로 가면 오늘 안에 상한에 닿습니다",
                "/ai-usage",
                "GLOBAL_COST_" + percent + ":" + LocalDate.now());
    }

    public void tenantCostCap(UUID tenantId, String tenantName) {
        publisher.publishDetached(NotificationType.TENANT_COST_CAP_REACHED, null,
                tenantName + " 이 일일 원가 상한에 도달했습니다",
                "그 업체의 챗봇이 안내 메시지만 표시하고 멈췄습니다",
                "/tenants?tenantId=" + tenantId,
                "TENANT_COST_CAP:" + tenantId + ":" + LocalDate.now());
    }

    public void tenantCostExceeded(UUID tenantId, String tenantName, int ratioPercent) {
        publisher.publish(NotificationType.TENANT_COST_EXCEEDED, null,
                tenantName + " 이 원가 초과로 돌아섰습니다",
                "이번 달 원가율 " + ratioPercent + "% — 쓸수록 적자입니다",
                "/profitability",
                "COST_EXCEEDED:" + tenantId + ":" + LocalDate.now().withDayOfMonth(1));
    }

    public void trialEndingForOps(UUID tenantId, String tenantName, int daysLeft) {
        publisher.publish(NotificationType.TRIAL_ENDING_SOON, null,
                tenantName + " 의 체험이 " + daysLeft + "일 뒤 끝납니다",
                "전환 안내가 필요한 시점입니다",
                "/tenants?tenantId=" + tenantId,
                "TRIAL_OPS:" + tenantId + ":" + LocalDate.now());
    }

    public void indexingFailuresForOps(long failedCount) {
        publisher.publish(NotificationType.INDEXING_FAILURES, null,
                "문서 학습이 " + failedCount + "건 실패했습니다",
                "업체가 학습을 기다리고 있을 수 있습니다",
                "/jobs",
                "INDEX_FAIL:" + LocalDate.now());
    }

    // ── 업체 ────────────────────────────────────────────────

    /**
     * 운영팀이 대리 접속했다.
     *
     * <p>사후 이력보다 실시간 알림이 훨씬 강한 신뢰 장치다 (tenant-plan.md §6.3).
     * 사유를 그대로 보여준다 — 업체에게 공개되는 문구라는 전제로 받은 값이다.
     */
    public void impersonationStarted(UUID tenantId, String reason) {
        publisher.publish(NotificationType.IMPERSONATION_STARTED, tenantId,
                "운영팀이 접속했습니다",
                "사유: " + reason,
                "/app/team",
                null);
    }

    public void quotaWarning(UUID tenantId, int percent, long used, int limit) {
        boolean exhausted = percent >= 100;
        publisher.publish(
                exhausted ? NotificationType.QUOTA_EXHAUSTED : NotificationType.QUOTA_WARNING,
                tenantId,
                exhausted
                        ? "이번 달 대화 한도를 다 쓰셨습니다"
                        : "이번 달 대화의 " + percent + "% 를 쓰셨습니다",
                used + " / " + limit + " 건" + (exhausted ? " — 챗봇이 멈췄습니다" : ""),
                "/app/plan",
                "QUOTA_" + percent + ":" + tenantId + ":" + LocalDate.now().withDayOfMonth(1));
    }

    public void trialEndingForTenant(UUID tenantId, int daysLeft) {
        publisher.publish(NotificationType.TRIAL_ENDING, tenantId,
                "무료 체험이 " + daysLeft + "일 뒤 끝납니다",
                "계속 쓰시려면 요금제를 선택해 주세요",
                "/app/plan",
                "TRIAL_TENANT:" + tenantId + ":" + LocalDate.now());
    }

    public void indexingDone(UUID tenantId, String title, int chunkCount) {
        publisher.publish(NotificationType.INDEXING_DONE, tenantId,
                "문서 학습이 끝났습니다",
                title + " — 이제 이 내용으로 답할 수 있습니다 (" + chunkCount + "조각)",
                "/app/sources",
                null);
    }

    public void indexingFailed(UUID tenantId, String title, String errorCode) {
        publisher.publish(NotificationType.INDEXING_FAILED, tenantId,
                "문서 학습에 실패했습니다",
                title + " — " + describe(errorCode),
                "/app/sources",
                null);
    }

    public void leadReceived(UUID tenantId, String name) {
        publisher.publish(NotificationType.LEAD_RECEIVED, tenantId,
                "새 연락처가 도착했습니다",
                name + " 님이 연락처를 남겼습니다",
                "/app/leads",
                null);
    }

    public void answerGapsPiling(UUID tenantId, long openCount) {
        publisher.publish(NotificationType.ANSWER_GAPS_PILING, tenantId,
                "챗봇이 답하지 못한 질문이 " + openCount + "건 쌓였습니다",
                "공통 질문으로 등록하면 다음부터 바로 답합니다",
                "/app/improve",
                "GAPS:" + tenantId + ":" + LocalDate.now());
    }

    /** 오류 코드는 업체에게 그대로 보여주지 않는다 — 업체는 `pdf_no_text` 를 모른다. */
    private String describe(String errorCode) {
        return switch (errorCode == null ? "" : errorCode) {
            case "pdf_no_text" -> "글자가 없는 스캔 문서입니다. 텍스트가 있는 파일로 올려주세요";
            case "no_text_found" -> "읽을 수 있는 글자가 없습니다";
            case "parse_failed" -> "파일을 여는 데 실패했습니다. 형식을 확인해 주세요";
            case "no_original_file" -> "원본 파일을 찾지 못했습니다. 다시 올려주세요";
            default -> "잠시 후 다시 학습해 주세요";
        };
    }
}
