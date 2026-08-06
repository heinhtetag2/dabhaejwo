-- 자동 청구 일정.
--
-- 결제한 날이 기준이 된다 — 8월 3일에 결제하면 9월 3일에 다시 청구한다.

-- 기준일(1~31). **next_billing_date 에서 파생하지 않는다** —
-- 31일 기준인 업체가 2월에 28일로 청구되면, 다음 달부터 28일에 눌러앉아 매년 앞당겨진다.
-- 원래 기준일을 따로 기억해야 3월에 다시 31일로 돌아온다.
ALTER TABLE tenants ADD COLUMN billing_day int
  CHECK (billing_day IS NULL OR (billing_day BETWEEN 1 AND 31));

COMMENT ON COLUMN tenants.billing_day IS
  '청구 기준일. 그 달에 없는 날짜면 말일로 당긴다(1/31 → 2/28 → 3/31).';

COMMENT ON COLUMN tenants.next_billing_date IS
  '다음 청구 예정일. 실패 재시도도 이 값을 앞당겨 쓴다. NULL 이면 청구하지 않는다.';

-- 배치가 매일 "오늘까지 청구할 곳"을 찾는다. 부분 인덱스라 청구 대상만 담긴다.
CREATE INDEX ON tenants (next_billing_date) WHERE next_billing_date IS NOT NULL;
