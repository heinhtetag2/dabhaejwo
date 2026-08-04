package com.dabhaejwo.domain.faq.service;

import com.dabhaejwo.domain.faq.dto.request.FaqOrderRequest;
import com.dabhaejwo.domain.faq.dto.request.FaqSaveRequest;
import com.dabhaejwo.domain.faq.dto.response.FaqResponse;
import com.dabhaejwo.domain.faq.entity.Faq;
import com.dabhaejwo.domain.faq.repository.FaqRepository;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.AuthPrincipal;
import com.dabhaejwo.global.security.CurrentAuth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 공통 질문 = 저장 답변.
 *
 * <p>여기 등록된 답변으로 나가는 응답은 <b>모델을 거치지 않는다.</b> 그래서 원가가 0이고
 * 대화 사용량에도 잡히지 않는다. 업체가 이걸 채울수록 서로에게 이득이다.
 *
 * <p>조회는 전 역할이 가능하고 쓰기는 OWNER·EDITOR 만이다 (tenant-plan.md §8).
 * 대리 접속 토큰은 VIEWER 로 발급되므로 운영자는 업체 설정을 바꿀 수 없다.
 */
@Service
public class FaqService {

    private final FaqRepository faqRepository;

    public FaqService(FaqRepository faqRepository) {
        this.faqRepository = faqRepository;
    }

    @Transactional(readOnly = true)
    public List<FaqResponse> list() {
        UUID tenantId = CurrentAuth.tenantUser().tenantId();
        return faqRepository.findAllByTenantIdOrderBySortOrderAsc(tenantId).stream()
                .map(FaqResponse::from)
                .toList();
    }

    @Transactional
    public FaqResponse create(FaqSaveRequest request) {
        AuthPrincipal.TenantUser user = CurrentAuth.requireEditor();
        // 새 항목은 맨 뒤로. 기존 순서를 흔들지 않는다.
        int nextOrder = faqRepository.findAllByTenantIdOrderBySortOrderAsc(user.tenantId()).stream()
                .mapToInt(Faq::getSortOrder)
                .max()
                .orElse(0) + 1;

        Faq faq = Faq.of(user.tenantId(), request.question().strip(), request.answer().strip(),
                request.links(), request.followUpFaqIds(), request.shown(), nextOrder);
        return FaqResponse.from(faqRepository.save(faq));
    }

    @Transactional
    public FaqResponse update(UUID faqId, FaqSaveRequest request) {
        AuthPrincipal.TenantUser user = CurrentAuth.requireEditor();
        Faq faq = find(faqId, user.tenantId());
        faq.edit(request.question().strip(), request.answer().strip(), request.links(),
                request.followUpFaqIds(), request.shown());
        return FaqResponse.from(faq);
    }

    @Transactional
    public void delete(UUID faqId) {
        AuthPrincipal.TenantUser user = CurrentAuth.requireEditor();
        faqRepository.delete(find(faqId, user.tenantId()));
    }

    /**
     * 순서 재배치. 받은 목록의 순서대로 1부터 다시 매긴다.
     *
     * <p>요청에 빠진 항목이 있으면 거부한다. 일부만 받아 나머지를 뒤로 미루면
     * 화면이 보여준 순서와 저장된 순서가 어긋난다.
     */
    @Transactional
    public List<FaqResponse> reorder(FaqOrderRequest request) {
        AuthPrincipal.TenantUser user = CurrentAuth.requireEditor();
        List<Faq> existing = faqRepository.findAllByTenantIdOrderBySortOrderAsc(user.tenantId());

        if (existing.size() != request.faqIds().size()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        Map<UUID, Faq> byId = existing.stream().collect(Collectors.toMap(Faq::getId, Function.identity()));

        int order = 1;
        for (UUID id : request.faqIds()) {
            Faq faq = byId.get(id);
            if (faq == null) {
                // 남의 테넌트 id 이거나 없는 id. 어느 쪽이든 순서를 못 맞춘다.
                throw new BusinessException(ErrorCode.FAQ_NOT_FOUND);
            }
            faq.moveTo(order++);
        }
        return list();
    }

    private Faq find(UUID faqId, UUID tenantId) {
        return faqRepository.findByIdAndTenantId(faqId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FAQ_NOT_FOUND));
    }
}
