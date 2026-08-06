package com.dabhaejwo.domain.chat.dto.response;

import com.dabhaejwo.domain.botsettings.entity.LauncherBackground;
import com.dabhaejwo.domain.botsettings.entity.WidgetPosition;

/**
 * 위젯이 <b>뜨기 전에</b> 묻는 것. api-contracts.md §10.
 *
 * <p>세션 시작({@code POST /session})과 나눈 이유 — 그쪽은 <b>대화를 만든다.</b>
 * 버블을 띄울지 판단하려고 매 페이지뷰마다 대화를 만들면 "열어보지도 않은 방문"이
 * 전부 통계에 잡히고 월 대화 한도까지 깎는다.
 *
 * <p>그래서 이 응답에는 인사말·공통 질문이 없다. 그것들은 방문자가 패널을 열었을 때 온다.
 *
 * @param enabled        띄울지 말지의 <b>최종 결론</b>. 업체가 껐거나, 이 경로가 노출 범위
 *                       밖이면 false 다. 위젯은 이유를 알 필요가 없고 알아서도 안 된다 —
 *                       어떤 페이지를 감추고 싶어 하는지가 남의 사이트 소스에 드러난다
 * @param brandColor     버블·버튼 색({@code #RRGGBB}). <b>여기서 줘야 한다</b> —
 *                       버블은 패널을 열기 전에 그려지므로 세션 응답으로는 늦다.
 *                       저장할 때 형식을 막아 두었다({@code BotSettingsSaveRequest})
 * @param launcherImageUrl 런처에 넣을 이미지. 없으면 위젯이 기본 말풍선 아이콘을 그린다.
 *                        아이콘 > 로고 순으로 서버가 이미 골라 준 값이다 —
 *                        위젯이 두 필드를 받아 스스로 고르면 미리보기와 어긋난다
 * @param launcherSizePx  런처 지름(px). 업체는 3단계 중에서 고르고 픽셀은 서버가 정한다
 * @param launcherBackground 올린 이미지 뒤에 깔 것. <b>이미지가 없으면 늘 {@code BRAND} 다</b> —
 *                        기본 아이콘이 흰 선이라 흰 바탕·투명 위에서는 보이지 않는다
 * @param nudgeDelayMs   자동으로 말 거는 시점. 0 이면 말 걸지 않는다
 */
public record WidgetConfigResponse(
        boolean enabled,
        WidgetPosition widgetPosition,
        String brandColor,
        String launcherImageUrl,
        int launcherSizePx,
        LauncherBackground launcherBackground,
        int nudgeDelayMs) {
}
