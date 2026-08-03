-- 답해줘 초기 스키마 (MariaDB 11.8 LTS 이상)
--
-- 11.8 을 요구하는 이유는 VECTOR 타입이 그때 GA 되었기 때문이다. 그 아래 버전에서는
-- knowledge_chunks 가 만들어지지 않는다.
--
-- 규약
--   · 테넌트 소유 도메인 엔티티는 UUID, 원장·로그성 테이블은 BIGINT AUTO_INCREMENT
--   · 시각은 DATETIME(6) 에 UTC 로만 저장한다. MariaDB 에는 timestamptz 가 없으므로
--     타임존은 타입이 아니라 규율로 지킨다 — JDBC connectionTimeZone=UTC +
--     hibernate.jdbc.time_zone=UTC 로 강제하고, 애플리케이션은 OffsetDateTime 을 쓴다.
--     (TIMESTAMP 는 UTC 정규화를 해주지만 2038년 상한이 있어 쓰지 않는다)
--   · enum 은 text + CHECK. 값 추가에 마이그레이션이 필요 없다
--   · 인덱스에 들어가는 문자열 컬럼은 TEXT 가 아니라 길이가 정해진 VARCHAR 여야 한다
--   · 테넌트 소유 엔티티는 예외 없이 tenant_id 를 갖는다

-- ============================================================
-- 운영자
-- ============================================================

