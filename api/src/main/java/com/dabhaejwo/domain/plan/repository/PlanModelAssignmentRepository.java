package com.dabhaejwo.domain.plan.repository;

import com.dabhaejwo.domain.plan.entity.PlanModelAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlanModelAssignmentRepository extends JpaRepository<PlanModelAssignment, UUID> {
}
