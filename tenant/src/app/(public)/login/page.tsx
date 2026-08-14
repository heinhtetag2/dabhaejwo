import type { Metadata } from "next";

import { LoginView } from "@/features/auth/login";
import { GuestOnly } from "@/features/auth/session-guard";
import { getLanguage } from "@/shared/lib/get-language";

export const metadata: Metadata = { title: "로그인 — 답해줘" };

export default async function Page() {
  const language = await getLanguage();
  return (
    <GuestOnly>
      <LoginView language={language} />
    </GuestOnly>
  );
}
