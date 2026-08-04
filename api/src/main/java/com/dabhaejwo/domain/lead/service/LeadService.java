package com.dabhaejwo.domain.lead.service;

import com.dabhaejwo.domain.lead.dto.response.LeadResponse;
import com.dabhaejwo.domain.lead.entity.Lead;
import com.dabhaejwo.domain.lead.entity.LeadStatus;
import com.dabhaejwo.domain.lead.repository.LeadRepository;
import com.dabhaejwo.global.common.PageResponse;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.AuthPrincipal;
import com.dabhaejwo.global.security.CurrentAuth;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 남긴 연락처.
 *
 * <p>업체 입장에서 이 화면이 곧 매출이다 — 챗봇의 값어치를 숫자로 보여주는 유일한 곳이다
 * (tenant-plan.md §4.4).
 *
 * <p>연락처 원문은 목록에 실리지 않는다. CSV 내보내기에서만 나가며, 그것도 편집 권한이 있어야 한다.
 */
@Service
public class LeadService {

    private final LeadRepository leadRepository;

    public LeadService(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<LeadResponse> list(int page, Integer size) {
        UUID tenantId = CurrentAuth.tenantUser().tenantId();
        Pageable pageable = PageRequest.of(Math.max(page, 0), PageResponse.clampSize(size));
        return PageResponse.of(
                leadRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId, pageable),
                LeadResponse::from);
    }

    @Transactional
    public LeadResponse changeStatus(UUID leadId, LeadStatus status) {
        AuthPrincipal.TenantUser user = CurrentAuth.requireEditor();
        Lead lead = leadRepository.findByIdAndTenantId(leadId, user.tenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LEAD_NOT_FOUND));
        lead.changeStatus(status);
        return LeadResponse.from(lead);
    }

    /**
     * CSV 내보내기. <b>여기서만 연락처 원문이 나간다.</b>
     *
     * <p>편집 권한을 요구한다 — 보기 전용 담당자에게 고객 연락처 원문을 통째로 주지 않는다.
     * 대리 접속 세션은 VIEWER 로 발급되므로 운영자도 내려받을 수 없다.
     */
    @Transactional(readOnly = true)
    public String exportCsv() {
        AuthPrincipal.TenantUser user = CurrentAuth.requireEditor();
        List<Lead> leads = leadRepository.findAllByTenantIdOrderByCreatedAtDesc(user.tenantId());

        StringBuilder csv = new StringBuilder("이름,연락처,남긴 이유,상태,시각\n");
        for (Lead lead : leads) {
            csv.append(escape(lead.getName())).append(',')
                    .append(escape(lead.getContactRaw())).append(',')
                    .append(escape(lead.getReason())).append(',')
                    .append(lead.getStatus()).append(',')
                    .append(lead.getCreatedAt()).append('\n');
        }
        return csv.toString();
    }

    /**
     * 쉼표·따옴표·줄바꿈이 든 값을 감싼다.
     *
     * <p>{@code =} 로 시작하는 값 앞에 작은따옴표를 붙인다 — 엑셀이 수식으로 해석해
     * 실행하는 CSV 인젝션을 막는다.
     */
    private String escape(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw;
        if (value.startsWith("=") || value.startsWith("+") || value.startsWith("-")
                || value.startsWith("@")) {
            value = "'" + value;
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
