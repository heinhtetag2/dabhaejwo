import type { ReactNode } from "react";

import { AuthGuard } from "@/widgets/auth-guard/auth-guard";
import { InternalBar } from "@/widgets/internal-bar/internal-bar";
import { NotificationBell } from "@/widgets/notification-bell/notification-bell";
import { OpsSidebar } from "@/widgets/sidebar/ops-sidebar";

/**
 * 인증 셸. 사이드바 + 내부용 표시 띠를 모든 운영 화면에 공통으로 씌운다.
 *
 * 클라이언트 가드는 UX 일 뿐이고 권한의 진실은 서버다 —
 * 각 API 가 @RequirePermission 으로 다시 검증한다.
 */
export default function ShellLayout({ children }: { children: ReactNode }) {
  return (
    <AuthGuard>
      <div className="flex min-h-dvh">
        <OpsSidebar />
        <div className="flex min-w-0 flex-1 flex-col">
          <InternalBar />
          {/* 벨은 어느 화면에서도 같은 자리에 있어야 한다 — 알림은 지금 보고 있는
              화면과 무관하게 온다. 조합은 여기(app 레이어)서 한다. */}
          <div className="flex justify-end border-b border-line bg-card px-7 py-2">
            <NotificationBell />
          </div>
          <main className="px-7 pt-6 pb-15">{children}</main>
        </div>
      </div>
    </AuthGuard>
  );
}
