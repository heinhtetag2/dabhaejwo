import { redirect } from "next/navigation";

import { ROUTES } from "@/shared/config/routes";

export default function Page() {
  // 진입 화면은 '오늘' — 로그인 직후 오늘 해야 할 일을 파악하는 게 목적이다.
  redirect(ROUTES.today);
}
