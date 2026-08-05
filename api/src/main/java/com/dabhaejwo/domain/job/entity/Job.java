package com.dabhaejwo.domain.job.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 크롤링·임베딩 작업 한 건.
 *
 * <p><b>지금 이 테이블은 비어 있다.</b> 작업을 만드는 쪽(크롤러·임베딩 워커)이 아직 없다.
 * 그래도 조회 경로를 먼저 만들어 두는 이유는, 워커가 붙는 순간 운영자가 실패를 볼 수
 * 있어야 하기 때문이다 — 실패를 못 보는 큐는 없는 것과 같다.
 */
@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobKind kind;

    /** 대상. 크롤링이면 경로, 임베딩이면 파일명이다. */
    @Column(nullable = false)
    private String target;

    @Column(name = "document_id")
    private UUID documentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    /** 운영자용이므로 원문 그대로 둔다({@code pdf_parse_timeout}). 한글 설명은 화면이 매핑한다. */
    @Column(name = "error_code")
    private String errorCode;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Job() {
    }

    public Long getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public JobKind getKind() {
        return kind;
    }

    public String getTarget() {
        return target;
    }

    public JobStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 재시도 여력이 남았는가. 화면이 "재시도 3/3"을 회색으로 보여줄지 판단한다.
     *
     * <p>실제 재시도는 아직 못 한다 — 큐에 다시 넣어도 집어갈 워커가 없다.
     */
    public boolean retriable() {
        return status == JobStatus.FAILED && attempts < maxAttempts;
    }
}
