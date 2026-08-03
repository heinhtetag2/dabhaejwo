package com.dabhaejwo.domain.guard.repository;

import com.dabhaejwo.domain.guard.entity.CostGuard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CostGuardRepository extends JpaRepository<CostGuard, Short> {

    default CostGuard current() {
        return findById(CostGuard.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "cost_guards 단일 행이 없습니다. V1__init.sql 시드가 적용되지 않았습니다."));
    }
}
