package com.dabhaejwo.domain.impersonation.repository;

import com.dabhaejwo.domain.impersonation.entity.ImpersonationSession;
import com.dabhaejwo.domain.impersonation.entity.ImpersonationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImpersonationSessionRepository extends JpaRepository<ImpersonationSession, UUID> {

    /** 업체에게 공개하는 접속 이력. 최근 것이 위다. */
    List<ImpersonationSession> findAllByTenantIdOrderByStartedAtDesc(UUID tenantId);

    Optional<ImpersonationSession> findByIdAndTenantId(UUID id, UUID tenantId);

    /** 해지 시 강제 종료할 대상. */
    List<ImpersonationSession> findAllByTenantIdAndStatus(UUID tenantId, ImpersonationStatus status);
}
