package com.dabhaejwo.domain.chat.dto.response;

import java.util.UUID;

/**
 * 버튼으로 보여줄 공통 질문 한 줄.
 *
 * <p>답은 담지 않는다 — 눌렀을 때 받는다. 미리 다 내려주면 응답이 커지고, 방문자가 열어보지도
 * 않은 답변까지 전부 실어 보내게 된다.
 *
 * <p>대화 시작 목록({@link WidgetSessionResponse})과 답변 뒤 후속 질문
 * ({@link AnswerResponse})이 <b>같은 모양</b>을 쓴다. 두 벌로 나누면 위젯이 같은 칩을
 * 두 가지로 그리게 되고, 언젠가 한쪽에만 필드가 붙는다.
 */
public record WidgetFaq(UUID id, String question) {
}
