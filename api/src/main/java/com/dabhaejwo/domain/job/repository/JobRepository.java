package com.dabhaejwo.domain.job.repository;

import com.dabhaejwo.domain.job.entity.Job;
import com.dabhaejwo.domain.job.entity.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

public interface JobRepository extends JpaRepository<Job, Long> {

    @Query("""
            SELECT j FROM Job j
            WHERE (:status IS NULL OR j.status = :status)
            ORDER BY j.updatedAt DESC
            """)
    Page<Job> search(@Param("status") JobStatus status, Pageable pageable);

    long countByStatus(JobStatus status);

    long countByStatusAndUpdatedAtGreaterThanEqual(JobStatus status, OffsetDateTime from);
}
