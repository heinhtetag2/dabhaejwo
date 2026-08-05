package com.dabhaejwo.domain.conversation.repository;

import com.dabhaejwo.domain.conversation.entity.MessageFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MessageFeedbackRepository extends JpaRepository<MessageFeedback, UUID> {

    long countByTenantIdAndHelpfulFalse(UUID tenantId);
}
