import type { ReactNode } from "react";

import { SessionGuard } from "@/features/auth/session-guard";
import { ImpersonationBanner } from "@/widgets/impersonation-banner/impersonation-banner";
import { NotificationBell } from "@/widgets/notification-bell/notification-bell";
import { TenantSidebar } from "@/widgets/sidebar/tenant-sidebar";

export default function ShellLayout({ children }: { children: ReactNode }) {
  return (
    <SessionGuard>
      <div className="flex min-h-dvh">
        <TenantSidebar />
        <div className="flex min-w-0 flex-1 flex-col">
          {/* 운영팀 대리 접속 중에만 뜬다 */}
          <ImpersonationBanner />
          {/* 벨은 어느 화면에서도 같은 자리에 있어야 한다 — 알림은 지금 보고 있는
              화면과 무관하게 온다. 조합은 여기(app 레이어)서 한다. */}
          <div className="flex justify-end border-b border-line bg-card px-7.5 py-2">
            <NotificationBell />
          </div>
          <main className="px-7.5 pt-6.5 pb-15">{children}</main>
        </div>
      </div>
    </SessionGuard>
  );
}
