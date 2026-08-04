package com.dabhaejwo.domain.plan.controller;

import com.dabhaejwo.domain.plan.dto.response.PublicPlanResponse;
import com.dabhaejwo.domain.plan.entity.Plan;
import com.dabhaejwo.domain.plan.repository.PlanRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 공개 요금제. 소개 페이지가 가격을 하드코딩하지 않고 여기서 읽는다 —
 * 운영자가 요금제를 바꿨는데 소개 페이지가 옛 가격을 보여주면 그 차이가 그대로 분쟁이 된다
 * (docs/plan/tenant-public-plan.md §2.2).
 */
@RestController
@RequestMapping("/api/public")
public class PublicPlanController {

    private final PlanRepository planRepository;

    public PublicPlanController(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    /** 판매 중인 것만. {@code sellable = false} 는 판매 중단된 구 요금제라 노출하지 않는다. */
    @GetMapping("/plans")
    public List<PublicPlanResponse> plans() {
        return planRepository.findAllByOrderBySortOrderAsc().stream()
                .filter(Plan::isSellable)
                .map(PublicPlanResponse::from)
                .toList();
    }
}
