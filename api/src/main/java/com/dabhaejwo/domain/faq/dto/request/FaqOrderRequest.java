package com.dabhaejwo.domain.faq.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * 순서 변경. <b>전체 순서를 한 번에 보낸다.</b>
 *
 * <p>항목 하나의 새 위치만 보내면 나머지 항목의 sortOrder 와 충돌한다 —
 * 두 사람이 동시에 옮기면 같은 순번이 둘 생긴다.
 */
public record FaqOrderRequest(@NotEmpty List<UUID> faqIds) {
}
