-- 서비스(코드에서는 bot) — 업체 하나가 챗봇을 여럿 운영한다.
--
-- 기획: docs/plan/service-plan.md
--
-- V2 는 `bot_settings.tenant_id` 를 PK 로 두면서 이렇게 적었다:
--   "업체당 하나. 1:N 이 될 일이 없는데 별도 id 를 두면 '설정이 두 벌 생긴' 상태가
--    표현 가능해지고, 그러면 언젠가 실제로 생긴다."
-- 그 전제가 틀렸다. 한 업체가 사이트를 여럿 운영하는 경우가 실제로 있고, 지금 구조에서는
-- 같은 봇·같은 지식·같은 통계를 공유할 수밖에 없어 **쇼핑몰 질문에 회사소개 문서로 답한다.**
--
-- ============================================================
-- 이 마이그레이션이 지키는 두 가지
-- ============================================================
--
-- 1. **이미 남의 사이트에 박힌 `pk_live_*` 가 계속 동작한다.**
--    키를 새로 뽑지 않는다 — 우리는 그 <script> 태그를 고칠 수 없다.
--    기존 키를 그대로 기본 서비스로 옮긴다.
--
-- 2. **아무것도 DROP 하지 않고 아무것도 NOT NULL 로 만들지 않는다.**
--    `ddl-auto: validate` 라 마이그레이션과 앱 배포가 사실상 동시다. 컬럼을 먼저 지우면
--    구버전으로 롤백하는 순간 API 가 기동조차 못 한다.
--    Hibernate 의 validate 는 "DB 에 있는데 매핑에 없는 컬럼"을 문제 삼지 않고 PK·nullability
--    도 검사하지 않는다 — 그래서 "nullable 백필 먼저 → 나중에 조이기"가 가능하다.
--    조이는 것(NOT NULL · PK 교체 · 구 UNIQUE 제거)은 V17 이 한다.
--
-- 그래서 이 파일을 적용한 뒤에도 **기존 앱이 그대로 뜨고 위젯이 그대로 답한다.**

-- 락을 무한정 기다리다 API 를 통째로 멈추는 것보다 빨리 실패하고 롤백하는 편이 낫다.
-- PostgreSQL 은 DDL 이 트랜잭션이라 중간에 실패하면 통째로 되감긴다 — 이것이 안전망이다.
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '10min';

-- ============================================================
-- 1. bots
-- ============================================================

CREATE TABLE bots (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id       uuid NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,

  -- 업체가 붙이는 이름. 화면에서는 "서비스 이름"이다.
  name            text NOT NULL,
  primary_domain  text NOT NULL,

  -- 위젯 키. 남의 사이트 소스에 박히고 조회가 이 값 하나로만 이뤄지므로 전역 UNIQUE 다.
  publishable_key text NOT NULL UNIQUE,

  -- `tenants.status`(계약: 돈을 내는가)와 다른 축이다. 이건 "업체가 이 서비스를 켜뒀나".
  -- 위젯 인증은 앞으로 둘 다 본다.
  status          text NOT NULL DEFAULT 'ACTIVE'
                  CHECK (status IN ('ACTIVE', 'PAUSED', 'DELETING')),

  -- 기본 서비스. 백필의 착지점이고, 서비스를 지목하지 않는 옛 경로(/app/leads)의 착지점이다.
  is_default      boolean NOT NULL DEFAULT false,

  deleted_at      timestamptz,
  -- 위젯은 즉시 멈추고 데이터 파기는 유예한다 — 실수로 지운 업체를 되살릴 수 있어야 한다.
  -- tenants.purge_after 와 같은 개념이므로 cost_guards.churn_purge_grace_days 를 함께 쓴다.
  purge_after     timestamptz,

  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now(),

  -- 같은 이름이 둘이면 화면의 서비스 선택기에서 구분할 수 없다.
  UNIQUE (tenant_id, name),

  -- **자식 테이블의 복합 FK 를 받기 위한 것이다.** 아래 §3 참조.
  UNIQUE (id, tenant_id)
);

CREATE INDEX ON bots (tenant_id);

-- 기본 서비스는 업체당 정확히 하나. 부분 유니크라 "기본이 둘"인 상태가 표현 불가능해진다.
CREATE UNIQUE INDEX bots_one_default_per_tenant ON bots (tenant_id) WHERE is_default;

COMMENT ON TABLE bots IS
  '서비스(화면 용어) = 챗봇 한 벌. 위젯 키·설정·지식·대화가 이 단위로 갈린다.';
COMMENT ON COLUMN bots.publishable_key IS
  'pk_live_*. tenants.publishable_key 에서 옮겨온 값이다 — 재발급하지 않는다.';
COMMENT ON COLUMN bots.status IS
  'ACTIVE 켜짐 · PAUSED 꺼둠 · DELETING 삭제 유예 중. tenants.status 와는 다른 축이다.';

