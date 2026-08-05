package com.dabhaejwo.domain.flag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 기능 플래그. {@code key} 가 곧 PK 다 — 코드가 이 문자열로 기능을 찾으므로 바뀌면 안 된다.
 */
@Entity
@Table(name = "feature_flags")
public class FeatureFlag {

    @Id
    @Column(name = "key", nullable = false)
    private String key;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlagScope scope;

    /**
     * {@code uuid[]}. 조회 조건으로 쓰지 않고 통째로 읽고 쓴다 —
     * 배열을 조회 조건으로 쓰게 되면 그건 조인 테이블이 필요하다는 신호다
     * (.claude/rules/backend-spring-boot.md).
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "target_tenant_ids", nullable = false, columnDefinition = "uuid[]")
    private UUID[] targetTenantIds;

    @Column(name = "target_plan_id")
    private UUID targetPlanId;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected FeatureFlag() {
    }

    public void update(FlagScope newScope, List<UUID> tenantIds, UUID planId, boolean newEnabled) {
        this.scope = newScope;
        this.targetTenantIds = tenantIds == null ? new UUID[0] : tenantIds.toArray(new UUID[0]);
        this.targetPlanId = planId;
        this.enabled = newEnabled;
        this.updatedAt = OffsetDateTime.now();
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public FlagScope getScope() {
        return scope;
    }

    public List<UUID> getTargetTenantIds() {
        return targetTenantIds == null ? List.of() : List.of(targetTenantIds);
    }

    public UUID getTargetPlanId() {
        return targetPlanId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
