package com.dabhaejwo.domain.audit.controller;

import com.dabhaejwo.domain.audit.dto.response.AuditLogResponse;
import com.dabhaejwo.domain.audit.service.AuditQueryService;
import com.dabhaejwo.global.audit.AuditAction;
import com.dabhaejwo.global.common.PageResponse;
import com.dabhaejwo.global.security.Permission;
import com.dabhaejwo.global.security.RequirePermission;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 감사 기록. <b>쓰기·삭제 엔드포인트가 존재하지 않는다.</b>
 *
 * <p>열람 권한은 {@code OPS_ADMIN} 만 갖는다 (admin-console-plan.md §7).
 */
@RestController
@RequestMapping("/api/ops/audit-logs")
public class AuditOpsController {

    private final AuditQueryService service;

    public AuditOpsController(AuditQueryService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePermission(Permission.AUDIT_READ)
    public PageResponse<AuditLogResponse> list(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) UUID operatorId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size) {
        return service.list(tenantId, operatorId, action, from, to, page, size);
    }
}
