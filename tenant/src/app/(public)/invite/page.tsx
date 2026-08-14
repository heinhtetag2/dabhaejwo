import type { Metadata } from "next";
import { Suspense } from "react";

import { InviteAcceptView } from "@/features/auth/invite";
import { getLanguage } from "@/shared/lib/get-language";

export const metadata: Metadata = { title: "초대 수락 — 답해줘" };

export default async function Page() {
  const language = await getLanguage();
  return (
    <Suspense fallback={null}>
      <InviteAcceptView language={language} />
    </Suspense>
  );
}
