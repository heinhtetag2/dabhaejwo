package com.dabhaejwo.domain.flag.repository;

import com.dabhaejwo.domain.flag.entity.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, String> {

    List<FeatureFlag> findAllByOrderByNameAsc();
}
