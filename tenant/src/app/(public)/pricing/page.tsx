import type { Metadata } from "next";

import { fetchPublicPlans } from "@/entities/public/plan";
import { PricingView } from "@/features/pricing";

export const metadata: Metadata = {
  title: "요금제 — 답해줘",
  description:
    "14일 무료 체험 후 사용량에 맞는 요금제를 고르세요. 한도를 넘어도 초과 요금은 청구되지 않습니다.",
};

export default async function Page() {
  return <PricingView plans={await fetchPublicPlans()} />;
}
