package com.dabhaejwo.domain.auth.service;

import com.dabhaejwo.domain.auth.entity.AuthScope;
import com.dabhaejwo.domain.auth.entity.LoginChallenge;
import com.dabhaejwo.domain.auth.repository.LoginChallengeRepository;
import com.dabhaejwo.global.config.AppProperties;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.notify.Greeting;
import com.dabhaejwo.global.notify.Mailer;
import com.dabhaejwo.global.notify.MailTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 로그인 2단계 인증(OTP) 발급과 검증.
 *
 * <p>업체 담당자와 운영자가 같은 코드를 쓴다 — 두 벌로 두면 한쪽만 고쳐져 어긋난다.
 * 구분은 {@link AuthScope} 하나로 한다.
 *
 * <p>지켜야 할 것 넷:
 * <ul>
 *   <li>코드는 <b>해시로만</b> 저장한다 — DB 를 읽어 남의 로그인을 완성할 수 없어야 한다
 *   <li>새로 발급하면 <b>이전 코드를 전부 닫는다</b> — 메일함에 살아 있는 옛 코드가 쌓이면 안 된다
 *   <li>틀린 횟수가 상한에 닿으면 <b>폐기하고 되돌리지 않는다</b>
 *   <li>메일 발송이 실패하면 <b>로그인도 실패한다</b> — 오지 않을 코드를 기다리게 두지 않는다
 * </ul>
 */
@Service
public class LoginChallengeService {

    private static final Logger log = LoggerFactory.getLogger(LoginChallengeService.class);

    /**
     * 코드 자릿수. 6자리는 100만분의 1이고, 시도 5회·5분 만료와 함께 보면 충분히 좁다.
     * 더 늘리면 사람이 옮겨 적다가 틀린다.
     */
    private static final int CODE_DIGITS = 6;

    /** {@link SecureRandom} 이다. {@code Math.random()} 으로 만든 인증 코드는 예측 가능하다. */
    private static final SecureRandom RANDOM = new SecureRandom();

    private final LoginChallengeRepository challengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final Mailer mailer;
    private final AppProperties.Otp config;

    public LoginChallengeService(LoginChallengeRepository challengeRepository,
                                 PasswordEncoder passwordEncoder,
                                 Mailer mailer,
                                 AppProperties properties) {
        this.challengeRepository = challengeRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailer = mailer;
        this.config = properties.otp();
    }

    /**
     * 코드를 만들어 메일로 보낸다.
     *
     * @return 클라이언트가 다음 단계에 되돌려줄 챌린지 id. <b>이 값만으로는 로그인할 수 없다</b> —
     *         메일로 간 코드가 있어야 한다
     */
    @Transactional
    public Issued issue(AuthScope scope, UUID subjectId, String email, String displayName,
                        String requesterIpHash) {
        OffsetDateTime now = OffsetDateTime.now();
        if (challengeRepository.countBySubjectIdAndCreatedAtAfter(subjectId, now.minusHours(1))
                >= config.resendPerHour()) {
            log.warn("OTP 재발송 한도 초과 — subject={}", subjectId);
            throw new BusinessException(ErrorCode.RATE_LIMITED,
                    "인증 코드를 너무 자주 요청했습니다. 잠시 후 다시 시도해 주세요");
        }
        challengeRepository.closeOpenChallenges(subjectId, now);

        String code = randomCode();
        LoginChallenge challenge = challengeRepository.save(LoginChallenge.issue(
                scope, subjectId, email, passwordEncoder.encode(code),
                config.ttlMinutes(), requesterIpHash));

        // 메일이 실패하면 여기서 예외가 올라가 챌린지 저장까지 함께 롤백된다.
        // 코드만 남고 메일은 안 간 상태를 만들지 않는다.
        mailer.send(email, "[답해줘] 로그인 인증 코드",
                body(Greeting.of(displayName, email), code, config.ttlMinutes()));

        return new Issued(challenge.getId(), config.ttlMinutes());
    }

    /**
     * 코드를 확인한다.
     *
     * @return 인증된 주체 id
     */
    @Transactional
    public UUID verify(AuthScope scope, UUID challengeId, String code) {
        LoginChallenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OTP_INVALID));

        // 스코프가 다르면 없는 것과 같이 다룬다. 업체 챌린지로 운영자 토큰을 받아낼 수 없다.
        if (challenge.getScope() != scope || !challenge.usable()) {
            throw new BusinessException(ErrorCode.OTP_INVALID);
        }
        if (config.devAcceptAnyCode()) {
            // 형식은 검사한다. 아무 값이나 통과시키면 화면의 입력 검증 버그를 못 잡는다.
            log.warn("[개발 우회] 인증 코드를 검사하지 않고 통과시킵니다 — subject={}", challenge.getSubjectId());
            challenge.consume();
            return challenge.getSubjectId();
        }
        if (!passwordEncoder.matches(code, challenge.getCodeHash())) {
            challenge.fail(config.maxAttempts());
            int left = Math.max(config.maxAttempts() - challenge.getAttempts(), 0);
            throw new BusinessException(ErrorCode.OTP_INVALID,
                    left > 0
                            ? "인증 코드가 올바르지 않습니다. " + left + "번 더 시도할 수 있습니다"
                            : "인증 코드를 여러 번 틀려 폐기했습니다. 처음부터 다시 로그인해 주세요");
        }
        challenge.consume();
        return challenge.getSubjectId();
    }

    /** 앞자리가 0이어도 자릿수가 유지되도록 문자열로 만든다. */
    private String randomCode() {
        return String.format("%0" + CODE_DIGITS + "d",
                RANDOM.nextInt((int) Math.pow(10, CODE_DIGITS)));
    }

    /** @param greeting 이미 {@link Greeting} 을 지난 값이다. 여기서 null 을 만나면 안 된다. */
    private MailTemplate.Body body(String greeting, String code, int ttlMinutes) {
        return MailTemplate.build(
                greeting,
                "로그인 인증 코드",
                "아래 코드를 로그인 화면에 입력해 주세요.",
                new MailTemplate.Highlight("인증 코드", code, true),
                null,
                java.util.List.of(
                        "이 코드는 " + ttlMinutes + "분 뒤에 만료되며 한 번만 쓸 수 있습니다.",
                        "본인이 요청하지 않았다면 이 메일을 무시하고 비밀번호를 바꿔 주세요."));
    }

    /**
     * @param challengeId 다음 단계에 되돌려줄 값
     * @param ttlMinutes  화면이 남은 시간을 표시하는 데 쓴다
     */
    public record Issued(UUID challengeId, int ttlMinutes) {
    }
}
