package com.dabhaejwo.domain.billing.controller;

import com.dabhaejwo.domain.billing.dto.request.BillingAuthRequest;
import com.dabhaejwo.domain.billing.dto.response.BillingMethodResponse;
import com.dabhaejwo.domain.billing.service.BillingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 업체의 결제수단.
 *
 * <p>카드 정보는 이 서버를 지나지 않는다 — 결제창이 토스와 직접 주고받고,
 * 우리는 인증키를 빌링키로 바꾸는 일만 한다.
 *
 * <p>전부 <b>소유자 전용</b>이다(결제는 tenant-plan.md §8 에서 OWNER 만).
 * 서비스에서 강제하므로 여기서는 검사하지 않는다.
 */
@RestController
@RequestMapping("/api/app/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    /** 등록된 카드. 없으면 {@code registered: false} 다 — 404 가 아니다. */
    @GetMapping("/method")
    public BillingMethodResponse method() {
        return billingService.current();
    }

    /** 결제창이 돌려준 인증키를 빌링키로 바꿔 저장한다. */
    @PostMapping("/method")
    public BillingMethodResponse register(@Valid @RequestBody BillingAuthRequest request) {
        return billingService.register(request);
    }

    @DeleteMapping("/method")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove() {
        billingService.remove();
    }
}
