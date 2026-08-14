package com.dabhaejwo.domain.tenant.service;

import com.dabhaejwo.domain.bot.service.BotContext;
import com.dabhaejwo.domain.tenant.dto.response.AllowedOriginResponse;
import com.dabhaejwo.domain.tenant.entity.AllowedOrigin;
import com.dabhaejwo.domain.tenant.repository.AllowedOriginRepository;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.BotScope;
import com.dabhaejwo.global.security.CurrentAuth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 허용 주소 관리.
 *
 * <p>마지막 하나를 지우지 못하게 막는다. 전부 지우면 위젯이 어디서도 뜨지 않는데,
 * 업체는 "왜 갑자기 챗봇이 사라졌는지" 알 방법이 없다.
 *
 * <p><b>주소는 서비스에 속한다.</b> 업체 범위로 두면 A 서비스 키를 B 서비스 도메인에 붙여도
 * 통과한다 — 위젯 인증이 이 목록으로 판정하기 때문이다.
 */
@Service
public class AllowedOriginService {

    private final AllowedOriginRepository originRepository;
    private final BotContext botContext;

    public AllowedOriginService(AllowedOriginRepository originRepository, BotContext botContext) {
        this.originRepository = originRepository;
        this.botContext = botContext;
    }

    @Transactional(readOnly = true)
    public List<AllowedOriginResponse> list() {
        return originRepository.findAllByBotId(botContext.scope().botId()).stream()
                .map(AllowedOriginResponse::from)
                .toList();
    }

    @Transactional
    public AllowedOriginResponse add(String rawOrigin) {
        CurrentAuth.requireEditor();
        BotScope scope = botContext.scope();
        String host = AllowedOrigin.normalizeHost(rawOrigin);

        if (host.isBlank() || !host.contains(".")) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "주소 형식이 올바르지 않습니다. 예: shop.example.com");
        }
        if (originRepository.existsByBotIdAndOrigin(scope.botId(), host)) {
            // 같은 주소를 두 번 넣으면 목록만 지저분해진다. 이미 있으니 그대로 돌려준다.
            return originRepository.findAllByBotId(scope.botId()).stream()
                    .filter(origin -> origin.getOrigin().equals(host))
                    .findFirst()
                    .map(AllowedOriginResponse::from)
                    .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED));
        }
        return AllowedOriginResponse.from(
                originRepository.save(AllowedOrigin.of(scope, host)));
    }

    @Transactional
    public void remove(UUID originId) {
        CurrentAuth.requireEditor();
        List<AllowedOrigin> origins = originRepository.findAllByBotId(botContext.scope().botId());

        if (origins.size() <= 1) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "마지막 주소는 지울 수 없습니다. 지우면 챗봇이 어디에서도 뜨지 않습니다");
        }
        AllowedOrigin target = origins.stream()
                .filter(origin -> origin.getId().equals(originId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.SOURCE_NOT_FOUND,
                        "해당 주소를 찾을 수 없습니다"));
        originRepository.delete(target);
    }
}
