package com.dabhaejwo.domain.billing.dto.request;

import com.dabhaejwo.domain.billing.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;

/** 답변 본문을 받지 않는다 — 회신은 이메일로 나가고 여기 사본을 두면 두 곳이 갈라진다. */
public record TicketStatusRequest(@NotNull TicketStatus status) {
}
