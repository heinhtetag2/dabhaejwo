import type { ReactNode } from "react";

import { SessionGuard } from "@/features/auth/session-guard";
import { ImpersonationBanner } from "@/widgets/impersonation-banner/impersonation-banner";
import { TenantSidebar } from "@/widgets/sidebar/tenant-sidebar";

export default function ShellLayout({ children }: { children: ReactNode }) {
  return (
    <SessionGuard>
      <div className="flex min-h-dvh">
        <TenantSidebar />
        <div className="flex min-w-0 flex-1 flex-col">
          {/* 운영팀 대리 접속 중에만 뜬다 */}
          <ImpersonationBanner />
          <main className="max-w-[1180px] px-7.5 pt-6.5 pb-15">{children}</main>
        </div>
      </div>
    </SessionGuard>
  );
}
