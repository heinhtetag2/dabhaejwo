package com.dabhaejwo.domain.gap.entity;

public enum GapReason {
    /** 챗봇이 답을 찾지 못했다. */
    ANSWER_FAILED,
    /** 답은 했으나 방문자가 👎 를 눌렀다. */
    THUMBS_DOWN
}
