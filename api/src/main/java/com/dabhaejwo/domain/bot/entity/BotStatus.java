package com.dabhaejwo.domain.bot.entity;

/**
 * 서비스 상태.
 *
 * <p>업체 계약 상태({@code TenantStatus})와 <b>다른 축</b>이다 — 그쪽은 "돈을 내는가",
 * 이쪽은 "업체가 이 서비스를 켜뒀는가"다. 파생시키면 정지된 업체의 서비스를 되살릴 때
 * 업체가 원래 꺼둔 것까지 같이 켜진다.
 */
public enum BotStatus {

    ACTIVE,

    /** 업체가 잠시 내렸다. 위젯은 뜨지 않지만 데이터는 그대로다. */
    PAUSED,

    /** 삭제 유예 중. 위젯은 즉시 멈추고 데이터 파기는 유예 기간 뒤에 한다. */
    DELETING
}
