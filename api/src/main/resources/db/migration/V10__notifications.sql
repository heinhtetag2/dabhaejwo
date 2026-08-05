-- 알림.
--
-- WebSocket 은 <b>전달 채널일 뿐이고 저장은 여기다.</b> 접속 중이 아닐 때 발생한 알림이
-- 사라지면 "알림이 왔었는데 없어졌다"가 된다 — 한도 초과나 대리 접속처럼 놓치면 안 되는
-- 것들이라 더 그렇다. 목록·안읽음 수·읽음 처리는 REST 로 이 테이블을 읽는다.

CREATE TABLE notifications (
  id          bigserial PRIMARY KEY,

  -- 수신자. 둘 중 하나만 채워진다.
  --   tenant_id  → 그 업체의 담당자 전원이 본다
  --   operator_id→ 특정 운영자 (지금은 안 쓰지만 "당신에게 배정됨" 류를 위해 남긴다)
  --   둘 다 NULL + audience='OPS' → 운영자 전체(역할 필터로 좁힌다)
  audience    text NOT NULL CHECK (audience IN ('OPS', 'TENANT')),
  tenant_id   uuid REFERENCES tenants(id) ON DELETE CASCADE,
  operator_id uuid REFERENCES operators(id),

  -- 운영자용 알림은 역할로 대상을 좁힌다. CS 에게 단가 알림, 개발에게 결제 알림은 소음이다.
  -- 비어 있으면 전 역할이 본다.
  target_roles text[] NOT NULL DEFAULT '{}',

  type        text NOT NULL,
  title       text NOT NULL,
  body        text,
  -- 누르면 바로 그 대상으로 간다. 이름만 확인하고 다시 찾게 만들지 않는다
  -- (admin-console-plan.md §4.1 조치 목록과 같은 원칙).
  target_path text,

  read_at     timestamptz,
  created_at  timestamptz NOT NULL DEFAULT now(),

  -- 중복 억제 키. 원가 상한 도달은 요청마다 발생하므로 이게 없으면 같은 문장으로 도배된다.
  -- 예) 'COST_CAP_80:2026-08-05', 'QUOTA_80:{tenantId}:2026-08'
  dedupe_key  text
);

-- 목록 조회: 수신자별 최신순. 안읽음 배지도 같은 인덱스를 탄다.
CREATE INDEX ON notifications (audience, tenant_id, created_at DESC);
CREATE INDEX ON notifications (audience, created_at DESC);
-- 안읽음만 세는 질의가 잦다. 읽은 것은 인덱스에서 빼 크기를 줄인다.
CREATE INDEX ON notifications (tenant_id, read_at) WHERE read_at IS NULL;

-- 같은 dedupe_key 는 한 번만. 부분 인덱스라 dedupe_key 가 없는 알림(가입·리드처럼
-- 매번 새로운 사건)은 제한을 받지 않는다.
CREATE UNIQUE INDEX notifications_dedupe_idx ON notifications (dedupe_key)
  WHERE dedupe_key IS NOT NULL;
