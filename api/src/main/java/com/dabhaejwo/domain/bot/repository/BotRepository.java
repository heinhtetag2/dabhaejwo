package com.dabhaejwo.domain.bot.repository;

import com.dabhaejwo.domain.bot.entity.Bot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BotRepository extends JpaRepository<Bot, UUID> {

    /**
     * 위젯 인증의 진입점. <b>이 프로젝트에서 가장 자주 도는 조회다</b> —
     * 위젯이 붙은 사이트의 모든 페이지뷰마다 한 번씩 온다.
     */
    Optional<Bot> findByPublishableKey(String publishableKey);

    /** 테넌트 격리 — id 만으로 조회하지 않는다. */
    Optional<Bot> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Bot> findAllByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    /**
     * 서비스를 지목하지 않는 옛 경로와 아직 서비스 개념이 없는 화면의 착지점.
     *
     * <p>M2 동안 대시보드는 전부 여기로 온다 — 업체마다 서비스가 하나뿐이라 관찰되는
     * 동작이 바뀌지 않는다.
     */
    Optional<Bot> findFirstByTenantIdAndIsDefaultTrue(UUID tenantId);

    /** purge 워커가 유예 끝난 것을 찾는다. */
    List<Bot> findAllByStatus(com.dabhaejwo.domain.bot.entity.BotStatus status);

    /** 요금제 상한 검사용. 삭제 유예 중인 것도 센다 — 지우자마자 새로 만들어 우회하지 못하게. */
    long countByTenantId(UUID tenantId);
}
