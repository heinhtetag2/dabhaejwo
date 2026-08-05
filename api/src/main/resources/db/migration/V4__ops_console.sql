-- 운영 콘솔(/api/ops) 10개 화면을 붙이면서 드러난 구멍을 채운다.
--
-- V1 은 운영 콘솔 기준으로 짜여 있었지만 정작 "운영자가 로그인한다"는 흐름이 빠져 있었다.
-- operators 테이블에 비밀번호를 둘 자리가 없어 아무도 콘솔에 들어갈 수 없다.

-- ============================================================
-- 1. 운영자 로그인
-- ============================================================

-- 기획서 §8 은 SSO + 2FA 를 요구한다. 그건 운영자 3명을 넘길 때 붙이기로 하고
-- (CLAUDE.md Stub 목록), 지금은 로컬 계정 + BCrypt 로 연다.
--
-- nullable 인 이유는 SSO 로 전환한 계정에는 비밀번호가 없어야 하기 때문이다.
-- 비밀번호 없이 로그인이 통과하는 일이 없도록 서비스 레이어에서 null 을 명시적으로 거부한다.
ALTER TABLE operators ADD COLUMN password_hash text;

-- ============================================================
-- 2. 감사 대상 행위 확장
-- ============================================================

-- V1 의 CHECK 는 업체 조치(대리 로그인·요금제·쿼터·정지·해지)만 담고 있다.
-- 운영자의 쓰기 액션은 전부 남긴다는 원칙(core security-rules)에 따라
-- 일시정지 해제·요금제 정의 수정·기능 공개 전환·문의 상태 변경을 추가한다.
--
-- "나중에 추가"는 반드시 누락된다. 화면을 만드는 지금 같이 넣는다.
ALTER TABLE audit_logs DROP CONSTRAINT audit_logs_action_check;
ALTER TABLE audit_logs ADD CONSTRAINT audit_logs_action_check CHECK (action IN (
  'IMPERSONATE', 'VIEW_CONVERSATIONS', 'CHANGE_PLAN',
  'GRANT_QUOTA', 'SUSPEND', 'CHURN', 'EXTEND_TRIAL',
  'MODEL_PRICE_WRITE', 'COST_GUARD_WRITE',
  'ACTIVATE', 'PLAN_WRITE', 'FLAG_WRITE', 'TICKET_WRITE'));

-- ============================================================
-- 3. 감사 기록 전체 목록 조회
-- ============================================================

-- V1 의 인덱스는 (tenant_id, created_at) 과 (operator_id, created_at) 뿐이라
-- 필터 없이 "최근 30일 전체"를 보는 감사 기록 화면의 기본 조회가 풀스캔이 된다.
CREATE INDEX ON audit_logs (created_at DESC);

-- ============================================================
-- 3. 일 집계 배치가 읽는 경로
-- ============================================================

-- 배치는 "특정 날짜에 발생한 ai_usage 전체"를 테넌트별로 묶는다. V1 의
-- (created_at, purpose) 인덱스는 purpose 가 선두가 아니라 이 질의에도 쓰이지만,
-- 집계 대상이 날짜 하나로 좁혀지므로 별도 인덱스를 추가하지 않는다.
--
-- 대화 수 집계는 conversations (tenant_id, started_at DESC) 를 그대로 쓴다.
-- 저장 답변 수는 messages 를 날짜로 훑어야 하는데 V1 에 그 인덱스가 없다.
CREATE INDEX ON messages (created_at) WHERE saved = true;
