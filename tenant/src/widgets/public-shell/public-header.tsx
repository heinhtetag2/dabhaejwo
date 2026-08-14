import Link from "next/link";

import { PUBLIC_NAV, ROUTES } from "@/shared/config/routes";
import { getLanguage } from "@/shared/lib/get-language";
import type { Language } from "@/shared/lib/language";
import { LoginCtaLink } from "@/shared/ui/login-cta-link";
import { SignupCtaLink } from "@/shared/ui/signup-cta-link";

import { LanguageSwitch } from "./language-switch";
import { PublicHeaderShell } from "./public-header-shell";

const NAV_LABEL: Record<string, Record<Language, string>> = {
  [ROUTES.pricing]: { en: "Pricing", ko: "요금제" },
};

const TEXT: Record<Language, { login: string; startFree: string }> = {
  en: { login: "Log in", startFree: "Start free" },
  ko: { login: "로그인", startFree: "무료로 시작" },
};

/**
 * 공개 영역 헤더.
 *
 * <p>Server Component 다 — 로그인 여부에 따라 모양을 바꾸지 않는다. 토큰이 메모리에만
 * 있어서 서버는 어차피 알 수 없고, 알아내려고 클라이언트 컴포넌트로 만들면 랜딩 전체가
 * 클라이언트 번들에 들어간다. 이미 로그인한 사람은 대시보드로 보내는 것으로 충분하다.
 *
 * <p>맨 위에서는 배경을 완전히 투명하게 둔다(사용자 결정, 참조: channel.io 가격 화면) — 선·
 * 반투명 배경 없이 히어로 색이 헤더 뒤로 그대로 이어진다. 헤더가 `sticky` 라 스크롤 시 본문이
 * 헤더 아래로 지나가며 겹칠 수 있다는 트레이드오프를 감수한다. 스크롤해 내려가면 숨겨지기
 * 직전부터 옅은 흰 배경이 얹힌다(`PublicHeaderShell`) — 투명한 채로 뚝 끊기듯 사라지지 않게.
 *
 * <p>스크롤 방향에 따라 숨고 보이는 동작은 `PublicHeaderShell`(클라이언트) 하나에만 있다 —
 * 이 컴포넌트는 여전히 Server Component 로 남아 언어 번역을 그대로 서버에서 렌더한다.
 *
 * <p>너비는 뷰포트 전체를 쓴다(사용자 결정) — 예전에는 `max-w-[1080px]` 로 가운데 정렬했다.
 * 세로 여백은 고정 높이 대신 상하 16px 패딩으로 준다.
 *
 * <p>언어 쿠키를 읽으므로(`getLanguage`) 이 화면은 동적 렌더로 opt-in 된다 — 감수한다.
 */
export async function PublicHeader() {
  const language = await getLanguage();
  const text = TEXT[language];

  return (
    <PublicHeaderShell>
      <div className="flex items-center gap-5 px-5 py-4 sm:px-8 lg:px-12">
        <Link
          href={ROUTES.landing}
          className="flex shrink-0 items-center gap-2.5 text-[16px] font-bold whitespace-nowrap tracking-[-0.03em]"
        >
          <span
            aria-hidden
            className="grid size-6.5 place-items-center rounded-[9px] bg-ink text-[13px] font-bold text-mark-bright"
          >
            답
          </span>
          답해줘
        </Link>

        <nav aria-label="주요 메뉴" className="hidden gap-1 sm:flex">
          {PUBLIC_NAV.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className="rounded-[10px] px-3 py-2 text-[14.5px] font-medium text-slate transition-colors hover:bg-fill hover:text-ink"
            >
              {NAV_LABEL[item.href]?.[language] ?? item.label}
            </Link>
          ))}
        </nav>

        <div className="ml-auto flex items-center gap-1.5">
          <LanguageSwitch language={language} />
          <span className="hidden min-[360px]:inline-flex">
            <LoginCtaLink
              variant="ghost"
              className="rounded-[10px] px-3 py-2 text-[14.5px] font-medium hover:bg-fill hover:text-ink"
            >
              {text.login}
            </LoginCtaLink>
          </span>
          <SignupCtaLink className="inline-flex items-center gap-1.5 whitespace-nowrap rounded-[10px] border border-ink bg-ink px-3.5 py-2 text-[14.5px] font-semibold text-white transition-colors hover:border-ink-2 hover:bg-ink-2">
            {text.startFree}
          </SignupCtaLink>
        </div>
      </div>
    </PublicHeaderShell>
  );
}
