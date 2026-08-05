package com.dabhaejwo.domain.plan.entity;

import com.dabhaejwo.global.llm.LlmProviderName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 요금제별 답변 모델과 조각 수. 원가 추정의 근거다.
 *
 * <p>{@code plan_id} 가 곧 PK 다 — 요금제 하나에 배정은 하나다. 별도 id 를 두면
 * "배정이 두 벌 생긴" 상태가 표현 가능해지고, 그러면 언젠가 실제로 생긴다.
 */
@Entity
@Table(name = "plan_model_assignments")
public class PlanModelAssignment {

    @Id
    @Column(name = "plan_id")
    private UUID planId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LlmProviderName provider;

    @Column(nullable = false)
    private String model;

    /**
     * 프롬프트에 실을 문서 조각 수. 많을수록 정확하지만 <b>입력 토큰이 비례해 늘어난다</b> —
     * 답변 원가의 대부분은 출력이 아니라 입력이다 (admin-console-plan.md §4.4).
     */
    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected PlanModelAssignment() {
    }

    public static PlanModelAssignment of(UUID planId, LlmProviderName provider, String model, int chunkCount) {
        PlanModelAssignment assignment = new PlanModelAssignment();
        assignment.planId = planId;
        assignment.provider = provider;
        assignment.model = model;
        assignment.chunkCount = chunkCount;
        assignment.updatedAt = OffsetDateTime.now();
        return assignment;
    }

    public void update(LlmProviderName newProvider, String newModel, int newChunkCount) {
        this.provider = newProvider;
        this.model = newModel;
        this.chunkCount = newChunkCount;
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getPlanId() {
        return planId;
    }

    public LlmProviderName getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public int getChunkCount() {
        return chunkCount;
    }
}
