package com.dabhaejwo.domain.faq.repository;

import com.dabhaejwo.domain.faq.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FaqRepository extends JpaRepository<Faq, UUID> {

    List<Faq> findAllByBotIdOrderBySortOrderAsc(UUID botId);

    /** 위젯이 버튼으로 띄울 것만. */
    List<Faq> findAllByBotIdAndShownTrueOrderBySortOrderAsc(UUID botId);

    /**
     * 후속 질문 조회.
     *
     * <p>{@code shownTrue} 를 거는 이유는 업체가 어떤 질문을 <b>숨긴 뒤에도</b> 다른 질문의
     * 후속 목록에는 그 id 가 남아 있기 때문이다. 걸러내지 않으면 숨긴 질문이 후속 칩으로
     * 되살아난다.
     */
    List<Faq> findAllByBotIdAndShownTrueAndIdIn(UUID botId, Collection<UUID> ids);

    /** 테넌트 격리 — id 만으로 조회하지 않는다. */
    Optional<Faq> findByIdAndBotId(UUID id, UUID botId);

    long countByBotId(UUID botId);

    /**
     * 업체 전체의 공통 질문 수. <b>운영 콘솔 전용</b>이다.
     *
     * <p>이름이 {@code AcrossBots} 인 것이 요점이다 — 업체 상세 화면은 계약 단위를 보므로
     * 서비스가 여럿이면 전부 더한다. 대시보드에서는 절대 쓰지 않는다.
     */
    @Query("SELECT COUNT(f) FROM Faq f WHERE f.tenantId = :tenantId")
    long countAcrossBots(@Param("tenantId") UUID tenantId);
}
