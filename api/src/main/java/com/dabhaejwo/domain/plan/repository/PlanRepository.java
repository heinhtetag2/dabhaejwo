package com.dabhaejwo.domain.plan.repository;

import com.dabhaejwo.domain.plan.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID> {

    /** 판매 중단된 구 요금제도 목록에 남긴다 — 사용 업체 수를 보여줘야 하기 때문이다. */
    List<Plan> findAllByOrderBySortOrderAsc();
}
