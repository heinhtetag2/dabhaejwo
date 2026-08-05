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

    /**
     * 검색어에 맞는 업체 전부. 페이지를 나누지 않는다.
     *
     * <p>원가율은 DB 컬럼이 아니라 집계에서 계산되는 값이라 <b>SQL 로 정렬할 수 없다.</b>
     * 페이지 단위로 잘라온 뒤 메모리에서 정렬하면 페이지마다 따로 정렬되어 전체 순서가
     * 어긋난다 — 2페이지 첫 줄이 1페이지 끝줄보다 원가율이 높은 상태가 된다.
     * 그래서 전부 읽어 계산·정렬한 뒤 잘라낸다. 기준은 업체 500곳이다
     * (admin-console-plan.md §10 비기능 요구사항).
     *
     * <p>{@code q} 는 <b>null 을 받지 않는다.</b> 검색어가 없으면 빈 문자열을 넘긴다.
     * 이 파라미터는 함수 안에만 나와서 Hibernate 가 타입을 추론할 근거가 없고, null 을 주면
     * PostgreSQL 이 {@code bytea} 로 바인딩해 {@code lower(bytea) does not exist} 로 터진다.
     * {@code LIKE '%%'} 는 전부 매칭이라 필터가 없는 것과 같다 —
     * {@code KnowledgeDocumentRepository#search} 와 같은 함정이다.
     */
    @Query("""
            SELECT t FROM Tenant t
            WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(t.primaryDomain) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(t.publishableKey) LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    List<Tenant> searchAll(@Param("q") String q);

    List<Tenant> findAllByStatus(TenantStatus status);

    long countByStatus(TenantStatus status);

    /** 요금제 화면의 "사용 업체 수". 판매 중단 판단의 근거다. */
    @Query("SELECT t.planId AS planId, COUNT(t) AS count FROM Tenant t "
            + "WHERE t.status <> com.dabhaejwo.domain.tenant.entity.TenantStatus.CHURNED "
            + "GROUP BY t.planId")
    List<PlanUsage> countByPlan();

    interface PlanUsage {
        UUID getPlanId();

        long getCount();
    }
}
