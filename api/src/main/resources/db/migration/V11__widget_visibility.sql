-- 위젯 노출 제어.
--
-- 지금까지 업체가 챗봇을 내리려면 자기 사이트에서 <script> 태그를 직접 빼는 수밖에 없었다.
-- 학습이 덜 된 채로 공개됐거나 답변이 이상할 때 급히 막을 수단이 콘솔에 없었다는 뜻이다.
--
-- 허용 주소를 지우는 우회로가 있긴 했지만 그건 끄는 게 아니다 — 방문자에게
-- "지금은 답변을 드리기 어렵습니다" 오류 말풍선이 뜬다. 고장 난 것처럼 보인다.

ALTER TABLE bot_settings
  ADD COLUMN widget_enabled boolean NOT NULL DEFAULT true;

-- 기본값이 true 인 이유 — 이 컬럼이 생겼다고 이미 잘 돌던 업체의 챗봇이 꺼지면 안 된다.
COMMENT ON COLUMN bot_settings.widget_enabled IS
  '위젯을 방문자에게 띄울지. false 면 위젯이 마운트 자체를 포기한다(오류를 그리지 않는다).';
