package com.dabhaejwo.domain.chat.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * 챗봇의 답 한 번.
 *
 * @param answered  근거를 찾아 실제로 답했는가. {@code false} 면 위젯이 연락처 폼을 제안하고,
 *                  그 질문은 업체 대시보드의 답변 개선 목록으로 올라간다
 * @param saved     저장 답변(공통 질문)으로 나갔는가. {@code true} 면 <b>모델을 거치지 않았고
 *                  {@code ai_usage} 에도 기록되지 않는다.</b> 위젯이 "저장된 답변" 태그를 표시한다
 * @param links     근거 문서 중 방문자가 열 수 있는 경로만. 업로드 파일은 들어가지 않는다
 * @param messageId 👍👎 를 붙일 대상. 미리보기는 기록을 남기지 않으므로 {@code null} 이다
 */
public record AnswerResponse(boolean answered,
                             boolean saved,
                             String answer,
                             List<String> links,
                             UUID messageId) {
}
