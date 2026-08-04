package com.dabhaejwo.domain.member.repository;

import com.dabhaejwo.domain.member.entity.TenantMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantMemberRepository extends JpaRepository<TenantMember, UUID> {

    /**
     * 로그인용. 이메일은 업체 안에서만 유일하므로(UNIQUE (tenant_id, email)) 전역으로는
     * 중복될 수 있다. 지금은 한 이메일이 여러 업체에 속하는 경우를 지원하지 않는다 —
     * 그렇게 되면 로그인 시 어느 업체인지 물어야 한다. docs/IMPROVEMENTS.md 참조.
     */
    List<TenantMember> findAllByEmail(String email);

    /** 테넌트 격리 — id 만으로 조회하지 않는다. */
    Optional<TenantMember> findByIdAndTenantId(UUID id, UUID tenantId);

    List<TenantMember> findAllByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);
}
