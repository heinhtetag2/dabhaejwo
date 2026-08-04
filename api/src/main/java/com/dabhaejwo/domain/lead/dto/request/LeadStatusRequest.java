package com.dabhaejwo.domain.lead.dto.request;

import com.dabhaejwo.domain.lead.entity.LeadStatus;
import jakarta.validation.constraints.NotNull;

public record LeadStatusRequest(@NotNull LeadStatus status) {
}
