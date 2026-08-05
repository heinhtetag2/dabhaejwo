package com.dabhaejwo.domain.auth.service;

import com.dabhaejwo.domain.auth.dto.request.OpsLoginRequest;
import com.dabhaejwo.domain.auth.dto.request.OtpVerifyRequest;
import com.dabhaejwo.domain.auth.dto.response.OpsLoginResponse;
import com.dabhaejwo.domain.auth.dto.response.OtpChallengeResponse;
import com.dabhaejwo.domain.auth.entity.AuthScope;
import com.dabhaejwo.domain.auth.dto.response.TokenResponse;
import com.dabhaejwo.domain.operator.dto.response.OperatorResponse;
import com.dabhaejwo.domain.operator.entity.Operator;
import com.dabhaejwo.domain.operator.repository.OperatorRepository;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.JwtProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영자 로그인.
 *
 * <p>기획서 §8 은 SSO + 2FA 를 요구한다. SSO 는 아직이지만 <b>2단계 인증은 붙었다</b> —
 * 메일 OTP 다. 운영 콘솔은 전 업체의 데이터를 볼 수 있어 업체 계정보다 위험이 크다.
 *
 * <p>실패 사유를 구분해 응답하지 않는다 — 없는 이메일인지, 비밀번호가 틀렸는지,
 * 비활성 계정인지 알려주면 계정 존재 여부를 확인하는 수단이 된다. 전부 같은 응답이다.
 */
@Service
public class OpsAuthService {

    /** 운영자 리프레시 토큰의 scope. 업체 담당자 토큰과 섞이지 않게 한다. */
    public static final String SCOPE_OPS = "ops";

    private final OperatorRepository operatorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final LoginChallengeService challengeService;

    public OpsAuthService(OperatorRepository operatorRepository,
                          PasswordEncoder passwordEncoder,
                          JwtProvider jwtProvider,
                          LoginChallengeService challengeService) {
        this.operatorRepository = operatorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.challengeService = challengeService;
    }

    /** 1단계 — 비밀번호를 확인하고 인증 코드를 메일로 보낸다. 토큰은 아직 없다. */
    @Transactional
    public OtpChallengeResponse login(OpsLoginRequest request, String requesterIpHash) {
        Operator operator = operatorRepository.findByEmail(request.email())
                .filter(Operator::loginable)
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));

        if (operator.isMustChangePassword()) {
            throw new BusinessException(ErrorCode.PASSWORD_CHANGE_REQUIRED);
        }

        return OtpChallengeResponse.of(
                challengeService.issue(AuthScope.OPS, operator.getId(), operator.getEmail(),
                        operator.getName(), requesterIpHash),
                operator.getEmail());
    }

    /** 2단계 — 코드가 맞으면 토큰을 준다. */
    @Transactional
    public OpsLoginResponse verifyOtp(OtpVerifyRequest request) {
        java.util.UUID operatorId =
                challengeService.verify(AuthScope.OPS, request.challengeId(), request.code());

        Operator operator = operatorRepository.findById(operatorId)
                .filter(Operator::loginable)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));

        operator.touchLastSeen();

        return new OpsLoginResponse(
                jwtProvider.issueOperatorToken(operator.getId(), operator.getName(), operator.getRole()),
                jwtProvider.issueRefreshToken(operator.getId(), SCOPE_OPS),
                OperatorResponse.from(operator));
    }

    /**
     * 액세스 토큰 재발급. 역할은 <b>토큰이 아니라 DB 에서 다시 읽는다</b> —
     * 권한을 낮춘 운영자가 옛 리프레시 토큰으로 옛 권한을 계속 받아가면 안 된다.
     */
    @Transactional(readOnly = true)
    public TokenResponse refresh(JwtProvider.RefreshSubject subject) {
        Operator operator = operatorRepository.findById(subject.subjectId())
                .filter(Operator::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));

        return new TokenResponse(
                jwtProvider.issueOperatorToken(operator.getId(), operator.getName(), operator.getRole()));
    }
}
