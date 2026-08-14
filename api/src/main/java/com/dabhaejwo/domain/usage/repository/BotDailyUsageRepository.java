package com.dabhaejwo.domain.usage.repository;

import com.dabhaejwo.domain.usage.entity.BotDailyUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BotDailyUsageRepository extends JpaRepository<BotDailyUsage, BotDailyUsage.Key> {

    Optional<BotDailyUsage> findByBotIdAndDay(UUID botId, LocalDate day);

    /**
     * 업체의 서비스별 기간 합계. "어느 서비스가 돈을 먹는가"를 답하는 값이다.
     *
     * <p>업체 조건이 <b>없다</b> — 호출부가 그 업체의 서비스 id 목록을 넘긴다.
     * 목록이 비면 호출하지 않는다(빈 {@code IN} 은 의미가 없다).
     */
    @Query("""
            SELECT u.botId AS botId,
                   SUM(u.convCount) AS convCount,
                   SUM(u.savedCount) AS savedCount,
                   SUM(u.tokensIn) AS tokensIn,
                   SUM(u.tokensOut) AS tokensOut,
                   SUM(u.costKrw) AS costKrw
            FROM BotDailyUsage u
            WHERE u.botId IN :botIds AND u.day >= :from AND u.day <= :to
            GROUP BY u.botId
            """)
    List<BotTotal> aggregateBetween(@Param("botIds") List<UUID> botIds,
                                    @Param("from") LocalDate from,
                                    @Param("to") LocalDate to);

    interface BotTotal {
        UUID getBotId();

        long getConvCount();

        long getSavedCount();

        long getTokensIn();

        long getTokensOut();

        BigDecimal getCostKrw();
    }
}
