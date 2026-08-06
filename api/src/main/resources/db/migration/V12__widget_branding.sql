-- 위젯 브랜딩 — 로고·런처 아이콘·크기.
--
-- 지금까지 위젯 런처는 52px 고정에 `💬` 이모지였다. 이모지는 운영체제가 그리므로
-- 윈도우·맥·안드로이드에서 서로 다른 그림이 나온다 — 업체 사이트에 얹히는 요소가
-- 우리가 통제하지 못하는 모양인 셈이다.

-- avatar_url 은 V3 에서 만들어졌지만 **어디서도 읽히지 않았다.** 새 컬럼을 만들면
-- 죽은 컬럼이 하나 남으므로, 이번에 제 이름을 찾아준다.
ALTER TABLE bot_settings RENAME COLUMN avatar_url TO launcher_icon_url;

COMMENT ON COLUMN bot_settings.launcher_icon_url IS
  '런처(동그란 버블)에 넣을 이미지. 없으면 로고, 그것도 없으면 기본 말풍선 아이콘.';

-- 업체 로고. 콘솔 사이드바와 위젯 런처 양쪽에 쓴다.
ALTER TABLE bot_settings ADD COLUMN logo_url text;

COMMENT ON COLUMN bot_settings.logo_url IS
  '업체 로고. 콘솔 사이드바에 뜨고, 런처 아이콘이 없으면 런처에도 쓰인다.';

-- 런처 크기. 픽셀을 직접 받지 않는 이유 — 업체가 감으로 정하게 되고,
-- 남의 사이트를 가릴 만큼 큰 값이나 눌리지 않을 만큼 작은 값이 나올 수 있다.
ALTER TABLE bot_settings
  ADD COLUMN launcher_size text NOT NULL DEFAULT 'MEDIUM'
  CHECK (launcher_size IN ('SMALL', 'MEDIUM', 'LARGE'));

COMMENT ON COLUMN bot_settings.launcher_size IS
  'SMALL 48px · MEDIUM 56px · LARGE 64px. 실제 픽셀은 위젯 CSS 가 정한다.';
