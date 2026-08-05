package com.dabhaejwo.domain.job.dto.response;

import com.dabhaejwo.domain.job.entity.Job;
import com.dabhaejwo.domain.job.entity.JobKind;
import com.dabhaejwo.domain.job.entity.JobStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 작업 한 건. api-contracts.md §8. */
public record JobResponse(
        Long id,
        JobKind kind,
        TenantRef tenant,
        String target,
        JobStatus status,
        String errorCode,
        int attempts,
        int maxAttempts,
        boolean retriable,
        OffsetDateTime updatedAt) {

    public record TenantRef(UUID id, String name) {
    }

    public static JobResponse of(Job job, TenantRef tenant) {
        return new JobResponse(
                job.getId(),
                job.getKind(),
                tenant,
                job.getTarget(),
                job.getStatus(),
                job.getErrorCode(),
                job.getAttempts(),
                job.getMaxAttempts(),
                job.retriable(),
                job.getUpdatedAt());
    }
}
