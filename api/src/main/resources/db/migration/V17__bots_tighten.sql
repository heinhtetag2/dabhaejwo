-- 서비스 — 스키마를 조인다.
--
-- V16 은 일부러 아무것도 조이지 않았다. 그래야 **코드를 하나도 안 바꾼 앱이 그대로 뜨고**,
-- 마이그레이션 실패와 코드 실패를 분리해 판정할 수 있기 때문이다.
--
-- 이제 앱이 `bot_id` 를 진실로 다루므로 조인다. 순서가 뒤집히면 안 된다 —
-- 스키마를 먼저 조였다면 구버전으로 롤백할 길이 사라진다.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '10min';

-- ============================================================
-- 1. 뒤늦게 들어온 행 마저 채우기
-- ============================================================
--
-- V16 적용 후 이 마이그레이션 사이에 구버전 앱이 만든 행이 있을 수 있다.
-- NOT NULL 을 걸기 전에 한 번 더 훑는다.

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
    EXECUTE format(
      'UPDATE %I c SET bot_id = b.id FROM bots b'
      ' WHERE c.bot_id IS NULL AND b.tenant_id = c.tenant_id AND b.is_default', t);
  END LOOP;
END $$;

-- ============================================================
-- 2. NOT NULL
-- ============================================================
--
-- `jobs` 는 빼둔다 — 이 테이블에 쓰는 코드가 아직 없고(작업 큐는 미구현),
-- 비어 있는 테이블에 제약을 걸어봐야 지켜지는 것이 없다.
-- `ai_usage` 도 뺀다 — 서비스 구분 이전 호출은 채울 근거가 없고,
-- 0 이나 임의값으로 메우면 원가 원장이 거짓이 된다.

DO $$
DECLARE
  t text;
BEGIN
  FOREACH t IN ARRAY ARRAY[
    'bot_settings', 'allowed_origins',
    'knowledge_sources', 'knowledge_documents', 'knowledge_chunks',
    'faqs', 'conversations', 'messages', 'message_feedback',
    'leads', 'answer_gaps'
  ]
  LOOP
    EXECUTE format('ALTER TABLE %I ALTER COLUMN bot_id SET NOT NULL', t);
  END LOOP;
END $$;

-- ============================================================
-- 3. bot_settings — PK 를 서비스로
-- ============================================================
--
-- V2 는 `tenant_id` 를 PK 로 두며 "1:N 이 될 일이 없다"고 적었다. 그 전제가 틀렸다.
--
-- 행 수가 곧 서비스 수(수십~수백)라 ACCESS EXCLUSIVE 락 시간이 밀리초 단위다.

DROP INDEX IF EXISTS bot_settings_bot_id_key;
ALTER TABLE bot_settings DROP CONSTRAINT bot_settings_pkey;
ALTER TABLE bot_settings ADD PRIMARY KEY (bot_id);

-- `tenant_id` 는 컬럼으로 남긴다 — 테넌트 격리 규칙을 지키고, 복합 FK 가
-- 두 값이 어긋난 행을 막는 근거이기도 하다.
COMMENT ON COLUMN bot_settings.tenant_id IS
  '격리용. 진실은 bot_id 다 — 조회는 전부 bot_id 로 한다.';

-- ============================================================
-- 4. 유일성을 서비스 범위로
-- ============================================================
--
-- 업체 범위로 두면 서비스가 둘일 때 서로를 막는다 — A 서비스에 등록한 주소를
-- B 서비스에 등록할 수 없고, A 에서 실패한 질문이 B 의 개선 목록과 충돌한다.

ALTER TABLE allowed_origins DROP CONSTRAINT IF EXISTS allowed_origins_tenant_id_origin_key;
ALTER TABLE allowed_origins ADD CONSTRAINT allowed_origins_bot_id_origin_key
  UNIQUE (bot_id, origin);

ALTER TABLE answer_gaps DROP CONSTRAINT IF EXISTS answer_gaps_tenant_id_question_norm_key;
ALTER TABLE answer_gaps ADD CONSTRAINT answer_gaps_bot_id_question_norm_key
  UNIQUE (bot_id, question_norm);

-- ============================================================
-- 5. 조회 인덱스를 서비스 범위로
-- ============================================================
--
-- 기존 (tenant_id, ...) 인덱스는 **지우지 않는다** — 운영 콘솔과 한도 판정이
-- 여전히 업체 범위로 조회한다.

CREATE INDEX IF NOT EXISTS knowledge_documents_bot_status_idx
  ON knowledge_documents (bot_id, status);
CREATE INDEX IF NOT EXISTS faqs_bot_sort_idx ON faqs (bot_id, sort_order);
CREATE INDEX IF NOT EXISTS conversations_bot_started_idx
  ON conversations (bot_id, started_at DESC);
CREATE INDEX IF NOT EXISTS leads_bot_status_idx ON leads (bot_id, status, created_at DESC);
-- 답변 개선 화면의 조회 조건. 부분 인덱스라 실패한 답변만 담는다.
CREATE INDEX IF NOT EXISTS messages_bot_unanswered_idx
  ON messages (bot_id, answered, created_at DESC) WHERE answered = false;

-- ============================================================
-- 6. 어서션
-- ============================================================

DO $$
DECLARE
  n bigint;
BEGIN
  -- 서비스마다 설정이 정확히 하나여야 한다. 없으면 위젯이 기본값으로 답한다.
  SELECT count(*) INTO n
  FROM bots b WHERE NOT EXISTS (SELECT 1 FROM bot_settings s WHERE s.bot_id = b.id);
  IF n > 0 THEN
    RAISE EXCEPTION '설정 없는 서비스 %건', n;
  END IF;

  -- 서비스마다 허용 주소가 하나 이상. 없으면 위젯이 어디에서도 뜨지 않는다.
  SELECT count(*) INTO n
  FROM bots b WHERE NOT EXISTS (SELECT 1 FROM allowed_origins o WHERE o.bot_id = b.id);
  IF n > 0 THEN
    RAISE EXCEPTION '허용 주소 없는 서비스 %건', n;
  END IF;
END $$;
