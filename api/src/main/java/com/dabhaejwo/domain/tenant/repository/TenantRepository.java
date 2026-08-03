package com.dabhaejwo.domain.tenant.repository;

import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.entity.TenantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByPublishableKey(String publishableKey);

    /**
     * 업체명·도메인·공개 키 부분 일치 검색.
     * 문의 메일에 적힌 도메인만으로 찾을 수 있어야 하므로 도메인은 서브도메인도 매칭된다.
     */
    @Query("""
            SELECT t FROM Tenant t
            WHERE t.status <> :excluded
              AND (:q IS NULL
                   OR LOWER(t.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(t.primaryDomain) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(t.publishableKey) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Tenant> search(@Param("q") String q,
                        @Param("excluded") TenantStatus excluded,
                        Pageable pageable);

    List<Tenant> findAllByStatus(TenantStatus status);

    long countByStatus(TenantStatus status);
}
