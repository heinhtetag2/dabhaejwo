package com.dabhaejwo.domain.member.entity;

import com.dabhaejwo.global.security.TenantMemberRole;
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
 * 업체 담당자. 업체 대시보드({@code /api/app})에 로그인하는 주체다.
 *
 * <p>운영자({@code operators})와 완전히 별개다 — 신뢰 수준이 다르고 토큰 체계도 나뉘어 있다.
 */
@Entity
@Table(name = "tenant_members")
public class TenantMember {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String email;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantMemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "invite_state", nullable = false)
    private InviteState inviteState;

    /**
     * BCrypt 해시. 초대만 보내고 수락 전이면 null 이다.
     * null 인 채로 로그인이 통과하지 않도록 {@link #matches} 에서 먼저 막는다.
     */
    @Column(name = "password_hash")
    private String passwordHash;

    /** 계정이 막혔을 때 연락할 수단. 메일 하나뿐이면 메일을 못 받는 순간 길이 없다. */
    private String phone;

    /**
     * 초대 링크 토큰의 해시.
     *
     * <p><b>원문을 저장하지 않는다</b> — 메일에만 실린다. 유출된 DB 로 남의 계정을
     * 만들 수 있으면 초대 링크는 그냥 백도어다.
     */
    @Column(name = "invite_token_hash")
    private String inviteTokenHash;

    @Column(name = "invite_expires_at")
    private OffsetDateTime inviteExpiresAt;

    /** 임시 비밀번호로 들어온 상태. 새 비밀번호를 정하기 전에는 로그인을 완료시키지 않는다. */
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "password_expires_at")
    private OffsetDateTime passwordExpiresAt;

    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected TenantMember() {
    }

    public static TenantMember invite(UUID tenantId, String email, String name, TenantMemberRole role) {
        return invite(tenantId, email, name, role, null);
    }

    public static TenantMember invite(UUID tenantId, String email, String name,
                                      TenantMemberRole role, String phone) {
        TenantMember member = new TenantMember();
        member.tenantId = tenantId;
        member.email = email;
        member.name = name;
        member.role = role;
        member.phone = phone;
        member.inviteState = InviteState.PENDING;
        member.createdAt = OffsetDateTime.now();
        return member;
    }

    /** 초대 링크를 건다. 다시 보내면 이전 토큰은 무효가 된다 — 해시를 덮어쓰기 때문이다. */
    public void attachInviteToken(String tokenHash, int ttlHours) {
        this.inviteTokenHash = tokenHash;
        this.inviteExpiresAt = OffsetDateTime.now().plusHours(ttlHours);
    }

    public boolean inviteUsable() {
        return inviteState == InviteState.PENDING
                && inviteTokenHash != null
                && inviteExpiresAt != null
                && inviteExpiresAt.isAfter(OffsetDateTime.now());
    }

    /**
     * 임시 비밀번호를 건다. <b>기존 비밀번호를 덮어쓴다</b> —
     * 남겨 두면 비밀번호 찾기를 남이 눌러도 원래 비밀번호로 계속 들어올 수 있어
     * "재설정했는데 옛 비밀번호가 살아 있는" 상태가 된다.
     */
    public void issueTemporaryPassword(String passwordHash, int ttlHours) {
        this.passwordHash = passwordHash;
        this.mustChangePassword = true;
        this.passwordExpiresAt = OffsetDateTime.now().plusHours(ttlHours);
        // 초대 중이었다면 임시 비밀번호로 들어오는 편이 빠르다. 초대 토큰은 버린다.
        this.inviteState = InviteState.ACCEPTED;
        this.inviteTokenHash = null;
        this.inviteExpiresAt = null;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.mustChangePassword = false;
        this.passwordExpiresAt = null;
    }

    /** 임시 비밀번호가 아직 살아 있는가. 만료된 임시 비밀번호로는 재설정도 못 한다. */
    public boolean temporaryPasswordUsable() {
        return mustChangePassword
                && passwordExpiresAt != null
                && passwordExpiresAt.isAfter(OffsetDateTime.now());
    }

    /** 데모·초기 계정용. 비밀번호가 이미 정해진 상태로 만든다. */
    public static TenantMember active(UUID tenantId,
                                      String email,
                                      String name,
                                      TenantMemberRole role,
                                      String passwordHash) {
        TenantMember member = invite(tenantId, email, name, role);
        member.inviteState = InviteState.ACCEPTED;
        member.passwordHash = passwordHash;
        return member;
    }

    public void acceptInvite(String passwordHash) {
        this.inviteState = InviteState.ACCEPTED;
        this.passwordHash = passwordHash;
        this.mustChangePassword = false;
        this.passwordExpiresAt = null;
        // 토큰은 한 번 쓰고 버린다. 남겨 두면 같은 링크로 비밀번호를 계속 바꿀 수 있다.
        this.inviteTokenHash = null;
        this.inviteExpiresAt = null;
    }

    public void changeRole(TenantMemberRole newRole) {
        this.role = newRole;
    }

    public void touchLastSeen() {
        this.lastSeenAt = OffsetDateTime.now();
    }

    /**
     * 로그인 가능한 상태인가. 초대 수락 전이면 비밀번호 해시가 없다.
     *
     * <p>호출부는 이 값이 false 여도 "초대 대기 중입니다" 같은 안내를 내지 않는다 —
     * 어떤 이메일이 등록돼 있는지 알려주는 셈이 되기 때문이다. 전부 {@code UNAUTHENTICATED} 다.
     */
    public boolean loginable() {
        return inviteState == InviteState.ACCEPTED && passwordHash != null;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public TenantMemberRole getRole() {
        return role;
    }

    public InviteState getInviteState() {
        return inviteState;
    }

    public OffsetDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
