package com.dabhaejwo.domain.member.service;

import com.dabhaejwo.domain.member.dto.request.MemberInviteRequest;
import com.dabhaejwo.domain.member.dto.request.MemberRoleRequest;
import com.dabhaejwo.domain.member.dto.response.MemberResponse;
import com.dabhaejwo.domain.member.entity.TenantMember;
import com.dabhaejwo.domain.member.repository.TenantMemberRepository;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.AuthPrincipal;
import com.dabhaejwo.global.security.CurrentAuth;
import com.dabhaejwo.global.security.TenantMemberRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 팀원 관리.
 *
 * <p>초대·삭제·권한 변경은 <b>소유자만</b> 할 수 있다 (tenant-plan.md §8).
 * 대리 접속 중에도 금지된다 — 운영자가 업체 팀 구성을 바꾸는 일은 없어야 한다.
 */
@Service
public class MemberService {

    private static final Logger log = LoggerFactory.getLogger(MemberService.class);

    private final TenantMemberRepository memberRepository;

    public MemberService(TenantMemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> list() {
        UUID tenantId = CurrentAuth.tenantUser().tenantId();
        return memberRepository.findAllByTenantIdOrderByCreatedAtAsc(tenantId).stream()
                .map(MemberResponse::from)
                .toList();
    }

    /**
     * 초대. 비밀번호 없이 PENDING 으로 만든다 — 수락 링크로 본인이 정한다.
     *
     * <p>TODO(stub): 메일 발송이 미연동이라 초대 메일이 나가지 않는다. 조용히 성공시키지 않고
     * 로그로 남긴다. 지금은 행이 생기는 것까지가 전부다.
     */
    @Transactional
    public MemberResponse invite(MemberInviteRequest request) {
        AuthPrincipal.TenantUser user = CurrentAuth.requireOwner();
        CurrentAuth.rejectIfImpersonating();

        String email = request.email().strip().toLowerCase(Locale.ROOT);
        if (memberRepository.existsByTenantIdAndEmail(user.tenantId(), email)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "이미 등록된 이메일입니다");
        }

        TenantMember member = memberRepository.save(
                TenantMember.invite(user.tenantId(), email, request.name(), request.role()));
        log.warn("초대 메일이 발송되지 않았습니다 — Mailer 미연동 (tenant={}, email={})",
                user.tenantId(), email);
        return MemberResponse.from(member);
    }

    @Transactional
    public MemberResponse changeRole(UUID memberId, MemberRoleRequest request) {
        AuthPrincipal.TenantUser user = CurrentAuth.requireOwner();
        CurrentAuth.rejectIfImpersonating();
        TenantMember member = find(memberId, user.tenantId());

        if (member.getRole() == TenantMemberRole.OWNER) {
            // 소유자를 강등하면 아무도 팀원을 관리할 수 없는 상태가 만들어질 수 있다.
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "소유자의 권한은 바꿀 수 없습니다");
        }
        if (request.role() == TenantMemberRole.OWNER) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "소유자 권한 이전은 아직 지원하지 않습니다");
        }
        member.changeRole(request.role());
        return MemberResponse.from(member);
    }

    @Transactional
    public void remove(UUID memberId) {
        AuthPrincipal.TenantUser user = CurrentAuth.requireOwner();
        CurrentAuth.rejectIfImpersonating();
        TenantMember member = find(memberId, user.tenantId());

        if (member.getRole() == TenantMemberRole.OWNER) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "소유자는 삭제할 수 없습니다");
        }
        memberRepository.delete(member);
    }

    private TenantMember find(UUID memberId, UUID tenantId) {
        return memberRepository.findByIdAndTenantId(memberId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
