package com.dabhaejwo.domain.audit.service;

import com.dabhaejwo.domain.audit.dto.response.AuditLogResponse;
import com.dabhaejwo.domain.operator.service.OperatorLookupService;
import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.global.audit.AuditAction;
import com.dabhaejwo.global.audit.AuditLog;
import com.dabhaejwo.global.audit.AuditLogRepository;
import com.dabhaejwo.global.common.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 감사 기록 조회. <b>조회만 있다.</b>
 *
 * <p>운영 콘솔은 남의 고객 데이터를 볼 수 있는 도구다. 누가 언제 왜 접근했는지가
 * 지워질 수 있으면 이 화면은 아무 의미가 없다 (admin-console-plan.md §4.10).
 */
@Service
public class AuditQueryService {

    /** 기간을 안 줬을 때의 기본 범위. 프로토타입의 "최근 30일"과 같은 값이다. */
    private static final int DEFAULT_RANGE_DAYS = 30;

    private final AuditLogRepository auditLogRepository;
    private final TenantRepository tenantRepository;
    private final OperatorLookupService operatorLookup;

    public AuditQueryService(AuditLogRepository auditLogRepository,
                             TenantRepository tenantRepository,
                             OperatorLookupService operatorLookup) {
        this.auditLogRepository = auditLogRepository;
        this.tenantRepository = tenantRepository;
        this.operatorLookup = operatorLookup;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(UUID tenantId,
                                               UUID operatorId,
                                               AuditAction action,
                                               OffsetDateTime from,
                                               OffsetDateTime to,
                                               int page,
                                               Integer size) {
        // 기간을 안 주면 최근 30일이다. 화면 기본값이자 성능 방어선이다 —
        // 3년치를 전 구간 훑는 조회를 기본 동작으로 두지 않는다.
        OffsetDateTime toAt = to == null ? OffsetDateTime.now().plusDays(1) : to;
        OffsetDateTime fromAt = from == null ? toAt.minusDays(DEFAULT_RANGE_DAYS) : from;

        Page<AuditLog> logs = auditLogRepository.search(tenantId, operatorId, action, fromAt, toAt,
                PageRequest.of(Math.max(page, 0), PageResponse.clampSize(size)));

        List<AuditLog> content = logs.getContent();
        Map<UUID, String> operatorNames = operatorLookup.namesOf(
                content.stream().map(AuditLog::getOperatorId).toList());
        Map<UUID, String> tenantNames = tenantRepository
                .findAllById(content.stream()
                        .map(AuditLog::getTenantId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(Tenant::getId, Tenant::getName));

        return PageResponse.of(logs, log -> AuditLogResponse.of(
                log,
                operatorLookup.nameOf(operatorNames, log.getOperatorId()),
                log.getTenantId() == null ? null
                        : tenantNames.getOrDefault(log.getTenantId(), "(삭제된 업체)")));
    }
}
