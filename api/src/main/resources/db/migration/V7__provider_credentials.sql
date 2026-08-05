-- 공급사 자격증명을 운영 콘솔에서 관리한다.
--
-- 지금까지 API 키는 .env 에만 있었다. 그래서 키를 바꾸려면 재배포가 필요했고,
-- 노출된 키를 급히 교체할 방법이 없었다(docs/IMPROVEMENTS.md P0).
--
-- **시크릿을 DB 에 두는 것은 security-rules 의 "시크릿은 환경변수" 원칙에서 벗어난다.**
-- 그래서 조건을 건다:
--   · 평문 저장 금지 — AES-256-GCM 으로 암호화하며 마스터 키는 여전히 환경변수에만 있다
--   · 응답에 원문을 싣지 않는다 — 마스킹된 힌트만 내려간다
--   · OPS_ADMIN 전용, 변경 시 사유 필수 + 감사 기록
-- 이 DB 는 다른 프로젝트와 공유하는 클러스터이고 pg_hba 가 전면 개방돼 있다(P1).
-- 암호화 없이 두면 DB 를 읽을 수 있는 누구나 우리 공급사 키를 쓸 수 있게 된다.

CREATE TABLE provider_credentials (
  provider       text PRIMARY KEY
                 CHECK (provider IN ('GOOGLE', 'ANTHROPIC', 'OPENAI')),
  -- AES-256-GCM 암호문(base64). 복호화 키는 환경변수 ENCRYPTION_KEY 에만 있다.
  api_key_cipher text NOT NULL,
  -- 화면 표시용. "AIza…4f2c" 처럼 앞뒤 몇 글자만 남긴 값이며 이것만으로는 키를 복원할 수 없다.
  key_hint       text NOT NULL,
  enabled        boolean NOT NULL DEFAULT true,
  updated_by     uuid REFERENCES operators(id),
  updated_at     timestamptz NOT NULL DEFAULT now(),
  created_at     timestamptz NOT NULL DEFAULT now()
);

-- STUB 은 여기 없다. 자격증명이 필요 없는 공급사이고, 행이 없으면 미설정으로 다룬다.

-- ============================================================
-- 임베딩 공급사를 설정값으로
-- ============================================================

-- 지금까지 임베딩 공급사는 환경변수(LLM_DEFAULT_PROVIDER)였다. 답변 공급사는
-- plan_model_assignments 로 DB 에서 고르는데 임베딩만 재배포가 필요했다.
--
-- **이 값을 바꾸면 기존 조각이 전부 무효가 된다** — 다른 모델이 만든 벡터끼리는
-- 거리를 비교할 수 없다. 그래서 문서에 "무엇으로 학습했는지"를 남기고(아래),
-- 설정과 다른 문서는 다시 학습 대상으로 표시한다.
ALTER TABLE cost_guards ADD COLUMN embedding_provider text NOT NULL DEFAULT 'STUB'
  CHECK (embedding_provider IN ('GOOGLE', 'ANTHROPIC', 'OPENAI', 'STUB'));

-- ============================================================
-- 임베딩 출처 기록
-- ============================================================

-- 이게 없으면 "이 조각이 어느 모델로 만들어졌나"를 알 수 없고, 공급사를 바꿨을 때
-- 무엇을 다시 학습해야 하는지 판단할 방법이 없다. 전부 지우고 다시 하는 수밖에 없어진다.
--
-- nullable 이다 — 이 컬럼이 생기기 전에 학습된 문서는 출처를 모른다.
-- 모르는 것은 "다르다"로 취급해 다시 학습 대상에 들어간다(해시 임베딩 시절 조각들이다).
ALTER TABLE knowledge_documents ADD COLUMN embedding_provider text;
ALTER TABLE knowledge_documents ADD COLUMN embedding_model text;

-- ============================================================
-- 감사 대상 행위 확장
-- ============================================================

ALTER TABLE audit_logs DROP CONSTRAINT audit_logs_action_check;
ALTER TABLE audit_logs ADD CONSTRAINT audit_logs_action_check CHECK (action IN (
  'IMPERSONATE', 'VIEW_CONVERSATIONS', 'CHANGE_PLAN',
  'GRANT_QUOTA', 'SUSPEND', 'CHURN', 'EXTEND_TRIAL',
  'MODEL_PRICE_WRITE', 'COST_GUARD_WRITE',
  'ACTIVATE', 'PLAN_WRITE', 'FLAG_WRITE', 'TICKET_WRITE',
  'PROVIDER_CREDENTIAL_WRITE'));
