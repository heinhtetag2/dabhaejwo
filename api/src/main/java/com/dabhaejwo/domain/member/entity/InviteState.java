package com.dabhaejwo.domain.member.entity;

public enum InviteState {
    /** 초대 메일만 보낸 상태. 비밀번호가 없어 로그인할 수 없다. */
    PENDING,
    ACCEPTED
}
