-- 업체 대시보드(/api/app) 를 붙이면서 드러난 스키마 구멍을 채운다.
--
-- V1 은 운영 콘솔 기준으로 짜여 있어 업체 담당자가 "직접 로그인해서 자기 챗봇을 설정한다"는
-- 흐름에 필요한 것이 빠져 있었다. 프로토타입(docs/prototype/chatbot-tenant-dashboard.html)의
-- 10개 화면을 기준으로 채운다.

-- ============================================================
-- 1. 업체 담당자 로그인
-- ============================================================

-- 초대만 보내고 아직 수락 전(invite_state='PENDING')이면 비밀번호가 없다. 그래서 nullable 이다.
-- 비밀번호 없이 로그인이 통과하는 일이 없도록 서비스 레이어에서 null 을 명시적으로 거부한다.
ALTER TABLE tenant_members ADD COLUMN password_hash text;

-- ============================================================
-- 2. 챗봇 설정 (말투와 모양 · 설치)
-- ============================================================

-- 업체당 하나. tenant_id 가 곧 PK 다 — 1:N 이 될 일이 없는데 별도 id 를 두면
-- "설정이 두 벌 생긴" 상태가 표현 가능해지고, 그러면 언젠가 실제로 생긴다.
CREATE TABLE bot_settings (
  tenant_id            uuid PRIMARY KEY REFERENCES tenants(id) ON DELETE CASCADE,

  -- 모양
  bot_name             text NOT NULL,
  brand_color          text NOT NULL DEFAULT '#17222E',
  greeting             text NOT NULL,

  -- 말투. persona 는 시스템 프롬프트로 들어간다
  persona              text NOT NULL DEFAULT '',
  fallback_message     text NOT NULL DEFAULT '',
  -- 안내를 거절할 주제. 쉼표 구분 문자열이 아니라 목록이다 —
  -- 화면에서는 쉼표로 입력받지만 저장은 정규화한다.
  -- faqs.links 와 같은 jsonb 를 쓴다. 조회 조건으로 쓰지 않으므로 경로 인덱싱이 필요 없다
  forbidden_topics     jsonb NOT NULL DEFAULT '[]'::jsonb,

  -- 답을 못 찾았을 때
  lead_capture_enabled boolean NOT NULL DEFAULT true,
  support_phone        text,
  agent_handoff_enabled boolean NOT NULL DEFAULT false,
  agent_hours          text,

  -- 설치 (어디에 띄울까요)
  widget_position      text NOT NULL DEFAULT 'BOTTOM_RIGHT'
                       CHECK (widget_position IN ('BOTTOM_RIGHT', 'BOTTOM_LEFT')),
  page_scope           text NOT NULL DEFAULT 'ALL'
                       CHECK (page_scope IN ('ALL', 'INCLUDE', 'EXCLUDE')),
  page_patterns        jsonb NOT NULL DEFAULT '[]'::jsonb,
  -- 0 이면 자동으로 말 걸지 않는다
  nudge_delay_seconds  integer NOT NULL DEFAULT 15 CHECK (nudge_delay_seconds >= 0),

  updated_at           timestamptz NOT NULL DEFAULT now()
);

-- ============================================================
-- 3. 답변 개선 (answer gaps)
-- ============================================================

-- "챗봇이 답하지 못한 질문"을 매번 messages 에서 집계하면, 같은 질문을 표현만 바꿔 물었을 때
-- 따로 세어진다. 질문을 정규화한 키로 묶어 누적하고, 업체가 답을 등록하면 FAQ 로 승격한다.
--
-- 이 테이블은 답변 파이프라인(위젯 ask)이 upsert 한다. 그 파이프라인은 아직 없으므로
-- 지금은 로컬 데모 시드만 채운다.
CREATE TABLE answer_gaps (
  id               bigserial PRIMARY KEY,
  tenant_id        uuid NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  question         text NOT NULL,          -- 마지막으로 들어온 원문
  -- 공백·문장부호를 걷어낸 소문자 키. 같은 질문의 표현 차이를 하나로 묶는다
  question_norm    text NOT NULL,
  reason           text NOT NULL CHECK (reason IN ('ANSWER_FAILED', 'THUMBS_DOWN')),
  occurrence_count integer NOT NULL DEFAULT 1,
  last_asked_at    timestamptz NOT NULL DEFAULT now(),
  last_path        text,
  bot_answer       text,                   -- 그때 챗봇이 실제로 한 말. 업체가 판단 근거로 본다
  status           text NOT NULL DEFAULT 'OPEN'
                   CHECK (status IN ('OPEN', 'RESOLVED', 'DISMISSED')),
  -- 답을 등록해 FAQ 로 승격된 경우 그 FAQ. FAQ 가 지워지면 이력만 남기고 끊는다
  resolved_faq_id  uuid REFERENCES faqs(id) ON DELETE SET NULL,
  created_at       timestamptz NOT NULL DEFAULT now(),
  UNIQUE (tenant_id, question_norm)
);

-- 목록은 항상 "미해결을 최근 순으로"다. status 를 앞에 둔다
CREATE INDEX ON answer_gaps (tenant_id, status, last_asked_at DESC);
