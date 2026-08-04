package com.dabhaejwo.domain.auth.service;

import com.dabhaejwo.domain.auth.dto.request.AppLoginRequest;
import com.dabhaejwo.domain.auth.dto.response.AppLoginResponse;
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
 */
@Service
public class AppAuthService {

    /** 업체 담당자 리프레시 토큰의 scope. 운영자 토큰과 섞이지 않게 한다. */
    static final String SCOPE_APP = "app";

    private final TenantMemberRepository memberRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AppAuthService(TenantMemberRepository memberRepository,
                          TenantRepository tenantRepository,
                          PasswordEncoder passwordEncoder,
                          JwtProvider jwtProvider) {
        this.memberRepository = memberRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    @Transactional
    public AppLoginResponse login(AppLoginRequest request) {
        TenantMember member = findByCredentials(request.email(), request.password());
        requireUsableTenant(member.getTenantId());

        member.touchLastSeen();

        return new AppLoginResponse(
                jwtProvider.issueTenantToken(member.getId(), member.getTenantId(), member.getRole()),
                jwtProvider.issueRefreshToken(member.getId(), SCOPE_APP),
                MemberResponse.from(member));
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(String refreshToken) {
        JwtProvider.RefreshSubject subject = jwtProvider.parseRefresh(refreshToken);
        if (!SCOPE_APP.equals(subject.scope())) {
            // 운영자 리프레시 토큰으로 업체 액세스 토큰을 받아내는 경로를 막는다.
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }

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
