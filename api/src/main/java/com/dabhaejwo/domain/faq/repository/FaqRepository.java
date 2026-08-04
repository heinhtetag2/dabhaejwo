package com.dabhaejwo.domain.faq.repository;

import com.dabhaejwo.domain.faq.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FaqRepository extends JpaRepository<Faq, UUID> {

    List<Faq> findAllByTenantIdOrderBySortOrderAsc(UUID tenantId);

    /** 위젯이 버튼으로 띄울 것만. */
    List<Faq> findAllByTenantIdAndShownTrueOrderBySortOrderAsc(UUID tenantId);

    /** 테넌트 격리 — id 만으로 조회하지 않는다. */
    Optional<Faq> findByIdAndTenantId(UUID id, UUID tenantId);

    long countByTenantId(UUID tenantId);
}
