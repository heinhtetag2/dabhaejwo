"use client";

import type { Language } from "@/shared/lib/language";
import { VersionSwitch } from "@/shared/ui/version-switch";

import { LandingView } from "./landing-view";
import { LandingViewV2 } from "./landing-view-v2";

/** 랜딩 화면의 Original/Revamp 비교 스위처. `landing-view-v2.tsx` 참조. */
export function LandingVersionSwitch({ language }: { language: Language }) {
  return (
    <VersionSwitch
      original={<LandingView language={language} />}
      revamp={<LandingViewV2 language={language} />}
    />
  );
}
