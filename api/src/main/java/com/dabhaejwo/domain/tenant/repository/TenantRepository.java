package com.dabhaejwo.domain.tenant.repository;

import com.dabhaejwo.domain.tenant.entity.Tenant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
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

    /**
     * 서비스 수 상한처럼 <b>세고 나서 만드는</b> 판정에 쓴다.
     *
     * <p>잠그지 않으면 동시 요청이 각자 "아직 여유가 있다"고 판단해 상한을 넘겨 만든다.
     * 사람이 대시보드에서 누르는 드문 행위라 잠금 비용이 사실상 0이다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Tenant t WHERE t.id = :id")
    java.util.Optional<Tenant> findByIdForUpdate(@Param("id") java.util.UUID id);


    /**
     * 오늘까지 청구할 업체.
     *
     * <p>{@code TRIAL} 은 제외한다 — 체험 중에는 받을 것이 없다. 정지·해지도 제외한다.
     *
     * <p>날짜가 <b>지난</b> 것도 집는다({@code <=}). 배치가 하루 멈췄다고 그 날 청구가
     * 영영 사라지면 안 된다.
     */
    @org.springframework.data.jpa.repository.Query("""
            SELECT t FROM Tenant t
            WHERE t.nextBillingDate IS NOT NULL
              AND t.nextBillingDate <= :today
              AND t.status = com.dabhaejwo.domain.tenant.entity.TenantStatus.ACTIVE
            ORDER BY t.nextBillingDate ASC
            """)
    java.util.List<Tenant> findDueForBilling(
            @org.springframework.data.repository.query.Param("today") java.time.LocalDate today,
            org.springframework.data.domain.Limit limit);

    Optional<Tenant> findByPublishableKey(String publishableKey);

    /**
     * 업체명·도메인·<b>서비스</b>(이름·도메인·위젯 키) 부분 일치 검색.
     *
     * <p>문의 메일에 적힌 도메인만으로 찾을 수 있어야 하므로 도메인은 서브도메인도 매칭된다.
     *
     * <p><b>서비스까지 보는 것이 요점이다.</b> 위젯 키가 `tenants` 에서 `bots` 로 옮겨간 뒤로
     * 두 번째 서비스의 키로는 업체를 찾을 수 없었다 — CS 문의 1순위가 "이 키 쓰는 업체가
     * 누구냐"인데 그 경로가 끊겨 있었다. {@code tenants.publishable_key} 는 첫 서비스의
     * 키를 복제한 유물이라 더는 보지 않는다.
     */
    @Query("""
            SELECT t FROM Tenant t
            WHERE t.status <> :excluded
              AND (:q IS NULL
                   OR LOWER(t.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(t.primaryDomain) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR EXISTS (SELECT 1 FROM Bot b WHERE b.tenantId = t.id
                              AND (LOWER(b.name) LIKE LOWER(CONCAT('%', :q, '%'))
                                OR LOWER(b.primaryDomain) LIKE LOWER(CONCAT('%', :q, '%'))
                                OR LOWER(b.publishableKey) LIKE LOWER(CONCAT('%', :q, '%')))))
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
               OR EXISTS (SELECT 1 FROM Bot b WHERE b.tenantId = t.id
                          AND (LOWER(b.name) LIKE LOWER(CONCAT('%', :q, '%'))
                            OR LOWER(b.primaryDomain) LIKE LOWER(CONCAT('%', :q, '%'))
                            OR LOWER(b.publishableKey) LIKE LOWER(CONCAT('%', :q, '%'))))
            """)
    List<Tenant> searchAll(@Param("q") String q);

    List<Tenant> findAllByStatus(TenantStatus status);

    long countByStatus(TenantStatus status);

    /**
     * 월별 가입 수. 정산 화면의 전환율 분모다.
     *
     * <p>월 경계는 <b>KST</b> 다 — 가입은 사람이 한 행위라 업체의 하루를 따른다
     * ({@code BusinessDay}). 모델 원가만 UTC 로 끊는 이유는 AI 사용량 화면과
     * 같은 숫자여야 하기 때문이다 ({@code AiUsageRepository#aggregateMonthlyCost}).
     */
    @Query(value = """
            SELECT date_trunc('month', created_at AT TIME ZONE 'Asia/Seoul')::date AS month,
                   COUNT(*) AS tenantCount
            FROM tenants
            WHERE created_at >= :from
            GROUP BY 1
            """, nativeQuery = true)
    List<MonthlyCount> countSignupsByMonth(@Param("from") java.time.OffsetDateTime from);

    /** 월별 해지 수. 가입과 나란히 놓아야 "늘고 있나 줄고 있나"에 답할 수 있다. */
    @Query(value = """
            SELECT date_trunc('month', churned_at AT TIME ZONE 'Asia/Seoul')::date AS month,
                   COUNT(*) AS tenantCount
            FROM tenants
            WHERE churned_at IS NOT NULL AND churned_at >= :from
            GROUP BY 1
            """, nativeQuery = true)
    List<MonthlyCount> countChurnsByMonth(@Param("from") java.time.OffsetDateTime from);

    /**
     * 가입월별 업체 id. 코호트 전환율의 교집합을 메모리에서 낸다.
     *
     * <p>SQL 로 한 번에 조인하지 않는 이유는 "한 번이라도 결제했나"가 청구 기간과
     * 무관한 판정이라 조인 조건이 없기 때문이다. 기준은 업체 500곳이다.
     */
    @Query(value = """
            SELECT id AS tenantId,
                   date_trunc('month', created_at AT TIME ZONE 'Asia/Seoul')::date AS month
            FROM tenants
            WHERE created_at >= :from
            """, nativeQuery = true)
    List<SignupCohort> findSignupCohorts(@Param("from") java.time.OffsetDateTime from);

    interface MonthlyCount {
        java.time.LocalDate getMonth();

        long getTenantCount();
    }

    interface SignupCohort {
        UUID getTenantId();

        java.time.LocalDate getMonth();
    }

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
