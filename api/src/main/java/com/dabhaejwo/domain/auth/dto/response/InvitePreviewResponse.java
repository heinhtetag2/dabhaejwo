package com.dabhaejwo.domain.auth.dto.response;

import com.dabhaejwo.global.security.TenantMemberRole;

/**
 * 초대 링크를 열었을 때 보여줄 정보.
 *
 * <p>비밀번호를 정하기 전에 <b>어디에 초대됐는지</b>는 알려줘야 한다 — 모르는 업체 이름이
 * 뜨면 잘못 온 것이고, 그걸 비밀번호를 만든 뒤에 알면 늦다.
 *
 * <p>토큰을 가진 사람에게만 나가므로 이메일을 가리지 않는다. 초대받은 본인의 주소다.
 */
public record InvitePreviewResponse(String tenantName, String email, String name,
                                    TenantMemberRole role) {
}
