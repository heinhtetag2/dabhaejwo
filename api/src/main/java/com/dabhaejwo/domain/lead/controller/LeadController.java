package com.dabhaejwo.domain.lead.controller;

import com.dabhaejwo.domain.lead.dto.request.LeadStatusRequest;
import com.dabhaejwo.domain.lead.dto.response.LeadResponse;
import com.dabhaejwo.domain.lead.service.LeadService;
import com.dabhaejwo.global.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/app/leads")
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @GetMapping
    public PageResponse<LeadResponse> list(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(required = false) Integer size) {
        return leadService.list(page, size);
    }

    @PatchMapping("/{id}")
    public LeadResponse changeStatus(@PathVariable UUID id,
                                     @Valid @RequestBody LeadStatusRequest request) {
        return leadService.changeStatus(id, request.status());
    }

    /**
     * CSV 내보내기 — 연락처 원문이 나가는 유일한 경로다.
     *
     * <p>BOM 을 붙인다. 없으면 엑셀이 UTF-8 을 못 알아보고 한글이 깨진다.
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv() {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = leadService.exportCsv().getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, payload, 0, bom.length);
        System.arraycopy(body, 0, payload, bom.length, body.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"leads.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(payload);
    }
}
