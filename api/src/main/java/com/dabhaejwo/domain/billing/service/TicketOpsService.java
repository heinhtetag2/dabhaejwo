package com.dabhaejwo.domain.billing.service;

import com.dabhaejwo.domain.billing.dto.request.TicketStatusRequest;
import com.dabhaejwo.domain.billing.dto.response.TicketResponse;
import com.dabhaejwo.domain.billing.entity.Ticket;
import com.dabhaejwo.domain.billing.entity.TicketStatus;
import com.dabhaejwo.domain.billing.repository.TicketRepository;
import com.dabhaejwo.domain.operator.service.OperatorLookupService;
import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.global.audit.AuditAction;
import com.dabhaejwo.global.audit.AuditLogService;
import com.dabhaejwo.global.common.PageResponse;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.AuthPrincipal;
import com.dabhaejwo.global.security.CurrentAuth;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TicketOpsService {

    private final TicketRepository ticketRepository;
    private final TenantRepository tenantRepository;
    private final OperatorLookupService operatorLookup;
    private final AuditLogService auditLogService;

    public TicketOpsService(TicketRepository ticketRepository,
                            TenantRepository tenantRepository,
                            OperatorLookupService operatorLookup,
                            AuditLogService auditLogService) {
        this.ticketRepository = ticketRepository;
        this.tenantRepository = tenantRepository;
        this.operatorLookup = operatorLookup;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> list(TicketStatus status, UUID tenantId, int page, Integer size) {
        Page<Ticket> tickets = ticketRepository.search(status, tenantId,
                PageRequest.of(Math.max(page, 0), PageResponse.clampSize(size)));
        return PageResponse.of(tickets, ticket -> toResponse(ticket, names(tickets.getContent()),
                operatorNames(tickets.getContent())));
    }

    @Transactional(readOnly = true)
    public long openCount() {
        return ticketRepository.countByStatus(TicketStatus.OPEN);
    }

    @Transactional
    public TicketResponse changeStatus(Long ticketId, TicketStatusRequest request) {
        AuthPrincipal.Operator operator = CurrentAuth.operator();
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_NOT_FOUND));

        ticket.changeStatus(request.status(), operator.operatorId());

        auditLogService.record(operator.operatorId(), AuditAction.TICKET_WRITE, ticket.getTenantId(), "",
                Map.of("ticketId", ticketId, "status", request.status().name()));

        List<Ticket> one = List.of(ticket);
        return toResponse(ticket, names(one), operatorNames(one));
    }

    private Map<UUID, String> names(List<Ticket> tickets) {
        return tenantRepository
                .findAllById(tickets.stream().map(Ticket::getTenantId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Tenant::getId, Tenant::getName));
    }

    private Map<UUID, String> operatorNames(List<Ticket> tickets) {
        return operatorLookup.namesOf(
                tickets.stream().map(Ticket::getAnsweredBy).filter(java.util.Objects::nonNull).toList());
    }

    private TicketResponse toResponse(Ticket ticket,
                                      Map<UUID, String> tenantNames,
                                      Map<UUID, String> operatorNames) {
        return TicketResponse.of(
                ticket,
                new TicketResponse.TenantRef(ticket.getTenantId(),
                        tenantNames.getOrDefault(ticket.getTenantId(), "(삭제된 업체)")),
                ticket.getAnsweredBy() == null ? null
                        : new TicketResponse.OperatorRef(ticket.getAnsweredBy(),
                                operatorLookup.nameOf(operatorNames, ticket.getAnsweredBy())));
    }
}
