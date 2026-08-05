import Link from "next/link";

import { LinkButton } from "@/shared/common/button";
import { PUBLIC_NAV, ROUTES } from "@/shared/config/routes";

/**
 * 공개 영역 헤더.
 *
 * <p>Server Component 다 — 로그인 여부에 따라 모양을 바꾸지 않는다. 토큰이 메모리에만
 * 있어서 서버는 어차피 알 수 없고, 알아내려고 클라이언트 컴포넌트로 만들면 랜딩 전체가
 * 클라이언트 번들에 들어간다. 이미 로그인한 사람은 대시보드로 보내는 것으로 충분하다.
 *
 * <p>스크롤 위치에 따라 모양을 바꾸지 않는다 — 그러려면 클라이언트 컴포넌트가 되어야 하고,
 * 그 대가로 랜딩이 통째로 클라이언트 번들에 들어간다. 대신 옅은 선 하나를 항상 둔다.
 * 선이 없으면 흰 본문이 헤더 아래로 지나갈 때 경계가 사라진다.
 */
export function PublicHeader() {
  return (
    <header className="sticky top-0 z-30 border-b border-edge bg-card/80 backdrop-blur-md">
      <div className="mx-auto flex h-[64px] max-w-[1080px] items-center gap-5 px-5">
        <Link
          href={ROUTES.landing}
          className="flex items-center gap-2.5 text-[16px] font-bold tracking-[-0.03em]"
        >
          <span
            aria-hidden
            className="grid size-6.5 place-items-center rounded-[9px] bg-ink text-[13px] font-bold text-mark"
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
              {item.label}
            </Link>
          ))}
        </nav>

        <div className="ml-auto flex items-center gap-1.5">
          <LinkButton
            href={ROUTES.login}
            variant="ghost"
            className="rounded-[10px] px-3 py-2 text-[14.5px] font-medium hover:bg-fill hover:text-ink"
          >
            로그인
          </LinkButton>
          <LinkButton
            href={ROUTES.signup}
            variant="primary"
            className="rounded-[10px] px-3.5 py-2 text-[14.5px] font-semibold"
          >
            무료로 시작
          </LinkButton>
        </div>
      </div>
    </header>
  );
}
