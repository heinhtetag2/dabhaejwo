"use client";

import { useState, type ReactNode } from "react";

import { cn } from "@/shared/lib/cn";

/**
 * 리디자인 비교용 임시 스위처.
 *
 * 방향이 정해지지 않은 상태에서 같은 화면 안에서 기존/개편 초안을 즉시 오가며 비교하기 위한
 * 것이다. 화면 흐름을 밀지 않도록 고정 위치로 띄운다. 둘 이상의 feature(pricing·landing)가
 * 같은 토글 UI 를 쓰므로 shared/ui 로 둔다 (fsd-rules).
 *
 * 방향이 확정되면 쓰는 곳에서 이 컴포넌트와 `*-v2.tsx` 를 지우고 원래 뷰 하나로 되돌린다.
 */
export function VersionSwitch({ original, revamp }: { original: ReactNode; revamp: ReactNode }) {
  const [version, setVersion] = useState<"original" | "revamp">("revamp");

  return (
    <>
      {version === "original" ? original : revamp}

      <div className="fixed right-6 bottom-6 z-50">
        <div className="inline-flex rounded-full border border-edge bg-card/95 p-1 text-[13.5px] font-medium shadow-lg backdrop-blur">
          <button
            type="button"
            onClick={() => setVersion("original")}
            className={cn(
              "rounded-full px-4 py-1.5 transition-colors",
              version === "original" ? "bg-ink text-white" : "text-slate hover:text-ink",
            )}
          >
            Original
          </button>
          <button
            type="button"
            onClick={() => setVersion("revamp")}
            className={cn(
              "rounded-full px-4 py-1.5 transition-colors",
              version === "revamp" ? "bg-ink text-white" : "text-slate hover:text-ink",
            )}
          >
            Revamp
          </button>
        </div>
      </div>
    </>
  );
}
