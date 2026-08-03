package com.dabhaejwo.global.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * 페이지네이션 응답. api-contracts.md §0-2 형태와 정확히 일치한다.
 * Spring 의 {@code Page} 를 그대로 직렬화하지 않는 이유는 그 JSON 형태가 버전에 따라 바뀌기 때문이다.
 */
public record PageResponse<T>(List<T> content, PageInfo page) {

    /** 리스트 조회 size 상한. 무제한 조회를 허용하지 않는다. */
    public static final int MAX_SIZE = 100;
    public static final int DEFAULT_SIZE = 20;

    public record PageInfo(int number, int size, long totalElements, int totalPages) {
    }

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                new PageInfo(page.getNumber(), page.getSize(),
                        page.getTotalElements(), page.getTotalPages()));
    }

    public static int clampSize(Integer requested) {
        if (requested == null || requested <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(requested, MAX_SIZE);
    }
}
