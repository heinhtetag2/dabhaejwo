package com.dabhaejwo.domain.chat.dto.request;

import jakarta.validation.constraints.Size;

public record SessionStartRequest(@Size(max = 255) String path) {
}
