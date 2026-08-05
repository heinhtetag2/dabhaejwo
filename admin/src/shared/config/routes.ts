/**
 * 라우트 경로는 여기 상수로만 쓴다. 문자열을 화면에 흩뿌리면 이름이 바뀔 때 놓친다.
 *
 * 화면 구성은 docs/plan/admin-console-plan.md §3 과 일치한다.
 */
export const ROUTES = {
  login: "/login",
  forgotPassword: "/forgot-password",

  // 운영
  today: "/today",
  tenants: "/tenants",
  profitability: "/profitability",
  aiUsage: "/ai-usage",
  jobs: "/jobs",

  // 서비스
  plans: "/plans",
  models: "/models",
  flags: "/flags",

  // 지원
  tickets: "/tickets",
  audit: "/audit",
  operators: "/operators",
} as const;

export type RoutePath = (typeof ROUTES)[keyof typeof ROUTES];

/**
 * 사이드바 구성. 3그룹은 기획서의 화면 구조 트리 그대로다.
 *
 * `permission` 이 있는 항목은 그 권한이 없는 역할에게 숨긴다. **이건 UX 다** —
 * 눌러 봐야 서버가 403 을 준다. 권한의 진실은 서버이고 여기는 헛걸음을 줄일 뿐이다.
 * 권한 키는 api 의 `Permission` enum 과 같은 문자열이어야 한다.
 */
export const NAV_GROUPS: ReadonlyArray<{
  label: string;
  items: ReadonlyArray<{ href: RoutePath; label: string; permission?: string }>;
}> = [
  {
    label: "운영",
    items: [
      // 오늘은 전 역할이 본다 (admin-console-plan.md §7).
      { href: ROUTES.today, label: "오늘" },
      { href: ROUTES.tenants, label: "업체", permission: "TENANT_READ" },
      { href: ROUTES.profitability, label: "수익성", permission: "PROFITABILITY_READ" },
      { href: ROUTES.aiUsage, label: "AI 사용량", permission: "AI_USAGE_READ" },
      // 개발자만 보는 화면처럼 보이지만 CS 문의의 상당수가 작업 실패에서 비롯되므로
      // 운영 그룹에 함께 둔다 (admin-console-plan.md §3)
      { href: ROUTES.jobs, label: "작업 큐", permission: "JOB_READ" },
    ],
  },
  {
    label: "서비스",
    items: [
      { href: ROUTES.plans, label: "요금제", permission: "PLAN_READ" },
      { href: ROUTES.models, label: "모델과 프롬프트", permission: "MODEL_PRICE_READ" },
      { href: ROUTES.flags, label: "기능 공개", permission: "FLAG_READ" },
    ],
  },
  {
    label: "지원",
    items: [
      { href: ROUTES.tickets, label: "문의", permission: "TICKET_READ" },
      // 감사 기록 열람과 운영자 계정 관리는 운영 관리자 전용이다.
      { href: ROUTES.audit, label: "감사 기록", permission: "AUDIT_READ" },
      { href: ROUTES.operators, label: "관리자", permission: "OPERATOR_READ" },
    ],
  },
];
