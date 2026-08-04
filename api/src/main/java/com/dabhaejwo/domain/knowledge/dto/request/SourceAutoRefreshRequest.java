package com.dabhaejwo.domain.knowledge.dto.request;

/** boolean 은 {@code is} 접두사를 쓰지 않는다 (api-contract-rules). */
public record SourceAutoRefreshRequest(boolean autoRefresh) {
}
