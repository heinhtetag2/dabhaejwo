package com.dabhaejwo.domain.operator.repository;

import com.dabhaejwo.domain.operator.entity.Operator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OperatorRepository extends JpaRepository<Operator, java.util.UUID> {

    Optional<Operator> findByEmail(String email);

    boolean existsByEmail(String email);

    /** 비활성 계정도 함께 보여준다 — 숨기면 왜 로그인이 안 되는지 화면에서 알 수 없다. */
    java.util.List<Operator> findAllByOrderByActiveDescNameAsc();

    /**
     * 살아 있는 운영 관리자 수.
     *
     * <p>마지막 한 명을 비활성화하거나 강등하면 <b>아무도 콘솔을 관리할 수 없게 된다.</b>
     * 되돌리려면 DB 를 직접 만져야 한다 — 그 상태를 만들지 않기 위해 센다.
     */
    long countByRoleAndActiveTrue(com.dabhaejwo.global.security.OperatorRole role);
}
