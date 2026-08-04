import type { Metadata } from "next";

import { GuestOnly } from "@/features/auth/session-guard";
import { SignupView } from "@/features/auth/signup";

export const metadata: Metadata = {
  title: "무료로 시작하기 — 답해줘",
  description: "사이트 주소만 있으면 됩니다. 카드 등록 없이 14일 무료로 시작하세요.",
};

export default function Page() {
  return (
    <GuestOnly>
      <SignupView />
    </GuestOnly>
  );
}
