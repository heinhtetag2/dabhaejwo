package com.dabhaejwo.domain.pricing.controller;

import com.dabhaejwo.domain.pricing.dto.request.ModelPriceCreateRequest;
import com.dabhaejwo.domain.pricing.dto.response.ModelPriceResponse;
import com.dabhaejwo.domain.pricing.service.ModelPriceService;
import com.dabhaejwo.global.security.Permission;
import com.dabhaejwo.global.security.RequirePermission;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 모델 단가. <b>POST 와 GET 뿐이다.</b>
 *
 * <p>PATCH·DELETE 가 없는 것이 설계다. 단가는 이력이며 행을 추가만 한다 — 기존 행을
 * 고치면 그 단가로 이미 계산된 과거 {@code ai_usage} 의 근거가 사라지고,
 * 언제부터 적자였는지 추적할 수 없게 된다.
 */
@RestController
@RequestMapping("/api/ops/model-prices")
public class ModelPriceOpsController {

    private final ModelPriceService service;

    public ModelPriceOpsController(ModelPriceService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePermission(Permission.MODEL_PRICE_READ)
    public List<ModelPriceResponse> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(Permission.MODEL_PRICE_WRITE)
    public ModelPriceResponse register(@Valid @RequestBody ModelPriceCreateRequest request) {
        return service.register(request);
    }
}
