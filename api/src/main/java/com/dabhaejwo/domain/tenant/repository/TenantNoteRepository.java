package com.dabhaejwo.domain.tenant.repository;

import com.dabhaejwo.domain.tenant.entity.TenantNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * 조회와 적재만 있다. 수정·삭제 경로를 만들지 않는다 — 메모는 누적이다.
 */
public interface TenantNoteRepository extends JpaRepository<TenantNote, Long> {

    List<TenantNote> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
