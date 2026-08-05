package com.dabhaejwo.domain.operator.dto.response;

import com.dabhaejwo.global.security.OperatorRole;

import java.util.List;

/**
 * 역할이 무엇을 할 수 있는가. api-contracts.md §1.
 *
 * <p><b>서버가 진실이라 서버가 준다.</b> 프론트에 매트릭스를 복제해 두면 코드가 바뀌어도
 * 화면은 옛 표를 보여주고, 운영자는 실제와 다른 권한을 믿게 된다.
 *
 * @param label       화면에 쓰는 한글 이름 (운영 관리자 · CS 담당 · 영업 담당 · 개발)
 * @param permissions 그 역할이 가진 권한 키. {@code OPS_ADMIN} 은 전부다
 */
public record RolePermissionsResponse(
        OperatorRole role,
        String label,
        List<String> permissions) {
}
