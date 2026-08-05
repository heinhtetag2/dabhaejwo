package com.dabhaejwo.domain.tenant.service;

import com.dabhaejwo.domain.billing.entity.BillingRecord;
import com.dabhaejwo.domain.billing.repository.BillingRecordRepository;
import com.dabhaejwo.domain.operator.service.OperatorLookupService;
import com.dabhaejwo.domain.tenant.dto.response.TenantActivityResponse;
import com.dabhaejwo.domain.tenant.entity.TenantNote;
import com.dabhaejwo.domain.tenant.repository.TenantNoteRepository;
import com.dabhaejwo.global.audit.AuditLog;
import com.dabhaejwo.global.audit.AuditLogRepository;
import com.dabhaejwo.global.common.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 업체 활동 이력. 감사 기록·결제 기록·내부 메모를 시각 역순으로 합친 <b>합성 뷰</b>다.
 *
 * <p>전용 테이블을 두지 않는 이유는 같은 사실을 두 곳에 쓰게 되기 때문이다. 두 벌이 되면
 * 언젠가 갈라지고, 갈라진 뒤에는 어느 쪽이 맞는지 알 수 없다.
 */
@Service
public class TenantActivityService {

    private final AuditLogRepository auditLogRepository;
    private final BillingRecordRepository billingRecordRepository;
    private final TenantNoteRepository noteRepository;
    private final OperatorLookupService operatorLookup;

    public TenantActivityService(AuditLogRepository auditLogRepository,
                                 BillingRecordRepository billingRecordRepository,
                                 TenantNoteRepository noteRepository,
                                 OperatorLookupService operatorLookup) {
        this.auditLogRepository = auditLogRepository;
        this.billingRecordRepository = billingRecordRepository;
        this.noteRepository = noteRepository;
        this.operatorLookup = operatorLookup;
    }

    @Transactional(readOnly = true)
    public PageResponse<TenantActivityResponse> list(UUID tenantId, int page, Integer size) {
        List<AuditLog> audits = auditLogRepository.findTop100ByTenantIdOrderByCreatedAtDesc(tenantId);
        List<BillingRecord> billings = billingRecordRepository.findAllByTenantIdOrderByPeriodDesc(tenantId);
        List<TenantNote> notes = noteRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId);

        Map<UUID, String> names = operatorLookup.namesOf(mergeOperatorIds(audits, notes));

        List<TenantActivityResponse> merged = new ArrayList<>();
        audits.forEach(audit -> merged.add(new TenantActivityResponse(
                "audit-" + audit.getId(),
                TenantActivityResponse.Type.valueOf(audit.getAction().name()),
                audit.getCreatedAt(),
                summaryOf(audit),
                audit.getReason(),
                new TenantActivityResponse.OperatorRef(
                        audit.getOperatorId(),
                        operatorLookup.nameOf(names, audit.getOperatorId())))));

        billings.forEach(billing -> merged.add(new TenantActivityResponse(
                "billing-" + billing.getId(),
                TenantActivityResponse.Type.PAYMENT,
                billing.getCreatedAt(),
                billing.getPeriod() + " " + billing.getAmount() + "원 " + billing.getStatus(),
                billing.getFailureReason(),
                // 결제는 시스템이 만든 항목이라 행위자가 없다.
                null)));

        notes.forEach(note -> merged.add(new TenantActivityResponse(
                "note-" + note.getId(),
                TenantActivityResponse.Type.NOTE,
                note.getCreatedAt(),
                note.getBody(),
                null,
                new TenantActivityResponse.OperatorRef(
                        note.getOperatorId(),
                        operatorLookup.nameOf(names, note.getOperatorId())))));

        merged.sort(java.util.Comparator.comparing(TenantActivityResponse::at).reversed());

        int pageSize = PageResponse.clampSize(size);
        int pageNumber = Math.max(page, 0);
        int from = Math.min(pageNumber * pageSize, merged.size());
        int to = Math.min(from + pageSize, merged.size());

        return new PageResponse<>(
                List.copyOf(merged.subList(from, to)),
                new PageResponse.PageInfo(pageNumber, pageSize, merged.size(),
                        (int) Math.ceil((double) merged.size() / pageSize)));
    }

    private List<UUID> mergeOperatorIds(List<AuditLog> audits, List<TenantNote> notes) {
        List<UUID> ids = new ArrayList<>(audits.stream().map(AuditLog::getOperatorId).toList());
        ids.addAll(notes.stream().map(TenantNote::getOperatorId).toList());
        return ids;
    }

    /**
     * 한 줄 요약. 감사 기록의 {@code meta} 에 담긴 변경 전후를 사람이 읽을 수 있게 편다.
     * meta 는 행위마다 키가 다르므로 없는 키는 조용히 건너뛴다.
     */
    private String summaryOf(AuditLog audit) {
        Map<String, Object> meta = audit.getMeta();
        return switch (audit.getAction()) {
            case CHANGE_PLAN -> meta.get("from") + " → " + meta.get("to");
            case GRANT_QUOTA -> "대화 +" + meta.getOrDefault("convDelta", 0)
                    + " · 문서 +" + meta.getOrDefault("docDelta", 0);
            case EXTEND_TRIAL -> "체험 " + meta.getOrDefault("days", 0) + "일 연장";
            default -> audit.getAction().name();
        };
    }
}
