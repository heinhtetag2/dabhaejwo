/**
 * 업체 대시보드 라우트.
 *
 * 공개 영역과 로그인 영역을 URL 로 가른다 — `/app` 아래가 로그인 필요다.
 * `/dashboard` 가 아니라 `/app` 인 이유는 API prefix(`/api/app/**`)와 같은 말을 쓰기 위해서다.
 * 같은 영역을 두 이름으로 부르면 코드에서 계속 번역하게 된다.
 *
 * 화면 구성 근거:
 *   공개 — docs/plan/tenant-public-plan.md §3
 *   대시보드 — docs/plan/tenant-plan.md §3
 */
export const ROUTES = {
  // 공개
  landing: "/",
  pricing: "/pricing",
  signup: "/signup",
  login: "/login",
  forgotPassword: "/forgot-password",
  invite: "/invite",
  terms: "/terms",
  privacy: "/privacy",

  // 대시보드 — 운영
  home: "/app",
  improve: "/app/improve",
  conversations: "/app/conversations",
  leads: "/app/leads",

  // 대시보드 — 챗봇
  sources: "/app/sources",
  faq: "/app/faq",
  appearance: "/app/appearance",
  widget: "/app/widget",
  install: "/app/install",

  // 대시보드 — 계정
  plan: "/app/plan",
  team: "/app/team",
} as const;

export type RoutePath = (typeof ROUTES)[keyof typeof ROUTES];

export const NAV_GROUPS: ReadonlyArray<{
  label: string;
  items: ReadonlyArray<{ href: RoutePath; label: string }>;
}> = [
  {
    label: "운영",
    items: [
      { href: ROUTES.home, label: "홈" },
      { href: ROUTES.improve, label: "답변 개선" },
      { href: ROUTES.conversations, label: "대화 로그" },
      { href: ROUTES.leads, label: "남긴 연락처" },
    ],
  },
  {
    label: "챗봇",
    items: [
      { href: ROUTES.sources, label: "지식 소스" },
      { href: ROUTES.faq, label: "공통 질문" },
      { href: ROUTES.appearance, label: "말투" },
      { href: ROUTES.widget, label: "위젯 관리" },
      { href: ROUTES.install, label: "설치" },
    ],
  },
  {
    label: "계정",
    items: [
      { href: ROUTES.plan, label: "요금제" },
      { href: ROUTES.team, label: "팀원" },
    ],
  },
];

/** 공개 헤더의 이동 항목. */
export const PUBLIC_NAV: ReadonlyArray<{ href: RoutePath; label: string }> = [
  { href: ROUTES.pricing, label: "요금제" },
];
