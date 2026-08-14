"use client";

import type { PublicPlan } from "@/entities/public/plan";
import type { Language } from "@/shared/lib/language";
import { VersionSwitch } from "@/shared/ui/version-switch";

import { PricingView } from "./pricing-view";
import { PricingViewV2 } from "./pricing-view-v2";

/** 요금제 화면의 Original/Revamp 비교 스위처. `pricing-view-v2.tsx` 참조. */
export function PricingVersionSwitch({ plans, language }: { plans: PublicPlan[]; language: Language }) {
  return (
    <VersionSwitch
      original={<PricingView plans={plans} language={language} />}
      revamp={<PricingViewV2 plans={plans} language={language} />}
    />
  );
}
