/**
 * 라우트 경로는 여기 상수로만 쓴다. 문자열을 화면에 흩뿌리면 이름이 바뀔 때 놓친다.
 *
 * 화면 구성은 docs/plan/admin-console-plan.md §3 과 일치한다.
 */
export const ROUTES = {
  login: "/login",

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
} as const;

export type RoutePath = (typeof ROUTES)[keyof typeof ROUTES];

/** 사이드바 구성. 3그룹은 기획서의 화면 구조 트리 그대로다. */
export const NAV_GROUPS: ReadonlyArray<{
  label: string;
  items: ReadonlyArray<{ href: RoutePath; label: string }>;
}> = [
  {
    label: "운영",
    items: [
      { href: ROUTES.today, label: "오늘" },
      { href: ROUTES.tenants, label: "업체" },
      { href: ROUTES.profitability, label: "수익성" },
      { href: ROUTES.aiUsage, label: "AI 사용량" },
      // 개발자만 보는 화면처럼 보이지만 CS 문의의 상당수가 작업 실패에서 비롯되므로
      // 운영 그룹에 함께 둔다 (admin-console-plan.md §3)
      { href: ROUTES.jobs, label: "작업 큐" },
    ],
  },
  {
    label: "서비스",
    items: [
      { href: ROUTES.plans, label: "요금제" },
      { href: ROUTES.models, label: "모델과 프롬프트" },
      { href: ROUTES.flags, label: "기능 공개" },
    ],
  },
  {
    label: "지원",
    items: [
      { href: ROUTES.tickets, label: "문의" },
      { href: ROUTES.audit, label: "감사 기록" },
    ],
  },
];
