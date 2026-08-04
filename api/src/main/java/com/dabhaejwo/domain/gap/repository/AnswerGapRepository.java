package com.dabhaejwo.domain.gap.repository;

import com.dabhaejwo.domain.gap.entity.AnswerGap;
import com.dabhaejwo.domain.gap.entity.GapStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnswerGapRepository extends JpaRepository<AnswerGap, Long> {

    Page<AnswerGap> findAllByTenantIdAndStatusOrderByLastAskedAtDesc(
            UUID tenantId, GapStatus status, Pageable pageable);

    List<AnswerGap> findAllByTenantIdAndStatusOrderByLastAskedAtDesc(UUID tenantId, GapStatus status);

    Optional<AnswerGap> findByIdAndTenantId(Long id, UUID tenantId);

    Optional<AnswerGap> findByTenantIdAndQuestionNorm(UUID tenantId, String questionNorm);

    long countByTenantIdAndStatus(UUID tenantId, GapStatus status);
}
