-- 답해줘 초기 스키마
--
-- 기획서 DDL(docs/plan/*.md)이 기준. 참조만 있고 정의가 없던 테이블(plans, operators,
-- impersonation_sessions)과 업체 대시보드/위젯 도메인을 채웠다.
--
-- 규약
--   · 테넌트 소유 도메인 엔티티는 uuid, 원장·로그성 테이블은 bigserial
--   · 시각은 전부 timestamptz (JPA 는 OffsetDateTime 으로 받는다)
--   · enum 은 PG enum 타입 대신 text + CHECK — 값 추가에 마이그레이션이 필요 없다
--   · 테넌트 소유 엔티티는 예외 없이 tenant_id 를 갖는다

CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================================
-- 운영자
-- ============================================================

CREATE TABLE operators (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  email        text NOT NULL UNIQUE,
  name         text NOT NULL,
  role         text NOT NULL CHECK (role IN ('OPS_ADMIN', 'CS', 'SALES', 'DEV')),
  active       boolean NOT NULL DEFAULT true,
  totp_secret  text,                      -- TODO(stub): 2FA 미구현. SSO 연동 시 대체
  last_seen_at timestamptz,
  created_at   timestamptz NOT NULL DEFAULT now()
);

-- ============================================================
-- 요금제
-- ============================================================

-- 요금제는 삭제하지 않는다. sellable=false 로 판매 중단만 — 기존 계약 업체가 남아 있다.
CREATE TABLE plans (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code        text NOT NULL UNIQUE,
  name        text NOT NULL,
  monthly_fee integer NOT NULL,           -- 원 단위. 협의가(기업)는 0 + negotiable
  negotiable  boolean NOT NULL DEFAULT false,
  conv_limit  integer NOT NULL,
  doc_limit   integer NOT NULL,
  sellable    boolean NOT NULL DEFAULT true,
  sort_order  integer NOT NULL DEFAULT 0,
  created_at  timestamptz NOT NULL DEFAULT now()
);

-- ============================================================
-- 업체(테넌트)
-- ============================================================

CREATE TABLE tenants (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name            text NOT NULL,
  primary_domain  text NOT NULL,
  publishable_key text NOT NULL UNIQUE,
  plan_id         uuid NOT NULL REFERENCES plans(id),
  status          text NOT NULL CHECK (status IN ('TRIAL', 'ACTIVE', 'SUSPENDED', 'CHURNED')),
  currency        text NOT NULL DEFAULT 'KRW',  -- 몽골 법인 대비. UI 는 당분간 KRW 고정
  trial_ends_at   timestamptz,
  next_billing_date date,
  churned_at      timestamptz,
  purge_after     timestamptz,            -- 해지 후 벡터·문서 삭제 예정 시각 (기본 30일 유예)
  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON tenants (status);
CREATE INDEX ON tenants (primary_domain);

-- 요금제별 답변 모델·조각 수. 원가 추정의 근거가 된다.
CREATE TABLE plan_model_assignments (
  plan_id     uuid PRIMARY KEY REFERENCES plans(id) ON DELETE CASCADE,
  provider    text NOT NULL,
  model       text NOT NULL,
  chunk_count integer NOT NULL DEFAULT 8,
  updated_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE tenant_members (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id    uuid NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  email        text NOT NULL,
  name         text,
  role         text NOT NULL CHECK (role IN ('OWNER', 'EDITOR', 'VIEWER')),
  invite_state text NOT NULL DEFAULT 'ACCEPTED'
               CHECK (invite_state IN ('PENDING', 'ACCEPTED')),
  last_seen_at timestamptz,
  created_at   timestamptz NOT NULL DEFAULT now(),
  UNIQUE (tenant_id, email)
);

-- 위젯이 호출할 수 있는 Origin. 공개 키가 노출돼도 여기 없는 도메인에서는 동작하지 않는다.
CREATE TABLE allowed_origins (
  id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id      uuid NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  origin         text NOT NULL,
  last_called_at timestamptz,
  created_at     timestamptz NOT NULL DEFAULT now(),
  UNIQUE (tenant_id, origin)
);

-- 이번 달 한정 쿼터 증량. 다음 달 자동 원복, 이력만 남는다.
CREATE TABLE quota_overrides (
  id          bigserial PRIMARY KEY,
  tenant_id   uuid NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  period      date NOT NULL,             -- 적용 월의 1일
  conv_delta  integer NOT NULL DEFAULT 0,
  doc_delta   integer NOT NULL DEFAULT 0,
  reason      text NOT NULL,
  operator_id uuid NOT NULL REFERENCES operators(id),
  created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON quota_overrides (tenant_id, period);

-- 내부 메모는 누적이다. 수정·삭제 경로를 만들지 않는다 — 영업·CS 맥락이 담긴다.
CREATE TABLE tenant_notes (
  id          bigserial PRIMARY KEY,
  tenant_id   uuid NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  body        text NOT NULL,
  operator_id uuid NOT NULL REFERENCES operators(id),
  created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON tenant_notes (tenant_id, created_at DESC);

CREATE TABLE billing_records (
  id          bigserial PRIMARY KEY,
  tenant_id   uuid NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  period      date NOT NULL,
  amount      integer NOT NULL,
  status      text NOT NULL CHECK (status IN ('PAID', 'FAILED', 'PENDING', 'REFUNDED')),
  attempts    integer NOT NULL DEFAULT 0,
  failure_reason text,
  receipt_url text,                       -- TODO(stub): PG 미연동
  created_at  timestamptz NOT NULL DEFAULT now(),
  UNIQUE (tenant_id, period)
);
CREATE INDEX ON billing_records (status, created_at DESC);

-- ============================================================
-- 대리 로그인 · 감사
-- ============================================================

CREATE TABLE impersonation_sessions (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id   uuid NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  operator_id uuid NOT NULL REFERENCES operators(id),
  reason      text NOT NULL CHECK (btrim(reason) <> ''),  -- 공백만 입력도 거부
  status      text NOT NULL DEFAULT 'ACTIVE'
              CHECK (status IN ('ACTIVE', 'ENDED', 'EXPIRED', 'REVOKED')),
  started_at  timestamptz NOT NULL DEFAULT now(),
  expires_at  timestamptz NOT NULL,       -- started_at + 30분
  ended_at    timestamptz
);
CREATE INDEX ON impersonation_sessions (tenant_id, started_at DESC);
CREATE INDEX ON impersonation_sessions (status, expires_at);

-- 감사 기록. 수정·삭제 불가, 3년 보존.
CREATE TABLE audit_logs (
  id          bigserial PRIMARY KEY,
  operator_id uuid NOT NULL REFERENCES operators(id),
  action      text NOT NULL CHECK (action IN (
                'IMPERSONATE', 'VIEW_CONVERSATIONS', 'CHANGE_PLAN',
                'GRANT_QUOTA', 'SUSPEND', 'CHURN', 'EXTEND_TRIAL',
                'MODEL_PRICE_WRITE', 'COST_GUARD_WRITE')),
  tenant_id   uuid REFERENCES tenants(id),
  reason      text NOT NULL,
  meta        jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON audit_logs (tenant_id, created_at DESC);
CREATE INDEX ON audit_logs (operator_id, created_at DESC);

-- 앱 레이어가 실수하거나 뚫려도 감사 기록은 못 고친다.
-- 보존 정책상 삭제가 필요해지면 이 트리거를 지우는 별도 마이그레이션을 명시적으로 만든다.
CREATE OR REPLACE FUNCTION audit_logs_immutable() RETURNS trigger AS $$
BEGIN
  RAISE EXCEPTION 'audit_logs is append-only (attempted %)', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_logs_no_update BEFORE UPDATE ON audit_logs
  FOR EACH ROW EXECUTE FUNCTION audit_logs_immutable();
CREATE TRIGGER audit_logs_no_delete BEFORE DELETE ON audit_logs
  FOR EACH ROW EXECUTE FUNCTION audit_logs_immutable();

-- ============================================================
-- 원가 · 사용량  (이 서비스의 심장)
-- ============================================================

-- 모델 단가 이력. 행을 추가만 하고 수정하지 않는다 — 과거 원가가 소급되면 안 된다.
CREATE TABLE model_prices (
  id             bigserial PRIMARY KEY,
  provider       text NOT NULL CHECK (provider IN ('GOOGLE', 'ANTHROPIC', 'OPENAI', 'STUB')),
  model          text NOT NULL,
  purpose_kind   text NOT NULL CHECK (purpose_kind IN ('GENERATE', 'EMBED')),
  input_per_1m   numeric(12,2) NOT NULL,  -- 원
  output_per_1m  numeric(12,2),           -- 임베딩 모델은 NULL
  effective_from timestamptz NOT NULL,
  note           text,
  created_at     timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON model_prices (provider, model, effective_from DESC);

-- 모델 호출 원장. LlmGateway 만 여기에 쓴다.
CREATE TABLE ai_usage (
  id            bigserial PRIMARY KEY,
  tenant_id     uuid NOT NULL REFERENCES tenants(id),
  purpose       text NOT NULL CHECK (purpose IN ('ANSWER', 'EMBED_DOC', 'EMBED_QUERY', 'ETC')),
  provider      text NOT NULL,
  model         text NOT NULL,
  model_price_id bigint REFERENCES model_prices(id),  -- 어느 단가로 계산했는지
  input_tokens  integer NOT NULL,
  output_tokens integer NOT NULL DEFAULT 0,
  cost_krw      numeric(12,4) NOT NULL,   -- 호출 시점 단가로 확정. 소급 변경 금지
  conversation_id uuid,
  created_at    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON ai_usage (tenant_id, created_at);
CREATE INDEX ON ai_usage (created_at, purpose);

-- 일 단위 집계. 오늘·업체 목록·수익성 화면은 이 테이블만 읽는다.
CREATE TABLE tenant_daily_usage (
  tenant_id   uuid NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  day         date NOT NULL,
  conv_count  integer NOT NULL DEFAULT 0,
  saved_count integer NOT NULL DEFAULT 0,  -- 저장 답변으로 처리돼 원가가 0인 건수
  doc_count   integer NOT NULL DEFAULT 0,
  tokens_in   bigint NOT NULL DEFAULT 0,
  tokens_out  bigint NOT NULL DEFAULT 0,
  cost_krw    numeric(12,2) NOT NULL DEFAULT 0,
  aggregated_at timestamptz NOT NULL DEFAULT now(),  -- 당일분은 이 시각을 화면에 함께 표시
  PRIMARY KEY (tenant_id, day)
);
CREATE INDEX ON tenant_daily_usage (day);

-- 비용 안전장치 + 임계값. 단일 행(id=1)으로 운영한다.
CREATE TABLE cost_guards (
  id                       smallint PRIMARY KEY DEFAULT 1 CHECK (id = 1),
  tenant_daily_cap_krw     integer NOT NULL DEFAULT 20000,
  global_daily_cap_krw     integer NOT NULL DEFAULT 400000,
  ip_questions_per_min     integer NOT NULL DEFAULT 10,
  bulk_upload_limit        integer NOT NULL DEFAULT 100,
  cost_ratio_warn_percent  integer NOT NULL DEFAULT 70,
  answer_fail_similarity   numeric(4,3) NOT NULL DEFAULT 0.720,
  default_chunk_count      integer NOT NULL DEFAULT 8,
  answer_max_length        integer NOT NULL DEFAULT 400,
  churn_purge_grace_days   integer NOT NULL DEFAULT 30,
  quota_exceeded_behavior  text NOT NULL DEFAULT 'STOP_AND_NOTICE'
                           CHECK (quota_exceeded_behavior IN ('STOP_AND_NOTICE', 'OVERAGE_BILLING', 'NOTIFY_ONLY')),
  slack_alert_enabled      boolean NOT NULL DEFAULT true,
  common_prompt            text NOT NULL DEFAULT '',
  updated_at               timestamptz NOT NULL DEFAULT now()
);

-- ============================================================
-- 지식 · 챗봇
-- ============================================================

CREATE TABLE knowledge_sources (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id       uuid NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  type            text NOT NULL CHECK (type IN ('WEBSITE', 'FILE', 'MANUAL')),
  origin          text NOT NULL,
  auto_refresh    boolean NOT NULL DEFAULT true,
  last_crawled_at timestamptz,
  created_at      timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON knowledge_sources (tenant_id);

CREATE TABLE knowledge_documents (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id   uuid NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  source_id   uuid NOT NULL REFERENCES knowledge_sources(id) ON DELETE CASCADE,
  title       text NOT NULL,
  path        text,                        -- 웹페이지 경로 또는 파일명
  status      text NOT NULL DEFAULT 'PENDING'
              CHECK (status IN ('PENDING', 'PROCESSING', 'INDEXED', 'FAILED', 'EXCLUDED')),
  error_code  text,
  chunk_count integer NOT NULL DEFAULT 0,
  size_bytes  bigint,
  content_sha256 text,                     -- 재크롤링 시 미변경 문서를 건너뛰는 근거
  indexed_at  timestamptz,
  created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON knowledge_documents (tenant_id, status);
CREATE INDEX ON knowledge_documents (source_id);

-- 임베딩 조각. 차원 변경은 전체 재임베딩을 뜻한다 — CLAUDE.md 핵심 결정 참조.
CREATE TABLE knowledge_chunks (
  id          bigserial PRIMARY KEY,
  tenant_id   uuid NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  document_id uuid NOT NULL REFERENCES knowledge_documents(id) ON DELETE CASCADE,
  ordinal     integer NOT NULL,
  content     text NOT NULL,
  embedding   vector(1536) NOT NULL,
  created_at  timestamptz NOT NULL DEFAULT now(),
  UNIQUE (document_id, ordinal)
);
-- 테넌트 격리가 먼저다. 벡터 검색은 항상 tenant_id 로 좁힌 뒤 수행한다.
CREATE INDEX ON knowledge_chunks (tenant_id);
CREATE INDEX knowledge_chunks_embedding_idx ON knowledge_chunks
  USING hnsw (embedding vector_cosine_ops);

-- 공통 질문 = 저장 답변. 모델을 거치지 않으므로 ai_usage 에 기록되지 않는다.
CREATE TABLE faqs (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id  uuid NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  question   text NOT NULL,
  answer     text NOT NULL,
  links      jsonb NOT NULL DEFAULT '[]'::jsonb,
  shown      boolean NOT NULL DEFAULT true,   -- 버튼 노출 여부. false 여도 직접 입력하면 매칭된다
  sort_order integer NOT NULL DEFAULT 0,
  hit_count  integer NOT NULL DEFAULT 0,
  embedding  vector(1536),                    -- 질문 유사도 매칭용
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON faqs (tenant_id, sort_order);

CREATE TABLE conversations (
  id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id      uuid NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  started_path   text,
  visitor_region text,
  visitor_ip_hash text,                     -- 원문 저장 금지. 레이트 리밋·중복 판별용 해시
  started_at     timestamptz NOT NULL DEFAULT now(),
  ended_at       timestamptz
);
CREATE INDEX ON conversations (tenant_id, started_at DESC);

CREATE TABLE messages (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id       uuid NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  conversation_id uuid NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
  role            text NOT NULL CHECK (role IN ('VISITOR', 'BOT')),
  content         text NOT NULL,
  answered        boolean,                  -- BOT 메시지만. false = 답변 실패
  saved           boolean NOT NULL DEFAULT false,  -- 저장 답변으로 나갔는가
  faq_id          uuid REFERENCES faqs(id),
  source_document_ids uuid[] NOT NULL DEFAULT '{}',
  created_at      timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON messages (conversation_id, created_at);
-- 답변 개선 화면의 조회 조건
CREATE INDEX ON messages (tenant_id, answered, created_at DESC) WHERE answered = false;

CREATE TABLE message_feedback (
  message_id uuid PRIMARY KEY REFERENCES messages(id) ON DELETE CASCADE,
  tenant_id  uuid NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  helpful    boolean NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON message_feedback (tenant_id, helpful);

CREATE TABLE leads (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id       uuid NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  conversation_id uuid REFERENCES conversations(id) ON DELETE SET NULL,
  name            text NOT NULL,
  contact         text NOT NULL,           -- 화면 노출 시 마스킹. 원문은 CSV 내보내기에서만
  reason          text,
  status          text NOT NULL DEFAULT 'NEW'
                  CHECK (status IN ('NEW', 'CONTACTED', 'CLOSED')),
  created_at      timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON leads (tenant_id, status, created_at DESC);

-- ============================================================
-- 작업 큐 · 기능 공개 · 문의
-- ============================================================

CREATE TABLE jobs (
  id           bigserial PRIMARY KEY,
  tenant_id    uuid NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  kind         text NOT NULL CHECK (kind IN ('CRAWL', 'RECRAWL', 'EMBED_DOC')),
  target       text NOT NULL,
  document_id  uuid REFERENCES knowledge_documents(id) ON DELETE CASCADE,
  status       text NOT NULL DEFAULT 'QUEUED'
               CHECK (status IN ('QUEUED', 'RUNNING', 'DONE', 'FAILED')),
  error_code   text,
  attempts     integer NOT NULL DEFAULT 0,
  max_attempts integer NOT NULL DEFAULT 3,
  created_at   timestamptz NOT NULL DEFAULT now(),
  updated_at   timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON jobs (status, updated_at DESC);
CREATE INDEX ON jobs (tenant_id, status);

CREATE TABLE feature_flags (
  key         text PRIMARY KEY,
  name        text NOT NULL,
  description text,
  scope       text NOT NULL CHECK (scope IN ('INTERNAL', 'TENANTS', 'PLAN', 'ALL')),
  target_tenant_ids uuid[] NOT NULL DEFAULT '{}',
  target_plan_id    uuid REFERENCES plans(id),
  enabled     boolean NOT NULL DEFAULT false,
  updated_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE tickets (
  id          bigserial PRIMARY KEY,
  tenant_id   uuid NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  subject     text NOT NULL,
  body        text NOT NULL,
  status      text NOT NULL DEFAULT 'OPEN'
              CHECK (status IN ('OPEN', 'ANSWERED', 'CLOSED')),
  answered_by uuid REFERENCES operators(id),
  answered_at timestamptz,
  created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON tickets (status, created_at);
CREATE INDEX ON tickets (tenant_id, created_at DESC);

-- ============================================================
-- 시드
-- ============================================================

INSERT INTO cost_guards (id, common_prompt) VALUES (1,
  '주어진 문서 조각만을 근거로 답한다. 조각에 없는 내용은 추측하지 않고 모른다고 답한다. '
  '답변에는 사용한 문서의 제목을 함께 밝힌다. 방문자의 개인정보를 묻지 않는다.');

INSERT INTO plans (code, name, monthly_fee, negotiable, conv_limit, doc_limit, sellable, sort_order) VALUES
  ('TRIAL',      '무료 체험',   0,      false,  100,     30,      true,  1),
  ('STARTER',    '스타터',      39000,  false,  1000,    100,     true,  2),
  ('BUSINESS',   '비즈니스',    89000,  false,  3000,    500,     true,  3),
  ('ENTERPRISE', '기업',        0,      true,   999999,  999999,  true,  4),
  ('LITE_LEGACY','라이트 (구)', 19000,  false,  300,     30,      false, 5);

-- 단가는 2026-08-03 공급사 공시가를 1,400 KRW/USD 로 환산한 값이다.
-- 운영 개시 전에 실단가와 환율을 확인하고 새 행을 추가할 것 (docs/IMPROVEMENTS.md P0).
-- 기존 행은 수정하지 않는다 — 과거 ai_usage 가 소급되면 안 된다.
INSERT INTO model_prices (provider, model, purpose_kind, input_per_1m, output_per_1m, effective_from, note) VALUES
  ('GOOGLE',    'gemini-3.5-flash',      'GENERATE', 2100.00, 12600.00, '2026-08-01T00:00:00Z', '$1.50/$9.00'),
  ('GOOGLE',    'gemini-3.5-flash-lite', 'GENERATE',  420.00,  3500.00, '2026-08-01T00:00:00Z', '$0.30/$2.50'),
  ('GOOGLE',    'gemini-3.1-flash-lite', 'GENERATE',  350.00,  2100.00, '2026-08-01T00:00:00Z', '$0.25/$1.50'),
  ('GOOGLE',    'gemini-embedding-001',  'EMBED',     210.00,     NULL, '2026-08-01T00:00:00Z', '$0.15 · 3072차원을 1536으로 절단'),
  ('ANTHROPIC', 'claude-opus-5',         'GENERATE', 7000.00, 35000.00, '2026-08-01T00:00:00Z', '$5/$25 · 기업 요금제 검토용'),
  ('ANTHROPIC', 'claude-sonnet-5',       'GENERATE', 4200.00, 21000.00, '2026-08-01T00:00:00Z', '$3/$15'),
  ('ANTHROPIC', 'claude-haiku-4-5',      'GENERATE', 1400.00,  7000.00, '2026-08-01T00:00:00Z', '$1/$5');

INSERT INTO plan_model_assignments (plan_id, provider, model, chunk_count)
SELECT id, 'GOOGLE', 'gemini-3.1-flash-lite', 5  FROM plans WHERE code = 'TRIAL'
UNION ALL
SELECT id, 'GOOGLE', 'gemini-3.5-flash-lite', 8  FROM plans WHERE code = 'STARTER'
UNION ALL
SELECT id, 'GOOGLE', 'gemini-3.5-flash',      8  FROM plans WHERE code = 'BUSINESS'
UNION ALL
SELECT id, 'GOOGLE', 'gemini-3.5-flash',     10  FROM plans WHERE code = 'ENTERPRISE'
UNION ALL
SELECT id, 'GOOGLE', 'gemini-3.5-flash-lite', 8  FROM plans WHERE code = 'LITE_LEGACY';
