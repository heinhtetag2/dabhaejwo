package com.dabhaejwo.domain.member.service;

import com.dabhaejwo.domain.member.dto.request.MemberInviteRequest;
import com.dabhaejwo.domain.member.dto.request.MemberRoleRequest;
import com.dabhaejwo.domain.member.dto.response.MemberResponse;
import com.dabhaejwo.domain.member.entity.TenantMember;
import com.dabhaejwo.domain.auth.service.InviteService;
import com.dabhaejwo.global.notify.Greeting;
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
    private final InviteService inviteService;

    public MemberService(TenantMemberRepository memberRepository, InviteService inviteService) {
        this.memberRepository = memberRepository;
        this.inviteService = inviteService;
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> list() {
        UUID tenantId = CurrentAuth.tenantUser().tenantId();
        return memberRepository.findAllByTenantIdOrderByCreatedAtAsc(tenantId).stream()
                .map(MemberResponse::from)
                .toList();
    }

    /**
     * 초대. 비밀번호 없이 PENDING 으로 만들고 <b>메일로 수락 링크를 보낸다.</b>
     *
     * <p>메일 발송이 실패하면 같은 트랜잭션이라 팀원 행도 함께 사라진다. 행만 남기면
     * 초대한 사람은 상대가 링크를 못 받은 사실을 모른 채 기다린다.
     */
    @Transactional
    public MemberResponse invite(MemberInviteRequest request) {
        AuthPrincipal.TenantUser user = CurrentAuth.requireOwner();
        CurrentAuth.rejectIfImpersonating();

        String email = request.email().strip().toLowerCase(Locale.ROOT);
        if (memberRepository.existsByTenantIdAndEmail(user.tenantId(), email)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "이미 등록된 이메일입니다");
        }

        TenantMember member = memberRepository.save(TenantMember.invite(
                user.tenantId(), email, request.name(), request.role(), request.phone()));

        inviteService.sendInvite(member, inviterName(user));
        return MemberResponse.from(member);
    }

    /**
     * 초대 메일 다시 보내기. 이전 링크는 무효가 된다 — 토큰 해시를 덮어쓰기 때문이다.
     *
     * <p>이미 수락한 팀원에게는 보내지 않는다. 보내면 비밀번호를 다시 정할 수 있는
     * 링크가 되어, 소유자가 남의 계정을 가로챌 수 있는 통로가 된다.
     */
    @Transactional
    public MemberResponse resendInvite(UUID memberId) {
        AuthPrincipal.TenantUser user = CurrentAuth.requireOwner();
        CurrentAuth.rejectIfImpersonating();

        TenantMember member = find(memberId, user.tenantId());
        if (member.getInviteState() != com.dabhaejwo.domain.member.entity.InviteState.PENDING) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "이미 수락한 팀원입니다. 비밀번호를 잊었다면 로그인 화면의 비밀번호 찾기를 이용해 주세요");
        }
        inviteService.sendInvite(member, inviterName(user));
        return MemberResponse.from(member);
    }

    /**
     * 초대 메일에 "누가 불렀는지"를 적는다. 모르는 곳에서 온 메일처럼 보이지 않게.
     *
     * <p>소유자의 이름은 <b>null 일 수 있다</b> — 가입 화면이 이름을 받지 않는다.
     * 그때는 이메일 앞부분을 쓴다.
     */
    private String inviterName(AuthPrincipal.TenantUser user) {
        return memberRepository.findById(user.memberId())
                .map(member -> Greeting.of(member.getName(), member.getEmail()))
                .orElse("담당자");
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
