-- 설정이 없는 서비스에 한 벌씩 만들어 준다.
--
-- 왜 빠졌나: V17 이 bot_settings 의 PK 를 bot_id 로 바꿨는데 **엔티티는 tenant_id 를
-- @Id 로 들고 있었다.** 그래서 두 번째 서비스의 설정을 저장할 때 JPA 가 같은 키로 보고
-- 첫 서비스의 행을 덮어썼다 — 업체가 두 화면을 번갈아 열 때마다 행 하나가 서로를 밀어내며
-- 매번 기본값으로 되돌아갔고, 결과적으로 어느 한쪽은 행이 아예 없는 상태가 됐다.
--
-- `ddl-auto: validate` 는 **PK 를 검사하지 않는다.** 그래서 기동도, 컴파일도, 단위 테스트도
-- 이 어긋남을 잡지 못했다. 서비스가 둘인 업체가 실제로 생겨야 드러난다.
--
-- 이 마이그레이션은 데이터를 **채우기만 한다** — 이미 있는 행은 건드리지 않는다.
-- 덮어쓰면 업체가 적어둔 말투를 두 번 잃는다.
--
-- 값은 BotSettings.defaults() 와 같아야 한다. 두 곳이 갈리면 "새 서비스의 기본값"이
-- 만들어진 경로에 따라 달라진다.
INSERT INTO bot_settings (bot_id, tenant_id, bot_name, brand_color, greeting, persona,
                          fallback_message, lead_capture_enabled, agent_handoff_enabled,
                          widget_position, page_scope, nudge_delay_seconds,
                          widget_enabled, launcher_size, launcher_background, updated_at)
SELECT b.id,
       b.tenant_id,
       b.name || ' 도우미',
       '#17222E',
       '안녕하세요! 무엇을 도와드릴까요?',
       '',
       '제가 확인하기 어려운 내용이네요. 상담원에게 연결해 드릴까요?',
       true,
       false,
       'BOTTOM_RIGHT',
       'ALL',
       15,
       true,
       'MEDIUM',
       'BRAND',
       now()
FROM bots b
WHERE NOT EXISTS (SELECT 1 FROM bot_settings s WHERE s.bot_id = b.id);

-- 이 뒤로는 "설정 없는 서비스"가 있으면 안 된다. 조용히 지나가면 화면이 빈 채로 뜨고
-- 업체는 자기가 안 적은 것으로 읽는다.
DO $$
DECLARE missing integer;
BEGIN
  SELECT count(*) INTO missing
  FROM bots b WHERE NOT EXISTS (SELECT 1 FROM bot_settings s WHERE s.bot_id = b.id);
  IF missing > 0 THEN
    RAISE EXCEPTION '설정이 없는 서비스가 %개 남았습니다', missing;
  END IF;
END $$;
