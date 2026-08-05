import type { ReactNode } from "react";

import { PublicFooter } from "@/widgets/public-shell/public-footer";
import { PublicHeader } from "@/widgets/public-shell/public-header";

/**
 * 공개 영역 셸.
 *
 * <p>Server Component 로 둔다 — 검색 유입이 유일한 경로라 JS 없이도 내용이 읽혀야 한다
 * (docs/plan/tenant-public-plan.md §2.4).
 */
export default function PublicLayout({ children }: { children: ReactNode }) {
  return (
    // 대시보드는 옅은 회색 위에서 카드를 구분하지만, 공개 영역은 흰 바탕이 기본이다.
    // 읽는 화면과 다루는 화면은 필요한 것이 다르다.
    <div className="flex min-h-dvh flex-col bg-card">
      <PublicHeader />
      <main className="flex-1">{children}</main>
      <PublicFooter />
    </div>
  );
}
