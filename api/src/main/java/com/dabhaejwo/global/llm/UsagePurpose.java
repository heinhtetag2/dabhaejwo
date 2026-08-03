package com.dabhaejwo.global.llm;

/**
 * 원가 발생 지점. 용도를 나누지 않으면 같은 모델이 답변용인지 요약용인지 구분되지 않아
 * 절감 지점을 찾을 수 없다 (admin-console-plan.md §4.4).
 *
 * <p>새 용도가 생기면 ETC 로 뭉뚱그리지 말고 값을 추가한다.
 */
public enum UsagePurpose {
    /** 대화 한 건마다. 원가의 대부분이며 입력 토큰이 지배적이다. */
    ANSWER,
    /** 문서 업로드·재크롤링 시 임베딩. 스파이크성. */
    EMBED_DOC,
    /** 대화 한 건당 1회. 건당 비용은 작지만 건수가 많다. */
    EMBED_QUERY,
    /** 제목 요약, 언어 감지. */
    ETC
}
