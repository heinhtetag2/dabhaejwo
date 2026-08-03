import type { ReactNode } from "react";

import { InternalBar } from "@/widgets/internal-bar/internal-bar";
import { OpsSidebar } from "@/widgets/sidebar/ops-sidebar";

/**
 * 인증 셸. 사이드바 + 내부용 표시 띠를 모든 운영 화면에 공통으로 씌운다.
 *
 * 클라이언트 가드는 UX 일 뿐이고 권한의 진실은 서버다 —
 * 각 API 가 @RequirePermission 으로 다시 검증한다.
 */
export default function ShellLayout({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-dvh">
      <OpsSidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <InternalBar />
        <main className="max-w-[1240px] px-7 pt-6 pb-15">{children}</main>
      </div>
    </div>
  );
}
