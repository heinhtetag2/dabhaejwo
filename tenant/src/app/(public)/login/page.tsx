import type { Metadata } from "next";

import { LoginView } from "@/features/auth/login";
import { GuestOnly } from "@/features/auth/session-guard";

export const metadata: Metadata = { title: "로그인 — 답해줘" };

export default function Page() {
  return (
    <GuestOnly>
      <LoginView />
    </GuestOnly>
  );
}
