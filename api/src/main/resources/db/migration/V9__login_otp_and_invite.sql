-- 로그인 2단계 인증(OTP), 초대 링크, 비밀번호 재설정.
--
-- 세 기능이 한 마이그레이션에 있는 이유는 같은 것을 공유하기 때문이다 —
-- 전부 "메일로 보낸 비밀값을 한 번만 쓰게 한다"는 하나의 구조다.

-- ── 로그인 챌린지 (OTP) ─────────────────────────────────────────────
--
-- 비밀번호가 맞았지만 아직 토큰을 주지 않은 상태. 여기서 메일로 보낸 코드를 맞혀야 끝난다.
--
-- **코드를 평문으로 두지 않는다.** DB 를 읽을 수 있는 사람이 남의 로그인을 완성할 수 있으면
-- 2단계 인증이 있으나 마나다. 비밀번호와 같은 방식(BCrypt)으로 해시해 둔다.
CREATE TABLE login_challenges (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  -- 'APP'(업체 담당자) | 'OPS'(운영자). 두 주체의 인증 경로는 분리돼 있으므로
  -- 챌린지도 어느 쪽인지 못 박는다 — 업체 챌린지로 운영자 토큰을 받아낼 수 없다.
  scope        text NOT NULL CHECK (scope IN ('APP', 'OPS')),
  subject_id   uuid NOT NULL,
  email        text NOT NULL,
  code_hash    text NOT NULL,
  attempts     int  NOT NULL DEFAULT 0,
  expires_at   timestamptz NOT NULL,
  consumed_at  timestamptz,
  -- 남용 추적용. 원본 IP 는 저장하지 않는다.
  requester_ip_hash text,
  created_at   timestamptz NOT NULL DEFAULT now()
);

-- 만료 정리와 재발송 횟수 계산이 쓴다.
CREATE INDEX ON login_challenges (subject_id, created_at DESC);
CREATE INDEX ON login_challenges (expires_at);

-- ── 팀원 초대 ───────────────────────────────────────────────────────
--
-- 전화번호는 초대 화면에서 함께 받는다(연락 수단이 메일 하나뿐이면 계정이 막혔을 때 길이 없다).
ALTER TABLE tenant_members ADD COLUMN phone text;

-- 초대 링크의 토큰. **원문을 저장하지 않는다** — 메일에만 실리고 DB 에는 해시만 남는다.
-- 유출된 DB 로 남의 계정을 만들 수 있으면 초대 링크는 그냥 백도어다.
ALTER TABLE tenant_members ADD COLUMN invite_token_hash text;
ALTER TABLE tenant_members ADD COLUMN invite_expires_at timestamptz;

-- 임시 비밀번호로 들어온 상태. true 면 비밀번호를 바꾸기 전에는 아무것도 못 한다.
ALTER TABLE tenant_members ADD COLUMN must_change_password boolean NOT NULL DEFAULT false;
ALTER TABLE tenant_members ADD COLUMN password_expires_at timestamptz;

-- 토큰으로 찾는다. 부분 인덱스라 초대 중인 행만 담긴다.
CREATE UNIQUE INDEX ON tenant_members (invite_token_hash) WHERE invite_token_hash IS NOT NULL;

-- ── 운영자 비밀번호 재설정 ──────────────────────────────────────────
--
-- 운영자는 초대 흐름이 없다(시더로만 만든다). 임시 비밀번호만 필요하다.
ALTER TABLE operators ADD COLUMN must_change_password boolean NOT NULL DEFAULT false;
ALTER TABLE operators ADD COLUMN password_expires_at timestamptz;
