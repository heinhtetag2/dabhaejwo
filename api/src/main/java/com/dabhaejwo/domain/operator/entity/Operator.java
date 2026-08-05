package com.dabhaejwo.domain.operator.entity;

import com.dabhaejwo.global.security.OperatorRole;
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
 * 운영자. 운영 콘솔({@code /api/ops})에 로그인하는 주체다.
 *
 * <p>업체 담당자({@code tenant_members})와 완전히 별개다 — 운영자는 남의 고객 데이터를
 * 볼 수 있고 담당자는 자기 테넌트만 볼 수 있다. 토큰 체계도 나뉘어 있다.
 *
 * <p>권한은 여기 저장하지 않고 {@link OperatorRole} 이 코드로 갖는다.
 * 역할→권한 매핑의 진실은 서버이며 기획서의 권한 매트릭스와 같은 커밋에서 움직인다.
 */
@Entity
@Table(name = "operators")
public class Operator {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperatorRole role;

    @Column(nullable = false)
    private boolean active;

    /** TODO(stub): 2FA 미구현. SSO 연동 시 이 자리가 대체된다. */
    @Column(name = "totp_secret")
    private String totpSecret;

    /**
     * BCrypt 해시. SSO 로 전환한 계정에는 비밀번호가 없으므로 nullable 이다.
     * null 인 채로 로그인이 통과하지 않도록 {@link #loginable()} 에서 먼저 막는다.
     */
    @Column(name = "password_hash")
    private String passwordHash;

    /** 임시 비밀번호로 들어온 상태. 새 비밀번호를 정하기 전에는 로그인을 완료시키지 않는다. */
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "password_expires_at")
    private OffsetDateTime passwordExpiresAt;

    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Operator() {
    }

    public static Operator create(String email, String name, OperatorRole role, String passwordHash) {
        Operator operator = new Operator();
        operator.email = email;
        operator.name = name;
        operator.role = role;
        operator.active = true;
        operator.passwordHash = passwordHash;
        operator.createdAt = OffsetDateTime.now();
        return operator;
    }

    /** 비활성 계정과 비밀번호 없는 계정은 로그인 대상이 아니다. */
    public boolean loginable() {
        return active && passwordHash != null && !passwordHash.isBlank();
    }

    /** 이름·역할 변경. 이메일은 바꾸지 않는다 — 로그인 식별자이고 감사 기록의 사람을 가리킨다. */
    public void edit(String newName, OperatorRole newRole) {
        this.name = newName;
        this.role = newRole;
    }

    /** {@code TenantMember} 와 같은 이유로 기존 비밀번호를 덮어쓴다. */
    public void issueTemporaryPassword(String newPasswordHash, int ttlHours) {
        this.passwordHash = newPasswordHash;
        this.mustChangePassword = true;
        this.passwordExpiresAt = OffsetDateTime.now().plusHours(ttlHours);
    }

    public boolean temporaryPasswordUsable() {
        return mustChangePassword
                && passwordExpiresAt != null
                && passwordExpiresAt.isAfter(OffsetDateTime.now());
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void changePassword(String newPasswordHash) {
        this.mustChangePassword = false;
        this.passwordExpiresAt = null;
        this.passwordHash = newPasswordHash;
    }

    /**
     * 비활성화·복구.
     *
     * <p><b>삭제가 아니다.</b> {@code operators} 는 audit_logs·quota_overrides·tenant_notes·
     * impersonation_sessions·tickets·provider_credentials 가 FK 로 참조한다. 지우면 감사 기록의
     * 행위자가 사라지는데 그 기록은 수정·삭제 불가에 3년 보존이다 — 퇴사한 사람의 행적도
     * 남아 있어야 한다.
     *
     * <p>비활성 계정은 로그인만 막히고 과거 기록에는 이름이 그대로 남는다.
     */
    public void changeActive(boolean next) {
        this.active = next;
    }

    public void touchLastSeen() {
        this.lastSeenAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public OperatorRole getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public OffsetDateTime getLastSeenAt() {
        return lastSeenAt;
    }
}
