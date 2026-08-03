/**
 * 업체 대시보드 라우트.
 *
 * 화면 구성은 docs/prototype/chatbot-tenant-dashboard.html 의 사이드바와 일치한다.
 * (이 면은 별도 기획서가 없고 프로토타입이 사실상 스펙이다 — docs/intake.md §5-2)
 */
export const ROUTES = {
  login: "/login",

  // 운영
  home: "/",
  improve: "/improve",
  conversations: "/conversations",
  leads: "/leads",

  // 챗봇
  sources: "/sources",
  faq: "/faq",
  appearance: "/appearance",
  install: "/install",

  // 계정
  plan: "/plan",
  team: "/team",
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
      { href: ROUTES.appearance, label: "말투와 모양" },
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
