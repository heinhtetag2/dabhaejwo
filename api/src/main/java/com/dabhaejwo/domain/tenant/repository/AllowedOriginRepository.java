package com.dabhaejwo.domain.tenant.repository;

import com.dabhaejwo.domain.tenant.entity.AllowedOrigin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AllowedOriginRepository extends JpaRepository<AllowedOrigin, UUID> {

    /**
     * 서비스 범위로만 조회한다.
     *
     * <p>{@code findAllByTenantId} 를 <b>일부러 두지 않았다.</b> 남겨두면 언젠가 쓰이고,
     * 그러면 A 서비스 키가 B 서비스 도메인에서 통과한다 — 컴파일러가 못 잡는 종류다.
     */
    List<AllowedOrigin> findAllByBotId(UUID botId);

    boolean existsByBotIdAndOrigin(UUID botId, String origin);

    /**
     * "설치됐다"는 신호. 위젯이 실제로 호출한 시각을 남긴다.
     *
     * <p><b>매 호출마다 쓰지 않는다.</b> 위젯이 붙은 사이트의 모든 페이지뷰마다 오는 요청이라
     * 그대로 쓰면 방문자 수만큼 UPDATE 가 돈다. 화면은 날짜만 보여주므로 그만한 정밀도가
     * 필요 없다 — 마지막 기록이 {@code threshold} 보다 오래됐을 때만 갱신한다.
     *
     * <p>읽고-판단하고-쓰는 대신 <b>한 문장의 조건부 UPDATE</b> 다. 동시에 들어온 요청들이
     * 서로를 덮어쓰지 않고, 대부분의 호출에서 0행을 갱신하고 끝난다.
     */
    @Modifying
    @Query("""
            UPDATE AllowedOrigin o SET o.lastCalledAt = :now
            WHERE o.botId = :botId AND LOWER(o.origin) = LOWER(:origin)
              AND (o.lastCalledAt IS NULL OR o.lastCalledAt < :threshold)
            """)
    int markCalled(@Param("botId") UUID botId,
                   @Param("origin") String origin,
                   @Param("now") OffsetDateTime now,
                   @Param("threshold") OffsetDateTime threshold);
}
