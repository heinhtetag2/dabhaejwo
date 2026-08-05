package com.dabhaejwo.domain.operator.controller;

import com.dabhaejwo.domain.operator.dto.response.OperatorResponse;
import com.dabhaejwo.domain.operator.entity.Operator;
import com.dabhaejwo.domain.operator.repository.OperatorRepository;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.CurrentAuth;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 지금 로그인한 운영자.
 *
 * <p><b>왜 필요한가.</b> 운영자 정보는 로그인 응답에만 실려 있었다. 그래서 새로고침으로
 * 세션을 되살리면 토큰은 있는데 <b>내가 누구인지 모르는 상태</b>가 된다 — 사이드바에 이름이
 * 안 뜨고, 역할을 모르니 권한 판정이 전부 거짓이 되어 메뉴가 통째로 사라진다.
 *
 * <p>업체 대시보드에는 같은 역할을 하는 {@code GET /api/app/me} 가 이미 있었다.
 * 운영 콘솔에만 없어서 세션 복원을 붙일 수 없었던 것이다.
 *
 * <p>권한을 걸지 않는다 — <b>자기 자신</b>을 읽는 것이라 어떤 역할이든 볼 수 있어야 하고,
 * 대상은 토큰에서만 유도하므로 남을 조회할 방법이 없다.
 */
@RestController
@RequestMapping("/api/ops")
public class OperatorMeController {

    private final OperatorRepository repository;

    public OperatorMeController(OperatorRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/me")
    public OperatorResponse me() {
        Operator operator = repository.findById(CurrentAuth.operator().operatorId())
                // 토큰은 살아 있는데 계정이 사라진 경우다. 비활성화가 아니라 삭제인데,
                // 우리는 운영자를 삭제하지 않으므로 정상 경로에서는 일어나지 않는다.
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));

        // 비활성화된 계정은 토큰이 남아 있어도 들여보내지 않는다. 액세스 토큰 수명(30분)
        // 만큼은 기존 토큰이 살아 있으므로, 여기서 막지 않으면 그 사이 세션 복원이 된다.
        if (!operator.isActive()) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        return OperatorResponse.from(operator);
    }
}
