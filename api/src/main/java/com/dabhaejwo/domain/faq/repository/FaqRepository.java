package com.dabhaejwo.domain.faq.repository;

import com.dabhaejwo.domain.faq.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FaqRepository extends JpaRepository<Faq, UUID> {

    List<Faq> findAllByTenantIdOrderBySortOrderAsc(UUID tenantId);

    /** 위젯이 버튼으로 띄울 것만. */
    List<Faq> findAllByTenantIdAndShownTrueOrderBySortOrderAsc(UUID tenantId);

    /**
     * 후속 질문 조회.
     *
     * <p>{@code shownTrue} 를 거는 이유는 업체가 어떤 질문을 <b>숨긴 뒤에도</b> 다른 질문의
     * 후속 목록에는 그 id 가 남아 있기 때문이다. 걸러내지 않으면 숨긴 질문이 후속 칩으로
     * 되살아난다.
     */
    List<Faq> findAllByTenantIdAndShownTrueAndIdIn(UUID tenantId, Collection<UUID> ids);

    /** 테넌트 격리 — id 만으로 조회하지 않는다. */
    Optional<Faq> findByIdAndTenantId(UUID id, UUID tenantId);

    long countByTenantId(UUID tenantId);
}