-- ============================================================
-- 2. 백필 — 업체마다 기본 서비스 하나
-- ============================================================
--
-- 이름은 **업체명을 그대로 쓴다.** "서비스 1" 로 두면 두 번째를 만들 때 선택기에
-- `서비스 1 / 아울렛` 이 나란히 떠서, 첫 번째의 이름을 고쳐야 한다는 걸 알기 어렵다.
--
-- status 를 tenants.status 에서 파생시키지 않는다 — 해지된 업체의 서비스도 ACTIVE 로 넣는다.
-- 기존 동작 보존이 우선이고, 노출 판정은 위젯 인증이 두 값을 함께 보는 것으로 해결한다.
-- (V11 의 widget_enabled DEFAULT true, V15 의 launcher_background DEFAULT 'BRAND' 와 같은 논리)

INSERT INTO bots (tenant_id, name, primary_domain, publishable_key,
                  status, is_default, created_at, updated_at)
SELECT t.id, t.name, t.primary_domain, t.publishable_key,
       'ACTIVE', true, t.created_at, now()
FROM tenants t;

-- ============================================================
-- 3. 자식 테이블 — nullable bot_id + 복합 FK
-- ============================================================
--
-- **`FOREIGN KEY (bot_id, tenant_id) REFERENCES bots (id, tenant_id)` 가 핵심 장치다.**
--
-- 자식에 `tenant_id` 를 남기는 이유는 테넌트 격리 규칙(모든 테넌트 소유 엔티티는 tenant_id)을
-- 지키고 조인 없이 격리를 걸기 위해서인데, 그러면 두 컬럼이 어긋난 행이 생길 수 있다.
-- 복합 FK 가 그 구멍을 **코드가 아니라 스키마로** 막는다 — 어긋난 행을 DB 가 거부한다.
--
-- bot_id 가 NULL 인 동안에는 제약이 통과한다(MATCH SIMPLE). 그래서 nullable 로 두고
-- 백필한 뒤 V17 에서 조이는 순서가 성립한다.

DO $$
DECLARE
  t text;
BEGIN
  FOREACH t IN ARRAY ARRAY[
    'bot_settings', 'allowed_origins',
    'knowledge_sources', 'knowledge_documents', 'knowledge_chunks',
    'faqs', 'conversations', 'messages', 'message_feedback',
    'leads', 'answer_gaps', 'jobs'
  ]
  LOOP
    EXECUTE format('ALTER TABLE %I ADD COLUMN bot_id uuid', t);

    -- 컬럼이 전부 NULL 인 지금 붙이면 검증할 행이 없어 즉시 끝난다.
    EXECUTE format(
      'ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (bot_id, tenant_id)'
      ' REFERENCES bots (id, tenant_id) ON DELETE CASCADE',
      t, t || '_bot_fk');

    EXECUTE format(
      'UPDATE %I c SET bot_id = b.id FROM bots b'
      ' WHERE b.tenant_id = c.tenant_id AND b.is_default', t);

    EXECUTE format('CREATE INDEX ON %I (bot_id)', t);
  END LOOP;
END $$;

-- 봇 설정은 서비스당 하나다. bot_id 가 NULL 일 수 있는 동안에는 부분 유니크로 표현한다.
-- (PK 교체는 V17)
CREATE UNIQUE INDEX bot_settings_bot_id_key ON bot_settings (bot_id) WHERE bot_id IS NOT NULL;

-- ai_usage 는 다르게 다룬다 — §4
ALTER TABLE ai_usage ADD COLUMN bot_id uuid;

COMMENT ON COLUMN ai_usage.bot_id IS
  '어느 서비스가 쓴 원가인가. FK 를 걸지 않는다 — 서비스가 지워져도 원장은 남아야 한다.'
  ' 서비스 구분 이전 호출은 NULL 이며 0 이나 기본값으로 채우지 않는다.';

-- ============================================================
-- 4. ai_usage 는 왜 다른가
-- ============================================================
--
-- **FK 를 걸지 않는다.** 서비스는 지워지는데 원장은 남아야 한다 —
-- CASCADE 면 원장이 지워지고, SET NULL 이면 원가 귀속이 소급 변경된다.
-- 둘 다 CLAUDE.md 의 "원가는 호출 시점 단가로 확정 저장, 소급 금지" 위반이다.
-- `model_prices` 에 FK 를 건 것은 그 테이블을 절대 지우지 않기 때문이다.
--
-- **백필하지 않는다.** 이 테이블이 가장 빨리 자라 UPDATE 가 가장 비싸고, 지금은 업체마다
-- 서비스가 하나뿐이라 채워봐야 정보 가치가 0이다. 과거 구간은 "서비스 구분 이전"으로
-- 화면이 정직하게 표시한다 — 0 이나 기본값으로 채우면 거짓이 된다
-- (V3 의 latency_ms 를 0 으로 채우지 않은 것과 같은 이유).
--
-- **인덱스도 지금 만들지 않는다.** (bot_id, created_at) 은 서비스별 원가 화면이 실제로
-- 생길 때 붙인다. 인덱스는 이 테이블에서 쓰기 비용이다.

