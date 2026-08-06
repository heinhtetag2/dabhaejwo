package com.dabhaejwo.domain.billing.repository;

import com.dabhaejwo.domain.billing.entity.BillingRecord;
import com.dabhaejwo.domain.billing.entity.BillingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BillingRecordRepository extends JpaRepository<BillingRecord, Long> {

    List<BillingRecord> findAllByTenantIdOrderByPeriodDesc(UUID tenantId);

    /** 같은 달을 두 번 청구하지 않기 위한 조회. (tenant_id, period) 는 UNIQUE 다. */
    java.util.Optional<BillingRecord> findByTenantIdAndPeriod(UUID tenantId, java.time.LocalDate period);

    /**
     * <b>가장 최근</b> 결제 시도가 실패한 업체들.
     *
     * <p>"실패 기록이 하나라도 있는 업체"가 아니다 — 지난달 실패하고 이번 달 정상 결제된
     * 업체를 계속 결제 실패로 세면 필터가 쓸모없어진다.
     */
    @Query("""
            SELECT DISTINCT b.tenantId FROM BillingRecord b
            WHERE b.status = com.dabhaejwo.domain.billing.entity.BillingStatus.FAILED
              AND b.period = (SELECT MAX(latest.period) FROM BillingRecord latest
                              WHERE latest.tenantId = b.tenantId)
            """)
    List<UUID> findTenantIdsWithLatestPaymentFailed();

    /**
     * 청구월별 집계. 정산 화면의 지표와 월별 추이가 함께 쓴다.
     *
     * <p>청구액과 수납액을 <b>한 번의 질의에서</b> 뽑는다. 두 번 나눠 부르면 그 사이에
     * 배치가 결제를 성공시켜 "청구보다 수납이 많은" 숫자가 나올 수 있다.
     *
     * <p>{@code REFUNDED} 는 수납에서 빠지고 환불 합계로 잡힌다 — 받았다가 돌려준 돈이라
     * 매출로 세면 안 된다.
     */
    @Query("""
            SELECT b.period AS period,
                   COALESCE(SUM(b.amount), 0) AS billedKrw,
                   COALESCE(SUM(CASE WHEN b.status = com.dabhaejwo.domain.billing.entity.BillingStatus.PAID
                                     THEN b.amount ELSE 0 END), 0) AS collectedKrw,
                   COALESCE(SUM(CASE WHEN b.status = com.dabhaejwo.domain.billing.entity.BillingStatus.REFUNDED
                                     THEN b.amount ELSE 0 END), 0) AS refundedKrw,
                   COALESCE(SUM(CASE WHEN b.status = com.dabhaejwo.domain.billing.entity.BillingStatus.PAID
                                     THEN 1 ELSE 0 END), 0) AS paidCount,
                   COALESCE(SUM(CASE WHEN b.status = com.dabhaejwo.domain.billing.entity.BillingStatus.FAILED
                                     THEN 1 ELSE 0 END), 0) AS failedCount,
                   COALESCE(SUM(CASE WHEN b.status = com.dabhaejwo.domain.billing.entity.BillingStatus.PENDING
                                     THEN 1 ELSE 0 END), 0) AS pendingCount
            FROM BillingRecord b
            WHERE b.period >= :from AND b.period < :to
            GROUP BY b.period
            """)
    List<PeriodTotal> aggregateByPeriod(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * 한 번이라도 결제에 성공한 업체.
     *
     * <p>가입 코호트 전환율의 분자다 — "그 달에 가입한 업체 중 <b>지금까지</b> 결제한 곳".
     * 첫 결제가 언제였는지는 묻지 않는다. 체험 14일이 걸쳐 있어 가입월과 첫 결제월이
     * 대부분 다르기 때문이다.
     */
    @Query("""
            SELECT DISTINCT b.tenantId FROM BillingRecord b
            WHERE b.status = com.dabhaejwo.domain.billing.entity.BillingStatus.PAID
            """)
    List<UUID> findTenantIdsEverPaid();

    Page<BillingRecord> findAllByPeriod(LocalDate period, Pageable pageable);

    /**
     * 상태 필터가 붙은 목록.
     *
     * <p>{@code :status IS NULL OR ...} 한 벌로 합치지 않고 메서드를 나눈 이유는
     * PostgreSQL 파라미터 타입 추론 함정을 아예 피하기 위해서다
     * ({@code .claude/rules/backend-spring-boot.md} PG 고유 규칙).
     */
    Page<BillingRecord> findAllByPeriodAndStatus(LocalDate period, BillingStatus status,
                                                 Pageable pageable);

    interface PeriodTotal {
        LocalDate getPeriod();

        long getBilledKrw();

        long getCollectedKrw();

        long getRefundedKrw();

        long getPaidCount();

        long getFailedCount();

        long getPendingCount();
    }
}
