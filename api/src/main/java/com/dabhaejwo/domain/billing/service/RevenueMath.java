package com.dabhaejwo.domain.billing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 정산 계산. <b>순수 함수만 둔다</b> — DB 도 시계도 모른다.
 *
 * <p>따로 뽑은 이유는 이 계산이 틀리면 조용히 틀리기 때문이다. 미수금이 음수가 되거나
 * 매출 0원인 달의 마진율이 0% 로 보이는 종류의 오류는 화면에서 그럴듯해 보이고,
 * 실 DB 없이 확인할 방법이 없으면 배포 뒤에야 드러난다.
 */
public final class RevenueMath {

    /** 무료 체험 기간. 코호트가 아직 판정 가능한지 보는 데 쓴다 (SignupService 와 같은 값). */
    public static final int TRIAL_DAYS = 14;

    private RevenueMath() {
    }

    /**
     * 미수금 = 청구 − 수납 − 환불.
     *
     * <p><b>음수로 내려보내지 않는다.</b> 환불이 그 달 청구보다 클 수 있는데(지난달 건을
     * 이번 달에 환불), 미수금이 음수로 뜨면 "받을 돈이 마이너스"라는 읽을 수 없는 말이 된다.
     * 환불 총액은 별도 값으로 함께 나가므로 정보가 사라지지는 않는다.
     */
    public static long outstanding(long billedKrw, long collectedKrw, long refundedKrw) {
        return Math.max(0, billedKrw - collectedKrw - refundedKrw);
    }

    /**
     * 마진 = 수납액 − 모델 원가.
     *
     * <p>인건비·서버비는 시스템이 모르므로 빼지 않는다. "영업이익"이라 부르지 않는 이유다.
     */
    public static BigDecimal margin(long collectedKrw, BigDecimal modelCostKrw) {
        return BigDecimal.valueOf(collectedKrw).subtract(modelCostKrw);
    }

    /**
     * 마진율.
     *
     * <p>수납액이 0이면 {@code null} 이다 — 0% 로 내리면 "남는 게 없다"로 읽히는데
     * 사실은 <b>받은 돈 자체가 없어</b> 비율이 정의되지 않는 것이다. 두 상황은
     * 운영자가 취할 조치가 전혀 다르다.
     */
    public static Integer marginPercent(long collectedKrw, BigDecimal modelCostKrw) {
        if (collectedKrw <= 0) {
            return null;
        }
        return margin(collectedKrw, modelCostKrw)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(collectedKrw), 0, RoundingMode.HALF_UP)
                .intValue();
    }

    /**
     * 코호트 전환율.
     *
     * <p>가입이 없던 달은 {@code null} 이다. 0% 로 두면 "아무도 전환하지 않았다"로 읽히는데
     * 전환할 대상 자체가 없었던 달이다.
     */
    public static Integer conversionPercent(long signupCount, long convertedCount) {
        if (signupCount <= 0) {
            return null;
        }
        return (int) Math.round(convertedCount * 100.0 / signupCount);
    }

    /**
     * 그 달의 코호트가 아직 판정 불가인가.
     *
     * <p>말일에 가입한 업체의 체험이 끝나지 않았으면 그 달 전환율은 <b>아직 오를 수 있다.</b>
     * 이걸 표시하지 않으면 이번 달과 지난 달이 같은 자격으로 나란히 놓여, 운영자는
     * "이번 달 전환율이 급락했다"고 잘못 읽는다.
     */
    public static boolean cohortOpen(YearMonth month, LocalDate today) {
        LocalDate lastJudgeableOn = month.atEndOfMonth().plusDays(TRIAL_DAYS);
        return !today.isAfter(lastJudgeableOn);
    }
}
