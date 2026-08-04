package com.dabhaejwo.domain.tenant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 스킴·경로를 붙여 보내도 서버가 호스트만 떼어 저장한다. */
public record AllowedOriginRequest(@NotBlank @Size(max = 255) String origin) {
}
