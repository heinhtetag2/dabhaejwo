package com.dabhaejwo.domain.auth.service;

import com.dabhaejwo.domain.auth.dto.request.ForgotPasswordRequest;
import com.dabhaejwo.domain.auth.dto.request.ResetPasswordRequest;
import com.dabhaejwo.domain.auth.entity.AuthScope;
import com.dabhaejwo.domain.member.entity.TenantMember;
import com.dabhaejwo.domain.member.repository.TenantMemberRepository;
import com.dabhaejwo.domain.operator.entity.Operator;
import com.dabhaejwo.domain.operator.repository.OperatorRepository;
import com.dabhaejwo.global.config.AppProperties;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.notify.Greeting;
import com.dabhaejwo.global.notify.Mailer;
import com.dabhaejwo.global.notify.MailTemplate;
import com.dabhaejwo.global.security.SlidingWindowLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

/**
 * 비밀번호 찾기.
 *
 * <p>흐름은 <b>메일 입력 → 임시 비밀번호 발송 → 임시 비밀번호로 본인 확인 → 새 비밀번호 설정</b>이다.
 *
 * <p>지켜야 할 것 셋:
 * <ul>
 *   <li>없는 이메일이어도 <b>같은 응답</b>을 준다 — 어떤 주소가 가입돼 있는지 알려주는 수단이 된다
 *   <li>임시 비밀번호는 <b>기존 비밀번호를 덮어쓴다</b> — 남겨 두면 재설정을 눌러도 옛 비밀번호가 산다
 *   <li>임시 비밀번호로는 <b>로그인이 끝나지 않는다</b> — 새 비밀번호를 정하는 화면으로만 간다
 * </ul>
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    /**
     * 임시 비밀번호 길이. 사람이 메일에서 옮겨 적으므로 헷갈리는 글자를 뺀 문자집합을 쓴다 —
     * O·0·I·l 이 섞이면 "비밀번호가 틀리다"는 문의가 그만큼 늘어난다.
     */
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int TEMP_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** 같은 주소로 메일 폭탄을 보내지 못하게 막는다. 시간당 3회면 실수로 여러 번 눌러도 충분하다. */
    private static final int SEND_PER_HOUR = 3;

    private final TenantMemberRepository memberRepository;
    private final OperatorRepository operatorRepository;
    private final PasswordEncoder passwordEncoder;
    private final Mailer mailer;
    private final AppProperties.Invite config;
    private final AppProperties.Mail mailConfig;
    private final SlidingWindowLimiter limiter = new SlidingWindowLimiter(Duration.ofHours(1));

    public PasswordResetService(TenantMemberRepository memberRepository,
                                OperatorRepository operatorRepository,
                                PasswordEncoder passwordEncoder,
                                Mailer mailer,
                                AppProperties properties) {
        this.memberRepository = memberRepository;
        this.operatorRepository = operatorRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailer = mailer;
        this.config = properties.invite();
        this.mailConfig = properties.mail();
    }

    /**
     * 임시 비밀번호를 메일로 보낸다.
     *
     * <p><b>계정이 없어도 예외를 던지지 않는다.</b> 응답이 갈리면 가입 여부를 확인하는 도구가 된다.
     */
    @Transactional
    public void forgot(AuthScope scope, ForgotPasswordRequest request) {
        String email = request.email().strip().toLowerCase(Locale.ROOT);

        if (!limiter.tryAcquire(scope + ":" + email, SEND_PER_HOUR)) {
            // 여기만은 거절한다. 조용히 넘기면 남의 메일함을 계속 두드릴 수 있다.
            throw new BusinessException(ErrorCode.RATE_LIMITED,
                    "재설정 메일을 너무 자주 요청했습니다. 잠시 후 다시 시도해 주세요");
        }

        String temporary = randomPassword();
        String hash = passwordEncoder.encode(temporary);
        String name;

        if (scope == AuthScope.OPS) {
            Optional<Operator> found = operatorRepository.findByEmail(email).filter(Operator::isActive);
            if (found.isEmpty()) {
                log.info("없는 운영자 주소로 비밀번호 찾기 요청 — 같은 응답을 돌려준다");
                return;
            }
            Operator operator = found.get();
            operator.issueTemporaryPassword(hash, config.tempPasswordTtlHours());
            name = operator.getName();
        } else {
            // 한 이메일이 여러 업체에 속할 수 있다. 첫 계정만 재설정한다 —
            // 전부 바꾸면 한 곳의 요청이 다른 업체 계정까지 잠근다 (IMPROVEMENTS 참조).
            Optional<TenantMember> found = memberRepository.findAllByEmail(email).stream().findFirst();
            if (found.isEmpty()) {
                log.info("없는 담당자 주소로 비밀번호 찾기 요청 — 같은 응답을 돌려준다");
                return;
            }
            TenantMember member = found.get();
            member.issueTemporaryPassword(hash, config.tempPasswordTtlHours());
            name = member.getName();
        }

        mailer.send(email, "[답해줘] 임시 비밀번호",
                forgotBody(scope, Greeting.of(name, email), temporary, config.tempPasswordTtlHours()));
    }

    /** 임시 비밀번호로 본인을 확인하고 새 비밀번호를 만든다. */
    @Transactional
    public void reset(AuthScope scope, ResetPasswordRequest request) {
        String email = request.email().strip().toLowerCase(Locale.ROOT);

        if (scope == AuthScope.OPS) {
            Operator operator = operatorRepository.findByEmail(email)
                    .filter(Operator::temporaryPasswordUsable)
                    .filter(o -> passwordEncoder.matches(request.temporaryPassword(), o.getPasswordHash()))
                    .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED,
                            "임시 비밀번호가 올바르지 않거나 만료되었습니다"));
            operator.changePassword(passwordEncoder.encode(request.newPassword()));
        } else {
            TenantMember member = memberRepository.findAllByEmail(email).stream()
                    .filter(TenantMember::temporaryPasswordUsable)
                    .filter(m -> passwordEncoder.matches(request.temporaryPassword(), m.getPasswordHash()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED,
                            "임시 비밀번호가 올바르지 않거나 만료되었습니다"));
            member.changePassword(passwordEncoder.encode(request.newPassword()));
        }
        log.info("비밀번호를 재설정했습니다 — scope={}", scope);
    }

    private String randomPassword() {
        StringBuilder out = new StringBuilder(TEMP_LENGTH);
        for (int i = 0; i < TEMP_LENGTH; i++) {
            out.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return out.toString();
    }

    private MailTemplate.Body forgotBody(AuthScope scope, String name, String temporary, int ttlHours) {
        return MailTemplate.build(
                name,
                "임시 비밀번호",
                "아래 임시 비밀번호로 새 비밀번호를 정하실 수 있습니다.",
                new MailTemplate.Highlight("임시 비밀번호", temporary, false),
                new MailTemplate.Cta("새 비밀번호 정하기", resetUrl(scope)),
                java.util.List.of(
                        "이 비밀번호는 " + ttlHours + "시간 동안만 쓸 수 있고, 새 비밀번호를 정하는 데에만 씁니다.",
                        "본인이 요청하지 않았다면 이 메일을 무시해 주세요. 다만 기존 비밀번호는 "
                                + "이미 이 임시 비밀번호로 바뀌었으므로, 직접 로그인하시려면 "
                                + "새 비밀번호를 정하셔야 합니다."));
    }

    /** 재설정 화면. 운영 콘솔과 업체 대시보드는 주소가 다르다. */
    private String resetUrl(AuthScope scope) {
        return (scope == AuthScope.OPS ? mailConfig.opsBaseUrl() : mailConfig.appBaseUrl())
                + "/forgot-password?step=reset";
    }
}
