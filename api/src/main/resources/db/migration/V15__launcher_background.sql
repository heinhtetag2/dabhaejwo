-- 런처 배경 — 업로드한 로고 뒤에 무엇을 깔 것인가.
--
-- 지금까지 런처 배경은 브랜드 색 하나였다. 기본 말풍선 아이콘(흰 선)을 기준으로 만든
-- 값인데, 업체가 자기 로고를 올리면서 문제가 됐다 — PNG 의 투명한 부분으로 브랜드 색이
-- 그대로 올라온다. 투명은 흰색이 아니라 **아무것도 안 칠한 것**이기 때문이다.
--
-- 이게 업체마다 다르게 나쁘다는 것이 핵심이다. 흰 바탕 기준으로 만든 로고는 진한 브랜드
-- 색 위에서 뭉개지고, 밝은 브랜드 색에 흰 로고를 올리면 아예 안 보인다. 우리가 한쪽으로
-- 정하면 반대쪽 업체가 매번 깨진다.

ALTER TABLE bot_settings
  ADD COLUMN launcher_background text NOT NULL DEFAULT 'BRAND'
  CHECK (launcher_background IN ('BRAND', 'WHITE', 'NONE'));

-- 기본이 BRAND 인 이유 — 컬럼이 생겼다고 잘 돌던 업체의 런처 모양이 바뀌면 안 된다.
COMMENT ON COLUMN bot_settings.launcher_background IS
  'BRAND 브랜드 색 · WHITE 흰 바탕 · NONE 투명. 이미지가 없으면 서버가 BRAND 로 강제한다 — 기본 아이콘이 흰 선이라 흰 바탕·투명에서는 보이지 않는다.';
