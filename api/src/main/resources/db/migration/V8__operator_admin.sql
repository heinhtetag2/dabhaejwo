-- 운영자 계정 관리(운영 콘솔 → 관리자).
--
-- 스키마 변경은 없다. operators 테이블은 V1 부터 필요한 것을 다 갖고 있고
-- (role · active · password_hash · last_seen_at), 관리 화면이 없었을 뿐이다.
-- 여기서 하는 일은 그 조작을 감사 대상으로 등록하는 것뿐이다.

-- ============================================================
-- 감사 대상 행위 확장
-- ============================================================

-- 운영자 계정 조작은 <b>권한 자체를 바꾸는 행위</b>다. 누가 누구에게 무슨 역할을 줬는지
-- 남지 않으면, 사고가 났을 때 "언제부터 이 사람이 감사 기록을 볼 수 있었나"에 답할 수 없다.
--
-- 계정 삭제 액션이 없는 것도 의도다. operators 는 audit_logs·quota_overrides·tenant_notes·
-- impersonation_sessions·tickets·provider_credentials 가 FK 로 참조한다. 지우면 감사 기록의
-- 행위자가 사라지는데, 그 기록은 수정·삭제 불가에 3년 보존이다. 그래서 비활성화만 한다.
ALTER TABLE audit_logs DROP CONSTRAINT audit_logs_action_check;
ALTER TABLE audit_logs ADD CONSTRAINT audit_logs_action_check CHECK (action IN (
  'IMPERSONATE', 'VIEW_CONVERSATIONS', 'CHANGE_PLAN',
  'GRANT_QUOTA', 'SUSPEND', 'CHURN', 'EXTEND_TRIAL',
  'MODEL_PRICE_WRITE', 'COST_GUARD_WRITE',
  'ACTIVATE', 'PLAN_WRITE', 'FLAG_WRITE', 'TICKET_WRITE',
  'PROVIDER_CREDENTIAL_WRITE',
  'OPERATOR_WRITE'));

-- 목록 조회는 이름순이고 비활성 계정도 함께 보여준다(숨기면 왜 로그인이 안 되는지 알 수 없다).
CREATE INDEX ON operators (active, name);
