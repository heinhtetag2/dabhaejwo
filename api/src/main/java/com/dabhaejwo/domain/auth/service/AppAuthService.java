package com.dabhaejwo.domain.auth.service;

import com.dabhaejwo.domain.auth.dto.request.AppLoginRequest;
import com.dabhaejwo.domain.auth.dto.request.OtpVerifyRequest;
import com.dabhaejwo.domain.auth.dto.response.AppLoginResponse;
import com.dabhaejwo.domain.auth.dto.response.OtpChallengeResponse;
import com.dabhaejwo.domain.auth.entity.AuthScope;
import com.dabhaejwo.domain.auth.dto.response.TokenResponse;
import com.dabhaejwo.domain.member.dto.response.MemberResponse;
import com.dabhaejwo.domain.member.entity.TenantMember;
import com.dabhaejwo.domain.member.repository.TenantMemberRepository;
import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.entity.TenantStatus;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.JwtProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 업체 담당자 로그인.
 *
 * <p>실패 사유를 구분해 응답하지 않는다 — 없는 이메일인지, 비밀번호가 틀렸는지,
 * 초대 수락 전인지를 알려주면 계정 존재 여부를 확인하는 수단이 된다. 전부 같은 응답이다.
 *
 * <p><b>두 단계다.</b> 비밀번호가 맞으면 토큰이 아니라 챌린지를 준다. 메일로 간 코드를
 * 맞혀야 토큰이 나온다 — 비밀번호 하나가 새면 계정이 통째로 넘어가는 구조를 없앤다.
 */
@Service
public class AppAuthService {

    /** 업체 담당자 리프레시 토큰의 scope. 운영자 토큰과 섞이지 않게 한다. */
    public static final String SCOPE_APP = "app";

    private final TenantMemberRepository memberRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final LoginChallengeService challengeService;

    public AppAuthService(TenantMemberRepository memberRepository,
                          TenantRepository tenantRepository,
                          PasswordEncoder passwordEncoder,
                          JwtProvider jwtProvider,
                          LoginChallengeService challengeService) {
        this.memberRepository = memberRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.challengeService = challengeService;
    }

    /**
     * 1단계 — 비밀번호를 확인하고 인증 코드를 메일로 보낸다.
     *
     * <p>여기서는 <b>토큰을 주지 않는다.</b>
     */
    @Transactional
    public OtpChallengeResponse login(AppLoginRequest request, String requesterIpHash) {
        TenantMember member = findByCredentials(request.email(), request.password());
        requireUsableTenant(member.getTenantId());

        // 임시 비밀번호로는 로그인을 끝내지 않는다. 새 비밀번호를 정하는 화면으로 보낸다 —
        // 그러지 않으면 메일로 보낸 임시값이 사실상 영구 비밀번호가 된다.
        if (member.isMustChangePassword()) {
            throw new BusinessException(ErrorCode.PASSWORD_CHANGE_REQUIRED);
        }

        return OtpChallengeResponse.of(
                challengeService.issue(AuthScope.APP, member.getId(), member.getEmail(),
                        member.getName(), requesterIpHash),
                member.getEmail());
    }

    /** 2단계 — 코드가 맞으면 토큰을 준다. */
    @Transactional
    public AppLoginResponse verifyOtp(OtpVerifyRequest request) {
        java.util.UUID memberId =
                challengeService.verify(AuthScope.APP, request.challengeId(), request.code());

        TenantMember member = memberRepository.findById(memberId)
                .filter(TenantMember::loginable)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
        requireUsableTenant(member.getTenantId());

        member.touchLastSeen();

        return new AppLoginResponse(
                jwtProvider.issueTenantToken(member.getId(), member.getTenantId(), member.getRole()),
                jwtProvider.issueRefreshToken(member.getId(), SCOPE_APP),
                MemberResponse.from(member));
    }

    /**
     * 액세스 토큰 재발급. scope 검사는 호출부({@code TokenRefreshService})가 이미 했다 —
     * 여기까지 온 subject 는 업체 담당자다.
     */
    @Transactional(readOnly = true)
    public TokenResponse refresh(JwtProvider.RefreshSubject subject) {
        TenantMember member = memberRepository.findById(subject.subjectId())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
        requireUsableTenant(member.getTenantId());

        return new TokenResponse(
                jwtProvider.issueTenantToken(member.getId(), member.getTenantId(), member.getRole()));
    }

    private TenantMember findByCredentials(String email, String rawPassword) {
        List<TenantMember> candidates = memberRepository.findAllByEmail(email);
        for (TenantMember member : candidates) {
            if (member.loginable() && passwordEncoder.matches(rawPassword, member.getPasswordHash())) {
                return member;
            }
        }
        throw new BusinessException(ErrorCode.UNAUTHENTICATED);
    }

    /**
     * 해지된 업체의 담당자는 로그인할 수 없다. 일시정지는 챗봇 응답만 멈추는 것이므로
     * 대시보드 접근은 허용한다 — 결제나 문의로 풀어야 하는데 못 들어오면 방법이 없다.
     */
    private void requireUsableTenant(java.util.UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND));
        if (tenant.getStatus() == TenantStatus.CHURNED) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
    }
}
