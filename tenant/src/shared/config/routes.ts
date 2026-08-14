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

  // 대시보드 — 계정 (업체 단위. 서비스를 가리지 않는다)
  plan: "/app/plan",
  team: "/app/team",
  bots: "/app/bots",
  botNew: "/app/bots/new",
} as const;

export type RoutePath = (typeof ROUTES)[keyof typeof ROUTES];

/**
 * 서비스별 화면. <b>서비스가 URL 세그먼트다.</b>
 *
 * 전역 선택 상태로 두면 서비스 둘을 두 탭에 띄울 수 없다 — 한쪽을 바꾸면 다른 쪽이
 * 다음 조회에서 조용히 따라 바뀐다. 그런데 여러 서비스를 나란히 비교하는 것이
 * 이 기능을 쓰는 사람이 하는 일 그 자체다.
 *
 * `/s/` 표식이 있는 이유는 `/app/plan`·`/app/team` 과 첫 세그먼트가 자리를 다투지 않게
 * 하기 위해서다 — URL 에 `/s/` 가 있으면 서비스별, 없으면 업체 단위다.
 */
export const BOT_SCREENS = [
  "", "improve", "conversations", "leads",
  "sources", "faq", "appearance", "widget", "install",
] as const;

export type BotScreen = (typeof BOT_SCREENS)[number];

export function botRoute(botId: string | null, screen: BotScreen = ""): string {
  if (!botId) {
    // 서비스를 모르는 자리(로그인 직후 등). 진입점이 알아서 현재 서비스로 보낸다.
    return "/app";
  }
  return screen === "" ? `/app/s/${botId}` : `/app/s/${botId}/${screen}`;
}

export const NAV_GROUPS: ReadonlyArray<{
  label: string;
  items: ReadonlyArray<{ screen: BotScreen; label: string }>;
}> = [
  {
    label: "운영",
    items: [
      { screen: "", label: "홈" },
      { screen: "improve", label: "답변 개선" },
      { screen: "conversations", label: "대화 로그" },
      { screen: "leads", label: "남긴 연락처" },
    ],
  },
  {
    label: "챗봇",
    items: [
      { screen: "sources", label: "지식 소스" },
      { screen: "faq", label: "공통 질문" },
      { screen: "appearance", label: "말투" },
      { screen: "widget", label: "위젯 관리" },
      { screen: "install", label: "설치" },
    ],
  },
];

/** 업체 단위 항목. 서비스를 바꿔도 그대로다. */
export const ACCOUNT_NAV: ReadonlyArray<{ href: string; label: string }> = [
  { href: ROUTES.bots, label: "서비스" },
  { href: ROUTES.plan, label: "요금제" },
  { href: ROUTES.team, label: "팀원" },
];

/** 공개 헤더의 이동 항목. */
export const PUBLIC_NAV: ReadonlyArray<{ href: RoutePath; label: string }> = [
  { href: ROUTES.pricing, label: "요금제" },
];
