package com.dabhaejwo.domain.tenant.service;

import com.dabhaejwo.domain.tenant.dto.response.AllowedOriginResponse;
import com.dabhaejwo.domain.tenant.entity.AllowedOrigin;
import com.dabhaejwo.domain.tenant.repository.AllowedOriginRepository;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.AuthPrincipal;
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
 */
@Service
public class AllowedOriginService {

    private final AllowedOriginRepository originRepository;

    public AllowedOriginService(AllowedOriginRepository originRepository) {
        this.originRepository = originRepository;
    }

    @Transactional(readOnly = true)
    public List<AllowedOriginResponse> list() {
        UUID tenantId = CurrentAuth.tenantUser().tenantId();
        return originRepository.findAllByTenantId(tenantId).stream()
                .map(AllowedOriginResponse::from)
                .toList();
    }

    @Transactional
    public AllowedOriginResponse add(String rawOrigin) {
        AuthPrincipal.TenantUser user = CurrentAuth.requireEditor();
        String host = AllowedOrigin.normalizeHost(rawOrigin);

        if (host.isBlank() || !host.contains(".")) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "주소 형식이 올바르지 않습니다. 예: shop.example.com");
        }
        if (originRepository.existsByTenantIdAndOrigin(user.tenantId(), host)) {
            // 같은 주소를 두 번 넣으면 목록만 지저분해진다. 이미 있으니 그대로 돌려준다.
            return originRepository.findAllByTenantId(user.tenantId()).stream()
                    .filter(origin -> origin.getOrigin().equals(host))
                    .findFirst()
                    .map(AllowedOriginResponse::from)
                    .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED));
        }
        return AllowedOriginResponse.from(
                originRepository.save(AllowedOrigin.of(user.tenantId(), host)));
    }

    @Transactional
    public void remove(UUID originId) {
        AuthPrincipal.TenantUser user = CurrentAuth.requireEditor();
        List<AllowedOrigin> origins = originRepository.findAllByTenantId(user.tenantId());

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
