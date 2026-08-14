import type { Metadata } from "next";
import { Suspense } from "react";

import { PasswordResetView } from "@/features/auth/password-reset";
import { getLanguage } from "@/shared/lib/get-language";

export const metadata: Metadata = { title: "비밀번호 찾기 — 답해줘" };

/**
 * {@code useSearchParams} 를 쓰는 화면은 Suspense 경계가 필요하다 —
 * 없으면 빌드가 정적 생성을 포기하고 페이지 전체가 동적으로 바뀐다.
 */
export default async function Page() {
  const language = await getLanguage();
  return (
    <Suspense fallback={null}>
      <PasswordResetView language={language} />
    </Suspense>
  );
}
