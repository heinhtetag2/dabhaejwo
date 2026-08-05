import type { Metadata } from "next";
import { Suspense } from "react";

import { InviteAcceptView } from "@/features/auth/invite";

export const metadata: Metadata = { title: "초대 수락 — 답해줘" };

export default function Page() {
  return (
    <Suspense fallback={null}>
      <InviteAcceptView />
    </Suspense>
  );
}
