package com.dabhaejwo.domain.faq.controller;

import com.dabhaejwo.domain.faq.dto.request.FaqOrderRequest;
import com.dabhaejwo.domain.faq.dto.request.FaqSaveRequest;
import com.dabhaejwo.domain.faq.dto.response.FaqResponse;
import com.dabhaejwo.domain.faq.service.FaqService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/app/faqs")
public class FaqController {

    private final FaqService faqService;

    public FaqController(FaqService faqService) {
        this.faqService = faqService;
    }

    @GetMapping
    public List<FaqResponse> list() {
        return faqService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FaqResponse create(@Valid @RequestBody FaqSaveRequest request) {
        return faqService.create(request);
    }

    /** {@code /order} 가 {@code /{id}} 보다 먼저 선언돼야 uuid 로 오인되지 않는다. */
    @PatchMapping("/order")
    public List<FaqResponse> reorder(@Valid @RequestBody FaqOrderRequest request) {
        return faqService.reorder(request);
    }

    @PatchMapping("/{id}")
    public FaqResponse update(@PathVariable UUID id, @Valid @RequestBody FaqSaveRequest request) {
        return faqService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        faqService.delete(id);
    }
}
