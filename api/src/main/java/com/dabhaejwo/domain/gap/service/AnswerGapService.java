package com.dabhaejwo.domain.gap.service;

import com.dabhaejwo.domain.faq.entity.Faq;
import com.dabhaejwo.domain.faq.repository.FaqRepository;
import com.dabhaejwo.domain.gap.dto.request.GapResolveRequest;
import com.dabhaejwo.domain.gap.dto.response.AnswerGapResponse;
import com.dabhaejwo.domain.gap.entity.AnswerGap;
import com.dabhaejwo.domain.gap.entity.GapStatus;
import com.dabhaejwo.domain.gap.repository.AnswerGapRepository;
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
 * 답변 개선 — 이 서비스의 심장이다 (tenant-plan.md §4.2).
 *
 * <p>개선 루프가 여기서 닫힌다: 방문자 질문 → 답변 실패 → 목록에 쌓임 → 업체가 답 등록
 * → 다음부터 챗봇이 답함. 등록된 답은 공통 질문이 되므로 <b>모델을 거치지 않고</b> 나간다.
 */
@Service
public class AnswerGapService {

    private final AnswerGapRepository gapRepository;
    private final FaqRepository faqRepository;

    public AnswerGapService(AnswerGapRepository gapRepository, FaqRepository faqRepository) {
        this.gapRepository = gapRepository;
        this.faqRepository = faqRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AnswerGapResponse> list(GapStatus status, int page, Integer size) {
        UUID tenantId = CurrentAuth.tenantUser().tenantId();
        Pageable pageable = PageRequest.of(Math.max(page, 0), PageResponse.clampSize(size));

        return PageResponse.of(
                gapRepository.findAllByTenantIdAndStatusOrderByLastAskedAtDesc(
                        tenantId, status == null ? GapStatus.OPEN : status, pageable),
                AnswerGapResponse::from);
    }

    /**
     * 답을 등록해 공통 질문으로 승격한다.
     *
     * <p>버튼으로는 내보내지 않는다({@code shown=false}). 방문자가 실제로 친 말은 버튼 문구로
     * 어울리지 않는 경우가 많고, 업체가 공통 질문 화면에서 다듬은 뒤 켜는 편이 낫다.
     * 켜지 않아도 <b>비슷한 질문이 들어오면 이 답변이 쓰인다</b> — 개선 효과는 즉시 생긴다.
     */
    @Transactional
    public AnswerGapResponse resolve(Long gapId, GapResolveRequest request) {
        AuthPrincipal.TenantUser user = CurrentAuth.requireEditor();
        AnswerGap gap = find(gapId, user.tenantId());

        if (gap.getStatus() == GapStatus.RESOLVED) {
            // 이미 답이 등록된 질문이다. 두 번 등록하면 같은 답변이 공통 질문에 둘 생긴다.
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION,
                    "이미 답변이 등록된 질문입니다");
        }

        String question = (request.question() == null || request.question().isBlank())
                ? gap.getQuestion()
                : request.question().strip();
        int nextOrder = faqRepository.findAllByTenantIdOrderBySortOrderAsc(user.tenantId()).stream()
                .mapToInt(Faq::getSortOrder)
                .max()
                .orElse(0) + 1;

        Faq faq = faqRepository.save(Faq.of(user.tenantId(), question, request.answer().strip(),
                List.of(), List.of(), false, nextOrder));
        gap.resolveWith(faq.getId());
        return AnswerGapResponse.from(gap);
    }

    /** 목록에서만 감춘다. 같은 질문이 다시 들어오면 되살아난다. */
    @Transactional
    public AnswerGapResponse dismiss(Long gapId) {
        AuthPrincipal.TenantUser user = CurrentAuth.requireEditor();
        AnswerGap gap = find(gapId, user.tenantId());
        gap.dismiss();
        return AnswerGapResponse.from(gap);
    }

    private AnswerGap find(Long gapId, UUID tenantId) {
        return gapRepository.findByIdAndTenantId(gapId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_GAP_NOT_FOUND));
    }
}
