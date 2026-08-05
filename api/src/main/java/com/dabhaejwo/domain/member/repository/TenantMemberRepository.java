package com.dabhaejwo.domain.member.repository;

import com.dabhaejwo.domain.member.entity.TenantMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    /** 초대 링크 조회. 토큰 원문이 아니라 해시로 찾는다 — 원문은 DB 에 없다. */
    java.util.Optional<TenantMember> findByInviteTokenHash(String inviteTokenHash);

    /** 테넌트 격리 — id 만으로 조회하지 않는다. */
    Optional<TenantMember> findByIdAndTenantId(UUID id, UUID tenantId);

    List<TenantMember> findAllByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);

    /**
     * 업체별 마지막 접속 시각. 운영 콘솔의 "7일 미접속" 필터와 상세의 "마지막 접속"이 쓴다.
     *
     * <p>한 번에 가져오는 이유는 업체 목록이 업체마다 담당자를 조회하면 N+1 이 되기 때문이다.
     * 아무도 접속한 적 없는 업체는 결과에 나오지 않는다 — 호출부가 "기록 없음"으로 다룬다.
     */
    @Query("""
            SELECT m.tenantId AS tenantId, MAX(m.lastSeenAt) AS lastSeenAt
            FROM TenantMember m
            WHERE m.lastSeenAt IS NOT NULL
            GROUP BY m.tenantId
            """)
    List<LastSeen> findLastSeenByTenant();

    interface LastSeen {
        UUID getTenantId();

        java.time.OffsetDateTime getLastSeenAt();
    }
}
