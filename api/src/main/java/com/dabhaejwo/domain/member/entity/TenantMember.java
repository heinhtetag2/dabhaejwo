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

    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected TenantMember() {
    }

    public static TenantMember invite(UUID tenantId, String email, String name, TenantMemberRole role) {
        TenantMember member = new TenantMember();
        member.tenantId = tenantId;
        member.email = email;
        member.name = name;
        member.role = role;
        member.inviteState = InviteState.PENDING;
        member.createdAt = OffsetDateTime.now();
        return member;
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
