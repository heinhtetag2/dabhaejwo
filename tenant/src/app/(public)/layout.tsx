import type { ReactNode } from "react";

import { LoginModal } from "@/features/auth/login";
import { SignupModal } from "@/features/auth/signup";
import { getLanguage } from "@/shared/lib/get-language";
import { PublicFooter } from "@/widgets/public-shell/public-footer";
import { PublicHeader } from "@/widgets/public-shell/public-header";

/**
 * 공개 영역 셸.
 *
 * <p>Server Component 로 둔다 — 검색 유입이 유일한 경로라 JS 없이도 내용이 읽혀야 한다
 * (docs/plan/tenant-public-plan.md §2.4). `SignupModal`·`LoginModal` 은 그 안에서
 * `"use client"` 인 별도 섬(island)이라 이 레이아웃 자체가 클라이언트 컴포넌트가 되지는 않는다.
 *
 * <p>모달을 여기 한 번만 마운트한다 — 페이지마다 각자 두면 열림 상태가 페이지 전환에
 * 끊기고, 헤더·푸터·랜딩·요금제가 모달을 각자 인스턴스화하게 된다.
 */
export default async function PublicLayout({ children }: { children: ReactNode }) {
  const language = await getLanguage();

  return (
    // 대시보드는 옅은 회색 위에서 카드를 구분하지만, 공개 영역은 흰 바탕이 기본이다.
    // 읽는 화면과 다루는 화면은 필요한 것이 다르다.
    <div className="flex min-h-dvh flex-col bg-card">
      <PublicHeader />
      <main className="flex-1">{children}</main>
      <PublicFooter />
      <SignupModal language={language} />
      <LoginModal language={language} />
    </div>
  );
}