-- ============================================================
-- 5. 서비스별 일 집계
-- ============================================================
--
-- **tenant_daily_usage 를 건드리지 않는다.** PK 에 bot_id 를 끼우고 SUM 으로 롤업하면
-- **서비스를 지웠을 때 과거 업체 매출·원가 집계가 함께 사라진다.** 그 테이블은 운영 콘솔
-- (오늘·수익성·업체 목록)의 캐시이고 그 화면들의 단위는 계약=업체다.
-- 같은 배치가 두 축을 각각 채운다.

CREATE TABLE bot_daily_usage (
  bot_id      uuid NOT NULL REFERENCES bots(id) ON DELETE CASCADE,
  day         date NOT NULL,
  conv_count  integer NOT NULL DEFAULT 0,
  saved_count integer NOT NULL DEFAULT 0,   -- 저장 답변으로 처리돼 원가가 0인 건수
  doc_count   integer NOT NULL DEFAULT 0,
  tokens_in   bigint  NOT NULL DEFAULT 0,
  tokens_out  bigint  NOT NULL DEFAULT 0,
  cost_krw    numeric(12,2) NOT NULL DEFAULT 0,
  aggregated_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (bot_id, day)
);
CREATE INDEX ON bot_daily_usage (day);

-- ============================================================
-- 6. 요금제 — 서비스 수 상한
-- ============================================================
--
-- **기존 요금제에 전부 1 을 준다.** 백필로 모든 업체가 정확히 1개를 갖게 되므로
-- 아무도 위반 상태가 되지 않는다 — 이것이 유일하게 안전한 값이다.
--
-- BUSINESS 를 3 으로 박고 싶어지지만 박지 않는다. 그건 가격 정책 결정이고,
-- 마이그레이션에 넣으면 되돌리는 데 또 마이그레이션이 필요하다.
-- plans 는 이미 운영 콘솔에서 편집 가능하다.
--
-- 검사는 **생성 시점에만** 한다. 상한을 나중에 내려도 기존 서비스를 강제로 지우지 않는다 —
-- 이미 남의 사이트에서 도는 위젯을 요금제 변경으로 조용히 죽이면 방문자에게는 장애로 보인다.
-- (sellable=false 로 판매만 중단하고 기존 계약은 남기는 철학과 같다)

ALTER TABLE plans ADD COLUMN bot_limit integer NOT NULL DEFAULT 1;

COMMENT ON COLUMN plans.bot_limit IS
  '이 요금제로 만들 수 있는 서비스 수. 생성 시점에만 검사한다 — 내려도 기존 서비스는 살아남는다.';

-- ============================================================
-- 7. 백필 어서션
-- ============================================================
--
-- 절반만 채워진 채 통과하면 V17 의 SET NOT NULL 에서야 발견되고, 그때는 이미 배포돼 있다.
-- 여기서 실패하면 트랜잭션 전체가 되감긴다.

DO $$
DECLARE
  t text;
  n bigint;
BEGIN
  FOREACH t IN ARRAY ARRAY[
    'bot_settings', 'allowed_origins',
    'knowledge_sources', 'knowledge_documents', 'knowledge_chunks',
    'faqs', 'conversations', 'messages', 'message_feedback',
    'leads', 'answer_gaps', 'jobs'
  ]
  LOOP
    EXECUTE format('SELECT count(*) FROM %I WHERE bot_id IS NULL', t) INTO n;
    IF n > 0 THEN
      RAISE EXCEPTION '백필 누락 — % 에 bot_id 없는 행 %건', t, n;
    END IF;
  END LOOP;

  -- 업체마다 기본 서비스가 정확히 하나여야 한다.
  SELECT count(*) INTO n
  FROM tenants t
  WHERE NOT EXISTS (SELECT 1 FROM bots b WHERE b.tenant_id = t.id AND b.is_default);
  IF n > 0 THEN
    RAISE EXCEPTION '기본 서비스 없는 업체 %건', n;
  END IF;

  -- **키가 그대로인지.** 여기가 틀리면 남의 사이트에 박힌 스크립트가 전부 죽는다.
  SELECT count(*) INTO n
  FROM tenants t JOIN bots b ON b.tenant_id = t.id AND b.is_default
  WHERE b.publishable_key IS DISTINCT FROM t.publishable_key;
  IF n > 0 THEN
    RAISE EXCEPTION '위젯 키가 옮겨지지 않은 업체 %건', n;
  END IF;

  -- 요금제 상한을 이미 넘긴 업체가 있으면 안 된다.
  SELECT count(*) INTO n
  FROM (SELECT b.tenant_id, count(*) AS c FROM bots b GROUP BY b.tenant_id) g
  JOIN tenants t ON t.id = g.tenant_id
  JOIN plans p ON p.id = t.plan_id
  WHERE g.c > p.bot_limit;
  IF n > 0 THEN
    RAISE EXCEPTION '요금제 서비스 상한을 넘긴 업체 %건', n;
  END IF;
END $$;
