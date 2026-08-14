package com.dabhaejwo.domain.gap.repository;

import com.dabhaejwo.domain.gap.entity.AnswerGap;
import com.dabhaejwo.domain.gap.entity.GapStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnswerGapRepository extends JpaRepository<AnswerGap, Long> {

    Page<AnswerGap> findAllByBotIdAndStatusOrderByLastAskedAtDesc(
            UUID tenantId, GapStatus status, Pageable pageable);

    List<AnswerGap> findAllByBotIdAndStatusOrderByLastAskedAtDesc(UUID tenantId, GapStatus status);

    Optional<AnswerGap> findByIdAndBotId(Long id, UUID botId);

    Optional<AnswerGap> findByBotIdAndQuestionNorm(UUID botId, String questionNorm);

    long countByBotIdAndStatus(UUID botId, GapStatus status);

    /**
     * 업체 전체의 미해결 개선 건수. <b>알림 훑기 전용</b>이다 —
     * "답변 공백이 쌓였다"는 알림은 업체에게 가므로 서비스를 가리지 않는다.
     */
    @Query("SELECT COUNT(g) FROM AnswerGap g WHERE g.tenantId = :tenantId AND g.status = :status")
    long countAcrossBotsAndStatus(@Param("tenantId") UUID tenantId, @Param("status") GapStatus status);
}
