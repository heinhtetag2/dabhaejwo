package com.dabhaejwo.domain.member.controller;

import com.dabhaejwo.domain.billing.dto.response.PlanOverviewResponse;
import com.dabhaejwo.domain.billing.service.PlanOverviewService;
import com.dabhaejwo.domain.impersonation.dto.response.ImpersonationHistoryResponse;
import com.dabhaejwo.domain.impersonation.repository.ImpersonationSessionRepository;
import com.dabhaejwo.domain.member.dto.request.MemberInviteRequest;
import com.dabhaejwo.domain.member.dto.request.MemberRoleRequest;
import com.dabhaejwo.domain.member.dto.response.MemberResponse;
import com.dabhaejwo.domain.member.service.MemberService;
import com.dabhaejwo.global.security.CurrentAuth;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** 계정 화면 — 요금제 · 팀원 · 운영팀 접속 이력. */
@RestController
@RequestMapping("/api/app")
public class MemberController {

    private final MemberService memberService;
    private final PlanOverviewService planOverviewService;
    private final ImpersonationSessionRepository sessionRepository;

    public MemberController(MemberService memberService,
                            PlanOverviewService planOverviewService,
                            ImpersonationSessionRepository sessionRepository) {
        this.memberService = memberService;
        this.planOverviewService = planOverviewService;
        this.sessionRepository = sessionRepository;
    }

    @GetMapping("/plan")
    public PlanOverviewResponse plan() {
        return planOverviewService.overview();
    }

    @GetMapping("/members")
    public List<MemberResponse> members() {
        return memberService.list();
    }

    @PostMapping("/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse invite(@Valid @RequestBody MemberInviteRequest request) {
        return memberService.invite(request);
    }

    @PatchMapping("/members/{id}")
    public MemberResponse changeRole(@PathVariable UUID id,
                                     @Valid @RequestBody MemberRoleRequest request) {
        return memberService.changeRole(id, request);
    }

    @DeleteMapping("/members/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID id) {
        memberService.remove(id);
    }

    /**
     * 운영팀 접속 이력. 업체에게 <b>공개</b>한다 (tenant-plan.md §6.3).
     *
     * <p>숨기는 편이 편하지만, 공개하는 쪽이 신뢰 확보에 유리하고 개인정보 처리방침
     * 고지 의무에도 부합한다. 전 역할이 볼 수 있다 — 감출 이유가 없다.
     */
    @GetMapping("/impersonation/history")
    public List<ImpersonationHistoryResponse> impersonationHistory() {
        UUID tenantId = CurrentAuth.tenantUser().tenantId();
        return sessionRepository.findAllByTenantIdOrderByStartedAtDesc(tenantId).stream()
                .map(ImpersonationHistoryResponse::from)
                .toList();
    }
}
