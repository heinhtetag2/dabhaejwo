package com.dabhaejwo.global.security;

import java.util.EnumSet;
import java.util.Set;

/**
 * 운영자 역할 → 권한 매핑. <b>권한의 진실은 서버다</b> — 프론트에서 버튼을 숨기는 것은 UX이지 보안이 아니다.
 *
 * <p>매핑은 docs/plan/admin-console-plan.md §7 과 docs/plan/tenant-plan.md §8 의
 * 권한 매트릭스와 일치해야 한다. 기획서를 고치면 여기도 같은 커밋에서 고친다.
 */
public enum OperatorRole {

    /** 전체. 모델 단가·비용 안전장치는 이 역할만 만질 수 있다 — 잘못 건드리면 전체 원가 계산과 가용성에 즉시 영향이 간다. */
    OPS_ADMIN(EnumSet.allOf(Permission.class)),

    /** 조회 + 대리 로그인 + 쿼터 증량 + 작업 큐 재시도. 계정·금액을 바꾸는 행위는 막는다. */
    CS(EnumSet.of(
            Permission.TENANT_READ,
            Permission.TENANT_NOTE_WRITE,
            Permission.TENANT_IMPERSONATE,
            Permission.QUOTA_GRANT,
            Permission.JOB_READ,
            Permission.JOB_RETRY,
            Permission.TICKET_READ,
            Permission.TICKET_WRITE)),

    /** 조회 + 메모 + 체험 연장 + 요금제 변경 + 수익성. 대리 로그인은 불가. */
    SALES(EnumSet.of(
            Permission.TENANT_READ,
            Permission.TENANT_NOTE_WRITE,
            Permission.TENANT_TRIAL_WRITE,
            Permission.TENANT_PLAN_WRITE,
            Permission.PROFITABILITY_READ,
            Permission.PLAN_READ,
            Permission.PLAN_WRITE,
            Permission.TICKET_READ)),

    /**
     * 조회 + 대리 로그인 + AI 사용량 + 작업 큐 + 기능 공개.
     * 대리 로그인을 허용하는 이유는 재현이 안 되는 버그 문의가 이 역할로 넘어오기 때문이다.
     */
    DEV(EnumSet.of(
            Permission.TENANT_READ,
            Permission.TENANT_IMPERSONATE,
            Permission.AI_USAGE_READ,
            Permission.JOB_READ,
            Permission.JOB_RETRY,
            Permission.FLAG_READ,
            Permission.FLAG_WRITE,
            Permission.TICKET_READ));

    private final Set<Permission> permissions;

    OperatorRole(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public boolean can(Permission permission) {
        return permissions.contains(permission);
    }

    public Set<Permission> permissions() {
        return Set.copyOf(permissions);
    }
}
