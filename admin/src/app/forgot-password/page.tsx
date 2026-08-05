import type { Metadata } from "next";
import { Suspense } from "react";

import { PasswordResetView } from "@/features/auth/password-reset";

export const metadata: Metadata = { title: "비밀번호 찾기 — 답해줘 운영" };

/** useSearchParams 를 쓰므로 Suspense 경계가 필요하다. */
export default function Page() {
  return (
    <Suspense fallback={null}>
      <PasswordResetView />
    </Suspense>
  );
}
