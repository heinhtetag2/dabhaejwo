import Link from "next/link";

import { LinkButton } from "@/shared/common/button";
import { PUBLIC_NAV, ROUTES } from "@/shared/config/routes";

/**
 * 공개 영역 헤더.
 *
 * <p>Server Component 다 — 로그인 여부에 따라 모양을 바꾸지 않는다. 토큰이 메모리에만
 * 있어서 서버는 어차피 알 수 없고, 알아내려고 클라이언트 컴포넌트로 만들면 랜딩 전체가
 * 클라이언트 번들에 들어간다. 이미 로그인한 사람은 대시보드로 보내는 것으로 충분하다.
 */
export function PublicHeader() {
  return (
    <header className="sticky top-0 z-30 border-b border-line bg-card/95 backdrop-blur">
      <div className="mx-auto flex max-w-[1080px] items-center gap-4 px-5 py-3.5">
        <Link href={ROUTES.landing} className="flex items-center gap-2.5">
          <span className="grid size-[22px] place-items-center rounded-md bg-mark font-mono text-xs font-bold text-ink">
            A
          </span>
          <span className="font-semibold tracking-[-0.01em]">답해줘</span>
        </Link>

        <nav aria-label="주요 메뉴" className="ml-3 hidden gap-1 sm:flex">
          {PUBLIC_NAV.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className="rounded-md px-2.5 py-1.5 text-[13.5px] text-slate transition-colors hover:bg-line-2 hover:text-ink"
            >
              {item.label}
            </Link>
          ))}
        </nav>

        <div className="ml-auto flex items-center gap-2">
          <LinkButton href={ROUTES.login} size="sm" variant="ghost">
            로그인
          </LinkButton>
          <LinkButton href={ROUTES.signup} size="sm" variant="primary">
            무료로 시작하기
          </LinkButton>
        </div>
      </div>
    </header>
  );
}
