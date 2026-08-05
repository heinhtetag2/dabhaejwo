package com.dabhaejwo.domain.billing.controller;

import com.dabhaejwo.domain.billing.dto.request.TicketStatusRequest;
import com.dabhaejwo.domain.billing.dto.response.TicketResponse;
import com.dabhaejwo.domain.billing.entity.TicketStatus;
import com.dabhaejwo.domain.billing.service.TicketOpsService;
import com.dabhaejwo.global.common.PageResponse;
import com.dabhaejwo.global.security.Permission;
import com.dabhaejwo.global.security.RequirePermission;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/ops/tickets")
public class TicketOpsController {

    private final TicketOpsService service;

    public TicketOpsController(TicketOpsService service) {
        this.service = service;
    }

    /** 정렬은 경과 시간 내림차순 고정이다. 파라미터로 열지 않는다. */
    @GetMapping
    @RequirePermission(Permission.TICKET_READ)
    public PageResponse<TicketResponse> list(@RequestParam(required = false) TicketStatus status,
                                             @RequestParam(required = false) UUID tenantId,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(required = false) Integer size) {
        return service.list(status, tenantId, page, size);
    }

    @PatchMapping("/{ticketId}")
    @RequirePermission(Permission.TICKET_WRITE)
    public TicketResponse changeStatus(@PathVariable Long ticketId,
                                       @Valid @RequestBody TicketStatusRequest request) {
        return service.changeStatus(ticketId, request);
    }
}
