-- 토스페이먼츠 자동결제(빌링) 연동.
--
-- 카드를 한 번 등록해 **빌링키**를 받아두고, 매달 그 키로 청구한다.
-- 카드번호는 우리가 갖지 않는다 — 토스가 보관하고 우리는 키만 갖는다.

CREATE TABLE tenant_billing (
  -- 업체당 결제수단 하나. 여러 장을 허용하면 "어느 카드로 청구했나"가 매달 달라진다.
  tenant_id         uuid PRIMARY KEY REFERENCES tenants(id) ON DELETE CASCADE,

  -- **빌링키는 그 자체로 돈을 뺄 수 있는 값이다.** 공급사 API 키와 같은 등급으로 다룬다 —
  -- 평문으로 두면 DB 를 읽는 것만으로 남의 카드에 청구할 수 있다. AES-256-GCM(SecretCipher).
  billing_key_cipher text NOT NULL,

  -- 토스에 넘긴 구매자 식별자. 빌링키로 청구할 때 함께 보내야 한다.
  -- tenant_id 를 그대로 쓰지만, 토스 쪽 규약이 바뀔 수 있어 별도 컬럼으로 둔다.
  customer_key      text NOT NULL,

  -- 화면에 보여줄 카드 정보. 마스킹된 값만 저장한다(토스가 마스킹해서 준다).
  card_company      text,
  card_number_masked text,
  card_type         text,

  registered_by     uuid REFERENCES tenant_members(id) ON DELETE SET NULL,
  registered_at     timestamptz NOT NULL DEFAULT now(),
  updated_at        timestamptz NOT NULL DEFAULT now()
);

-- 청구 기록에 토스 쪽 식별자를 남긴다. 없으면 분쟁이 생겼을 때 대조할 방법이 없다.
ALTER TABLE billing_records ADD COLUMN order_id text;
ALTER TABLE billing_records ADD COLUMN payment_key text;
ALTER TABLE billing_records ADD COLUMN paid_at timestamptz;
ALTER TABLE billing_records ADD COLUMN method text;

-- **같은 주문번호로 두 번 청구되지 않게 못 박는다.** 멱등키를 헤더로도 보내지만,
-- 그건 토스 쪽 보장이고 이건 우리 쪽 보장이다. 이중결제는 되돌려도 신뢰가 안 돌아온다.
CREATE UNIQUE INDEX ON billing_records (order_id) WHERE order_id IS NOT NULL;

COMMENT ON COLUMN billing_records.order_id IS
  '주문번호. tenant + 청구월로 결정되므로 재시도해도 같은 값이다 — 그래서 중복 청구가 막힌다.';