CREATE TABLE operators (
  id           UUID PRIMARY KEY,
  email        VARCHAR(320) NOT NULL UNIQUE,
  name         VARCHAR(100) NOT NULL,
  role         VARCHAR(16) NOT NULL CHECK (role IN ('OPS_ADMIN', 'CS', 'SALES', 'DEV')),
  active       BOOLEAN NOT NULL DEFAULT TRUE,
  totp_secret  VARCHAR(64),                -- TODO(stub): 2FA 미구현. SSO 연동 시 대체
  last_seen_at DATETIME(6),
  created_at   DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 요금제
-- ============================================================

-- 요금제는 삭제하지 않는다. sellable=false 로 판매 중단만 — 기존 계약 업체가 남아 있다.
CREATE TABLE plans (
  id          UUID PRIMARY KEY,
  code        VARCHAR(32) NOT NULL UNIQUE,
  name        VARCHAR(60) NOT NULL,
  monthly_fee INT NOT NULL,                -- 원 단위. 협의가(기업)는 0 + negotiable
  negotiable  BOOLEAN NOT NULL DEFAULT FALSE,
  conv_limit  INT NOT NULL,
  doc_limit   INT NOT NULL,
  sellable    BOOLEAN NOT NULL DEFAULT TRUE,
  sort_order  INT NOT NULL DEFAULT 0,
  created_at  DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 업체(테넌트)
-- ============================================================

CREATE TABLE tenants (
  id                UUID PRIMARY KEY,
  name              VARCHAR(120) NOT NULL,
  primary_domain    VARCHAR(253) NOT NULL,
  publishable_key   VARCHAR(64) NOT NULL UNIQUE,
  plan_id           UUID NOT NULL,
  status            VARCHAR(16) NOT NULL
                    CHECK (status IN ('TRIAL', 'ACTIVE', 'SUSPENDED', 'CHURNED')),
  currency          CHAR(3) NOT NULL DEFAULT 'KRW',  -- 몽골 법인 대비. UI 는 당분간 KRW 고정
  trial_ends_at     DATETIME(6),
  next_billing_date DATE,
  churned_at        DATETIME(6),
  purge_after       DATETIME(6),           -- 해지 후 벡터·문서 삭제 예정 시각 (기본 30일 유예)
  created_at        DATETIME(6) NOT NULL,
  updated_at        DATETIME(6) NOT NULL,
  CONSTRAINT fk_tenants_plan FOREIGN KEY (plan_id) REFERENCES plans(id),
  INDEX idx_tenants_status (status),
  INDEX idx_tenants_domain (primary_domain)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 요금제별 답변 모델·조각 수. 원가 추정의 근거가 된다.
CREATE TABLE plan_model_assignments (
  plan_id     UUID PRIMARY KEY,
  provider    VARCHAR(16) NOT NULL,
  model       VARCHAR(64) NOT NULL,
  chunk_count INT NOT NULL DEFAULT 8,
  updated_at  DATETIME(6) NOT NULL,
  CONSTRAINT fk_pma_plan FOREIGN KEY (plan_id) REFERENCES plans(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tenant_members (
  id           UUID PRIMARY KEY,
  tenant_id    UUID NOT NULL,
  email        VARCHAR(320) NOT NULL,
  name         VARCHAR(100),
  role         VARCHAR(16) NOT NULL CHECK (role IN ('OWNER', 'EDITOR', 'VIEWER')),
  invite_state VARCHAR(16) NOT NULL DEFAULT 'ACCEPTED'
               CHECK (invite_state IN ('PENDING', 'ACCEPTED')),
  last_seen_at DATETIME(6),
  created_at   DATETIME(6) NOT NULL,
  CONSTRAINT fk_members_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
  UNIQUE KEY uq_members_tenant_email (tenant_id, email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 위젯이 호출할 수 있는 Origin. 공개 키가 노출돼도 여기 없는 도메인에서는 동작하지 않는다.
CREATE TABLE allowed_origins (
  id             UUID PRIMARY KEY,
  tenant_id      UUID NOT NULL,
  origin         VARCHAR(253) NOT NULL,
  last_called_at DATETIME(6),
  created_at     DATETIME(6) NOT NULL,
  CONSTRAINT fk_origins_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
  UNIQUE KEY uq_origins_tenant_origin (tenant_id, origin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 이번 달 한정 쿼터 증량. 다음 달 자동 원복, 이력만 남는다.
CREATE TABLE quota_overrides (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id   UUID NOT NULL,
  period      DATE NOT NULL,               -- 적용 월의 1일
  conv_delta  INT NOT NULL DEFAULT 0,
  doc_delta   INT NOT NULL DEFAULT 0,
  reason      TEXT NOT NULL,
  operator_id UUID NOT NULL,
  created_at  DATETIME(6) NOT NULL,
  CONSTRAINT fk_quota_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
  CONSTRAINT fk_quota_operator FOREIGN KEY (operator_id) REFERENCES operators(id),
  INDEX idx_quota_tenant_period (tenant_id, period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 내부 메모는 누적이다. 수정·삭제 경로를 만들지 않는다 — 영업·CS 맥락이 담긴다.
CREATE TABLE tenant_notes (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id   UUID NOT NULL,
  body        TEXT NOT NULL,
  operator_id UUID NOT NULL,
  created_at  DATETIME(6) NOT NULL,
  CONSTRAINT fk_notes_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
  CONSTRAINT fk_notes_operator FOREIGN KEY (operator_id) REFERENCES operators(id),
  INDEX idx_notes_tenant_created (tenant_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE billing_records (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id      UUID NOT NULL,
  period         DATE NOT NULL,
  amount         INT NOT NULL,
  status         VARCHAR(16) NOT NULL
                 CHECK (status IN ('PAID', 'FAILED', 'PENDING', 'REFUNDED')),
  attempts       INT NOT NULL DEFAULT 0,
  failure_reason VARCHAR(255),
  receipt_url    VARCHAR(500),             -- TODO(stub): PG 미연동
  created_at     DATETIME(6) NOT NULL,
  CONSTRAINT fk_billing_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
  UNIQUE KEY uq_billing_tenant_period (tenant_id, period),
  INDEX idx_billing_status_created (status, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 대리 로그인 · 감사
-- ============================================================

CREATE TABLE impersonation_sessions (
  id          UUID PRIMARY KEY,
  tenant_id   UUID NOT NULL,
  operator_id UUID NOT NULL,
  reason      TEXT NOT NULL CHECK (TRIM(reason) <> ''),  -- 공백만 입력도 거부
  status      VARCHAR(16) NOT NULL DEFAULT 'ACTIVE'
              CHECK (status IN ('ACTIVE', 'ENDED', 'EXPIRED', 'REVOKED')),
  started_at  DATETIME(6) NOT NULL,
  expires_at  DATETIME(6) NOT NULL,        -- started_at + 30분
  ended_at    DATETIME(6),
  CONSTRAINT fk_imp_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
  CONSTRAINT fk_imp_operator FOREIGN KEY (operator_id) REFERENCES operators(id),
  INDEX idx_imp_tenant_started (tenant_id, started_at DESC),
  INDEX idx_imp_status_expires (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 감사 기록. 수정·삭제 불가, 3년 보존.
CREATE TABLE audit_logs (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  operator_id UUID NOT NULL,
  action      VARCHAR(32) NOT NULL CHECK (action IN (
                'IMPERSONATE', 'VIEW_CONVERSATIONS', 'CHANGE_PLAN',
                'GRANT_QUOTA', 'SUSPEND', 'CHURN', 'EXTEND_TRIAL',
                'MODEL_PRICE_WRITE', 'COST_GUARD_WRITE')),
  tenant_id   UUID,
  reason      TEXT NOT NULL,
  meta        JSON NOT NULL,
  created_at  DATETIME(6) NOT NULL,
  CONSTRAINT fk_audit_operator FOREIGN KEY (operator_id) REFERENCES operators(id),
  CONSTRAINT fk_audit_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  INDEX idx_audit_tenant_created (tenant_id, created_at DESC),
  INDEX idx_audit_operator_created (operator_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 앱 레이어가 실수하거나 뚫려도 감사 기록은 못 고친다.
-- 보존 정책상 삭제가 필요해지면 이 트리거를 지우는 별도 마이그레이션을 명시적으로 만든다.
CREATE TRIGGER audit_logs_no_update BEFORE UPDATE ON audit_logs
  FOR EACH ROW SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'audit_logs is append-only (UPDATE denied)';

CREATE TRIGGER audit_logs_no_delete BEFORE DELETE ON audit_logs
  FOR EACH ROW SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'audit_logs is append-only (DELETE denied)';

-- ============================================================
-- 원가 · 사용량  (이 서비스의 심장)
-- ============================================================

-- 모델 단가 이력. 행을 추가만 하고 수정하지 않는다 — 과거 원가가 소급되면 안 된다.
CREATE TABLE model_prices (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  provider       VARCHAR(16) NOT NULL
                 CHECK (provider IN ('GOOGLE', 'ANTHROPIC', 'OPENAI', 'STUB')),
  model          VARCHAR(64) NOT NULL,
  purpose_kind   VARCHAR(16) NOT NULL CHECK (purpose_kind IN ('GENERATE', 'EMBED')),
  input_per_1m   DECIMAL(12,2) NOT NULL,   -- 원
  output_per_1m  DECIMAL(12,2),            -- 임베딩 모델은 NULL
  effective_from DATETIME(6) NOT NULL,
  note           VARCHAR(255),
  created_at     DATETIME(6) NOT NULL,
  INDEX idx_prices_lookup (provider, model, effective_from DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 모델 호출 원장. LlmGateway 만 여기에 쓴다.
CREATE TABLE ai_usage (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id       UUID NOT NULL,
  purpose         VARCHAR(16) NOT NULL
                  CHECK (purpose IN ('ANSWER', 'EMBED_DOC', 'EMBED_QUERY', 'ETC')),
  provider        VARCHAR(16) NOT NULL,
  model           VARCHAR(64) NOT NULL,
  model_price_id  BIGINT,                  -- 어느 단가로 계산했는지
  input_tokens    INT NOT NULL,
  output_tokens   INT NOT NULL DEFAULT 0,
  cost_krw        DECIMAL(12,4) NOT NULL,  -- 호출 시점 단가로 확정. 소급 변경 금지
  conversation_id UUID,
  created_at      DATETIME(6) NOT NULL,
  CONSTRAINT fk_usage_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  CONSTRAINT fk_usage_price FOREIGN KEY (model_price_id) REFERENCES model_prices(id),
  INDEX idx_usage_tenant_created (tenant_id, created_at),
  INDEX idx_usage_created_purpose (created_at, purpose)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 일 단위 집계. 오늘·업체 목록·수익성 화면은 이 테이블만 읽는다.
CREATE TABLE tenant_daily_usage (
  tenant_id     UUID NOT NULL,
  day           DATE NOT NULL,
  conv_count    INT NOT NULL DEFAULT 0,
  saved_count   INT NOT NULL DEFAULT 0,    -- 저장 답변으로 처리돼 모델 원가가 0인 건수
  doc_count     INT NOT NULL DEFAULT 0,
  tokens_in     BIGINT NOT NULL DEFAULT 0,
  tokens_out    BIGINT NOT NULL DEFAULT 0,
  cost_krw      DECIMAL(12,2) NOT NULL DEFAULT 0,
  aggregated_at DATETIME(6) NOT NULL,      -- 당일분은 이 시각을 화면에 함께 표시
  PRIMARY KEY (tenant_id, day),
  CONSTRAINT fk_daily_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
  INDEX idx_daily_day (day)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 비용 안전장치 + 임계값. 단일 행(id=1)으로 운영한다.
CREATE TABLE cost_guards (
  id                      SMALLINT PRIMARY KEY CHECK (id = 1),
  tenant_daily_cap_krw    INT NOT NULL DEFAULT 20000,
  global_daily_cap_krw    INT NOT NULL DEFAULT 400000,
  ip_questions_per_min    INT NOT NULL DEFAULT 10,
  bulk_upload_limit       INT NOT NULL DEFAULT 100,
  cost_ratio_warn_percent INT NOT NULL DEFAULT 70,
  answer_fail_similarity  DECIMAL(4,3) NOT NULL DEFAULT 0.720,
  default_chunk_count     INT NOT NULL DEFAULT 8,
  answer_max_length       INT NOT NULL DEFAULT 400,
  churn_purge_grace_days  INT NOT NULL DEFAULT 30,
  quota_exceeded_behavior VARCHAR(24) NOT NULL DEFAULT 'STOP_AND_NOTICE'
                          CHECK (quota_exceeded_behavior IN
                                 ('STOP_AND_NOTICE', 'OVERAGE_BILLING', 'NOTIFY_ONLY')),
  slack_alert_enabled     BOOLEAN NOT NULL DEFAULT TRUE,
  common_prompt           MEDIUMTEXT NOT NULL,
  updated_at              DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 지식 · 챗봇
-- ============================================================

CREATE TABLE knowledge_sources (
  id              UUID PRIMARY KEY,
  tenant_id       UUID NOT NULL,
  type            VARCHAR(16) NOT NULL CHECK (type IN ('WEBSITE', 'FILE', 'MANUAL')),
  origin          VARCHAR(500) NOT NULL,
  auto_refresh    BOOLEAN NOT NULL DEFAULT TRUE,
  last_crawled_at DATETIME(6),
  created_at      DATETIME(6) NOT NULL,
  CONSTRAINT fk_sources_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
  INDEX idx_sources_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE knowledge_documents (
  id             UUID PRIMARY KEY,
  tenant_id      UUID NOT NULL,
  source_id      UUID NOT NULL,
  title          VARCHAR(300) NOT NULL,
  path           VARCHAR(500),             -- 웹페이지 경로 또는 파일명
  status         VARCHAR(16) NOT NULL DEFAULT 'PENDING'
                 CHECK (status IN ('PENDING', 'PROCESSING', 'INDEXED', 'FAILED', 'EXCLUDED')),
  error_code     VARCHAR(64),
  chunk_count    INT NOT NULL DEFAULT 0,
  size_bytes     BIGINT,
  content_sha256 CHAR(64),                 -- 재크롤링 시 미변경 문서를 건너뛰는 근거
  indexed_at     DATETIME(6),
  created_at     DATETIME(6) NOT NULL,
  CONSTRAINT fk_docs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
  CONSTRAINT fk_docs_source FOREIGN KEY (source_id) REFERENCES knowledge_sources(id) ON DELETE CASCADE,
  INDEX idx_docs_tenant_status (tenant_id, status),
  INDEX idx_docs_source (source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 임베딩 조각. 차원 변경은 전체 재임베딩을 뜻한다 — CLAUDE.md 핵심 결정 참조.
--
-- VECTOR INDEX 는 WHERE 없이 `ORDER BY VEC_DISTANCE_COSINE(...) LIMIT n` 인 질의에서만
-- 사용된다. 우리는 테넌트 격리 때문에 WHERE tenant_id = ? 가 항상 붙으므로 인덱스를
-- 타지 못하고 테넌트 범위 스캔이 될 수 있다. 정확성이 먼저이므로 필터를 빼지 않는다.
-- 성능이 문제가 되면 테넌트별 파티셔닝을 검토한다 (docs/IMPROVEMENTS.md).
CREATE TABLE knowledge_chunks (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id   UUID NOT NULL,
  document_id UUID NOT NULL,
  ordinal     INT NOT NULL,
  content     MEDIUMTEXT NOT NULL,
  embedding   VECTOR(1536) NOT NULL,
  created_at  DATETIME(6) NOT NULL,
  CONSTRAINT fk_chunks_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
  CONSTRAINT fk_chunks_document FOREIGN KEY (document_id) REFERENCES knowledge_documents(id) ON DELETE CASCADE,
  UNIQUE KEY uq_chunks_doc_ordinal (document_id, ordinal),
  INDEX idx_chunks_tenant (tenant_id),
  VECTOR INDEX vec_chunks_embedding (embedding) DISTANCE=cosine
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 공통 질문 = 저장 답변. 모델을 거치지 않으므로 ai_usage 에 기록되지 않는다.
CREATE TABLE faqs (
  id         UUID PRIMARY KEY,
  tenant_id  UUID NOT NULL,
  question   VARCHAR(300) NOT NULL,
  answer     MEDIUMTEXT NOT NULL,
  links      JSON NOT NULL,
  shown      BOOLEAN NOT NULL DEFAULT TRUE,  -- 버튼 노출 여부. false 여도 직접 입력하면 매칭된다
  sort_order INT NOT NULL DEFAULT 0,
  hit_count  INT NOT NULL DEFAULT 0,
  embedding  VECTOR(1536),                   -- 질문 유사도 매칭용
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_faqs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
  INDEX idx_faqs_tenant_order (tenant_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE conversations (
  id              UUID PRIMARY KEY,
  tenant_id       UUID NOT NULL,
  started_path    VARCHAR(500),
  visitor_region  VARCHAR(60),
  visitor_ip_hash CHAR(64),                 -- 원문 저장 금지. 레이트 리밋·중복 판별용 해시
  started_at      DATETIME(6) NOT NULL,
  ended_at        DATETIME(6),
  CONSTRAINT fk_conv_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
  INDEX idx_conv_tenant_started (tenant_id, started_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE messages (
  id              UUID PRIMARY KEY,
  tenant_id       UUID NOT NULL,
  conversation_id UUID NOT NULL,
  role            VARCHAR(16) NOT NULL CHECK (role IN ('VISITOR', 'BOT')),
  content         MEDIUMTEXT NOT NULL,
  answered        BOOLEAN,                  -- BOT 메시지만. false = 답변 실패
  saved           BOOLEAN NOT NULL DEFAULT FALSE,  -- 저장 답변으로 나갔는가
  faq_id          UUID,
  created_at      DATETIME(6) NOT NULL,
  CONSTRAINT fk_msg_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
  CONSTRAINT fk_msg_conv FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
  CONSTRAINT fk_msg_faq FOREIGN KEY (faq_id) REFERENCES faqs(id),
  INDEX idx_msg_conv_created (conversation_id, created_at),
  -- MariaDB 에는 부분 인덱스가 없다. 답변 개선 화면은 answered=false 만 보므로
  -- PostgreSQL 판에서는 WHERE 절이 붙어 있었다. 여기서는 전체 인덱스로 둔다.
  INDEX idx_msg_answer_gaps (tenant_id, answered, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 답변에 사용한 문서. PostgreSQL 판에서는 messages.source_document_ids uuid[] 였다.
-- MariaDB 에는 배열 타입이 없어 조인 테이블로 편다.
CREATE TABLE message_sources (
  message_id  UUID NOT NULL,
  document_id UUID NOT NULL,
  PRIMARY KEY (message_id, document_id),
  CONSTRAINT fk_msgsrc_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
  CONSTRAINT fk_msgsrc_document FOREIGN KEY (document_id) REFERENCES knowledge_documents(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE message_feedback (
  message_id UUID PRIMARY KEY,
  tenant_id  UUID NOT NULL,
  helpful    BOOLEAN NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_fb_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
  CONSTRAINT fk_fb_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
  INDEX idx_fb_tenant_helpful (tenant_id, helpful)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE leads (
  id              UUID PRIMARY KEY,
  tenant_id       UUID NOT NULL,
  conversation_id UUID,
  name            VARCHAR(100) NOT NULL,
  contact         VARCHAR(255) NOT NULL,   -- 화면 노출 시 마스킹. 원문은 CSV 내보내기에서만
  reason          VARCHAR(500),
  status          VARCHAR(16) NOT NULL DEFAULT 'NEW'
                  CHECK (status IN ('NEW', 'CONTACTED', 'CLOSED')),
  created_at      DATETIME(6) NOT NULL,
  CONSTRAINT fk_leads_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
  CONSTRAINT fk_leads_conv FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE SET NULL,
  INDEX idx_leads_tenant_status (tenant_id, status, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 작업 큐 · 기능 공개 · 문의
-- ============================================================

CREATE TABLE jobs (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id    UUID NOT NULL,
  kind         VARCHAR(16) NOT NULL CHECK (kind IN ('CRAWL', 'RECRAWL', 'EMBED_DOC')),
  target       VARCHAR(500) NOT NULL,
  document_id  UUID,
  status       VARCHAR(16) NOT NULL DEFAULT 'QUEUED'
               CHECK (status IN ('QUEUED', 'RUNNING', 'DONE', 'FAILED')),
  error_code   VARCHAR(64),
  attempts     INT NOT NULL DEFAULT 0,
  max_attempts INT NOT NULL DEFAULT 3,
  created_at   DATETIME(6) NOT NULL,
  updated_at   DATETIME(6) NOT NULL,
  CONSTRAINT fk_jobs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
  CONSTRAINT fk_jobs_document FOREIGN KEY (document_id) REFERENCES knowledge_documents(id) ON DELETE CASCADE,
  INDEX idx_jobs_status_updated (status, updated_at DESC),
  INDEX idx_jobs_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE feature_flags (
  `key`          VARCHAR(64) PRIMARY KEY,
  name           VARCHAR(120) NOT NULL,
  description    VARCHAR(500),
  scope          VARCHAR(16) NOT NULL CHECK (scope IN ('INTERNAL', 'TENANTS', 'PLAN', 'ALL')),
  target_plan_id UUID,
  enabled        BOOLEAN NOT NULL DEFAULT FALSE,
  updated_at     DATETIME(6) NOT NULL,
  CONSTRAINT fk_flags_plan FOREIGN KEY (target_plan_id) REFERENCES plans(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- PostgreSQL 판에서는 feature_flags.target_ids uuid[] 였다. 배열 타입이 없어 조인 테이블로 편다.
CREATE TABLE feature_flag_tenants (
  flag_key  VARCHAR(64) NOT NULL,
  tenant_id UUID NOT NULL,
  PRIMARY KEY (flag_key, tenant_id),
  CONSTRAINT fk_fft_flag FOREIGN KEY (flag_key) REFERENCES feature_flags(`key`) ON DELETE CASCADE,
  CONSTRAINT fk_fft_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tickets (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id   UUID NOT NULL,
  subject     VARCHAR(300) NOT NULL,
  body        MEDIUMTEXT NOT NULL,
  status      VARCHAR(16) NOT NULL DEFAULT 'OPEN'
              CHECK (status IN ('OPEN', 'ANSWERED', 'CLOSED')),
  answered_by UUID,
  answered_at DATETIME(6),
  created_at  DATETIME(6) NOT NULL,
  CONSTRAINT fk_tickets_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
  CONSTRAINT fk_tickets_operator FOREIGN KEY (answered_by) REFERENCES operators(id),
  INDEX idx_tickets_status_created (status, created_at),
  INDEX idx_tickets_tenant_created (tenant_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 시드
-- ============================================================

INSERT INTO cost_guards (id, common_prompt, updated_at) VALUES (1,
  '주어진 문서 조각만을 근거로 답한다. 조각에 없는 내용은 추측하지 않고 모른다고 답한다. 답변에는 사용한 문서의 제목을 함께 밝힌다. 방문자의 개인정보를 묻지 않는다.',
  UTC_TIMESTAMP(6));

INSERT INTO plans (id, code, name, monthly_fee, negotiable, conv_limit, doc_limit, sellable, sort_order, created_at) VALUES
  (UUID(), 'TRIAL',       '무료 체험',   0,      FALSE, 100,    30,     TRUE,  1, UTC_TIMESTAMP(6)),
  (UUID(), 'STARTER',     '스타터',      39000,  FALSE, 1000,   100,    TRUE,  2, UTC_TIMESTAMP(6)),
  (UUID(), 'BUSINESS',    '비즈니스',    89000,  FALSE, 3000,   500,    TRUE,  3, UTC_TIMESTAMP(6)),
  (UUID(), 'ENTERPRISE',  '기업',        0,      TRUE,  999999, 999999, TRUE,  4, UTC_TIMESTAMP(6)),
  (UUID(), 'LITE_LEGACY', '라이트 (구)', 19000,  FALSE, 300,    30,     FALSE, 5, UTC_TIMESTAMP(6));

-- 단가는 2026-08-03 공급사 공시가를 1,400 KRW/USD 로 환산한 값이다.
-- 운영 개시 전에 실단가와 환율을 확인하고 새 행을 추가할 것 (docs/IMPROVEMENTS.md P0).
-- 기존 행은 수정하지 않는다 — 과거 ai_usage 가 소급되면 안 된다.
INSERT INTO model_prices (provider, model, purpose_kind, input_per_1m, output_per_1m, effective_from, note, created_at) VALUES
  ('GOOGLE',    'gemini-3.5-flash',      'GENERATE', 2100.00, 12600.00, '2026-08-01 00:00:00', '$1.50/$9.00', UTC_TIMESTAMP(6)),
  ('GOOGLE',    'gemini-3.5-flash-lite', 'GENERATE',  420.00,  3500.00, '2026-08-01 00:00:00', '$0.30/$2.50', UTC_TIMESTAMP(6)),
  ('GOOGLE',    'gemini-3.1-flash-lite', 'GENERATE',  350.00,  2100.00, '2026-08-01 00:00:00', '$0.25/$1.50', UTC_TIMESTAMP(6)),
  ('GOOGLE',    'gemini-embedding-001',  'EMBED',     210.00,     NULL, '2026-08-01 00:00:00', '$0.15 · 3072차원을 1536으로 절단', UTC_TIMESTAMP(6)),
  ('ANTHROPIC', 'claude-opus-5',         'GENERATE', 7000.00, 35000.00, '2026-08-01 00:00:00', '$5/$25 · 기업 요금제 검토용', UTC_TIMESTAMP(6)),
  ('ANTHROPIC', 'claude-sonnet-5',       'GENERATE', 4200.00, 21000.00, '2026-08-01 00:00:00', '$3/$15', UTC_TIMESTAMP(6)),
  ('ANTHROPIC', 'claude-haiku-4-5',      'GENERATE', 1400.00,  7000.00, '2026-08-01 00:00:00', '$1/$5', UTC_TIMESTAMP(6));

INSERT INTO plan_model_assignments (plan_id, provider, model, chunk_count, updated_at)
SELECT id, 'GOOGLE', 'gemini-3.1-flash-lite', 5,  UTC_TIMESTAMP(6) FROM plans WHERE code = 'TRIAL'
UNION ALL
SELECT id, 'GOOGLE', 'gemini-3.5-flash-lite', 8,  UTC_TIMESTAMP(6) FROM plans WHERE code = 'STARTER'
UNION ALL
SELECT id, 'GOOGLE', 'gemini-3.5-flash',      8,  UTC_TIMESTAMP(6) FROM plans WHERE code = 'BUSINESS'
UNION ALL
SELECT id, 'GOOGLE', 'gemini-3.5-flash',     10,  UTC_TIMESTAMP(6) FROM plans WHERE code = 'ENTERPRISE'
UNION ALL
SELECT id, 'GOOGLE', 'gemini-3.5-flash-lite', 8,  UTC_TIMESTAMP(6) FROM plans WHERE code = 'LITE_LEGACY';
