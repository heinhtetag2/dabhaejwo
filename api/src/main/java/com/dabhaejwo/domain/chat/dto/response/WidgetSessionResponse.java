package com.dabhaejwo.domain.chat.dto.response;

import com.dabhaejwo.domain.botsettings.entity.WidgetPosition;

import java.util.List;
import java.util.UUID;

/**
 * 위젯이 열릴 때 한 번에 받아가는 것 전부.
 *
 * <p>인사말·공통 질문·색을 따로 부르면 왕복이 셋이 되고, 그동안 방문자는 빈 패널을 본다.
 *
 * @param sessionId      이후 질문에 붙일 대화 식별자. 방문자 입력을 브라우저에 저장하지 않기 위해
 *                       서버가 발급한다 (widget-embed-script.md 보안 절)
 * @param widgetPosition 대시보드 API 와 <b>같은 이름·같은 값</b>이다. 같은 것을 두 이름으로
 *                       부르면 코드에서 계속 번역하게 된다 (api-contract-rules 단일 표현)
 */
public record WidgetSessionResponse(UUID sessionId,
                                    String botName,
                                    String greeting,
                                    String brandColor,
                                    WidgetPosition widgetPosition,
                                    boolean leadCaptureEnabled,
                                    List<WidgetFaq> faqs) {

    /** 버튼으로 보여줄 공통 질문. 답은 눌렀을 때 받는다 — 미리 다 내려주면 응답이 커진다. */
    public record WidgetFaq(UUID id, String question) {
    }
}
