package com.dabhaejwo.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 비밀번호는 맞았지만 아직 토큰을 주지 않은 상태.
 *
 * <p>메일로 보낸 코드를 맞혀야 로그인이 끝난다. <b>코드는 해시로만 저장한다</b> —
 * DB 를 읽을 수 있는 사람이 남의 로그인을 완성할 수 있으면 2단계 인증이 있으나 마나다.
 *
 * <p>한 번 쓰면 {@code consumedAt} 이 찍혀 다시 쓸 수 없다. 시도 횟수를 넘겨도 같다 —
 * <b>폐기는 되돌리지 않는다.</b> 되돌릴 수 있으면 무한 대입이 가능해진다.
 */
@Entity
@Table(name = "login_challenges")
public class LoginChallenge {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthScope scope;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(nullable = false)
    private String email;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @Column(name = "requester_ip_hash")
    private String requesterIpHash;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected LoginChallenge() {
    }

    public static LoginChallenge issue(AuthScope scope, UUID subjectId, String email,
                                       String codeHash, int ttlMinutes, String requesterIpHash) {
        LoginChallenge challenge = new LoginChallenge();
        challenge.scope = scope;
        challenge.subjectId = subjectId;
        challenge.email = email;
        challenge.codeHash = codeHash;
        challenge.attempts = 0;
        challenge.createdAt = OffsetDateTime.now();
        challenge.expiresAt = challenge.createdAt.plusMinutes(ttlMinutes);
        challenge.requesterIpHash = requesterIpHash;
        return challenge;
    }

    public boolean usable() {
        return consumedAt == null && expiresAt.isAfter(OffsetDateTime.now());
    }

    /** 틀렸다. 시도 횟수를 올리고, 상한에 닿으면 폐기한다. */
    public void fail(int maxAttempts) {
        attempts++;
        if (attempts >= maxAttempts) {
            consumedAt = OffsetDateTime.now();
        }
    }

    /** 맞았다. 두 번 쓰지 못하게 즉시 닫는다. */
    public void consume() {
        consumedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public AuthScope getScope() {
        return scope;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public String getEmail() {
        return email;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public int getAttempts() {
        return attempts;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }
}
