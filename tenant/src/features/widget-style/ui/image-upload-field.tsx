"use client";

import { useRef, useState } from "react";

import { useRemoveBrandingImage, useUploadBrandingImage } from "@/entities/chatbot/bot-settings";
import { ApiError } from "@/shared/api/http-client";
import { Button } from "@/shared/common/button";
import { env } from "@/shared/config/env";
import { Notice } from "@/shared/ui/notice";

/**
 * 로고·아이콘 업로드 한 칸.
 *
 * <p>이미지는 <b>고르는 즉시 올라간다.</b> 나머지 설정처럼 "저장" 버튼을 기다리게 하면,
 * 업로드는 멀티파트라 저장(JSON)과 한 요청으로 묶을 수 없어 두 번 저장하는 셈이 되고
 * 업체는 어느 쪽이 반영됐는지 알 수 없다.
 */
export function ImageUploadField({
  kind,
  label,
  hint,
  currentUrl,
  round,
}: {
  kind: "logo" | "launcher-icon";
  label: string;
  hint: string;
  currentUrl: string | null;
  round?: boolean;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [error, setError] = useState<string | null>(null);

  const upload = useUploadBrandingImage(kind);
  const remove = useRemoveBrandingImage(kind);
  const busy = upload.isPending || remove.isPending;

  const pick = (file: File | undefined) => {
    if (!file) return;
    setError(null);
    upload.mutate(file, {
      onError: (cause: unknown) =>
        setError(cause instanceof ApiError ? cause.message : "올리지 못했습니다"),
      // 같은 파일을 다시 고를 수 있게 비운다.
      onSettled: () => {
        if (inputRef.current) inputRef.current.value = "";
      },
    });
  };

  return (
    <div className="mb-4">
      <span className="mb-1.5 block text-[12.5px] font-medium">{label}</span>

      <div className="flex items-center gap-3">
        <span
          className={`grid size-14 shrink-0 place-items-center overflow-hidden border border-line bg-paper ${
            round ? "rounded-full" : "rounded-lg"
          }`}
        >
          {currentUrl ? (
            // 상대 경로로 저장돼 있다. API 도메인이 콘솔과 다르므로 여기서 붙인다.
            //
            // next/image 를 쓰지 않는다 — 최적화하려면 그 호스트를 next.config 에 미리
            // 등록해야 하는데 API 도메인은 배포 환경마다 다르고 빌드 시점에 정해진다.
            // 56px 짜리 썸네일이라 최적화로 얻을 것도 없다.
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={new URL(currentUrl, env.apiBaseUrl).toString()}
              alt=""
              className="size-full object-cover"
            />
          ) : (
            <span className="text-[11px] text-slate-2">없음</span>
          )}
        </span>

        <div className="min-w-0 flex-1">
          <p className="text-[11.5px] leading-relaxed text-slate-2">{hint}</p>
          <div className="mt-2 flex gap-2">
            <Button size="sm" disabled={busy} onClick={() => inputRef.current?.click()}>
              {upload.isPending ? "올리는 중…" : currentUrl ? "바꾸기" : "이미지 고르기"}
            </Button>
            {currentUrl ? (
              <Button
                size="sm"
                variant="danger"
                disabled={busy}
                onClick={() => {
                  setError(null);
                  remove.mutate();
                }}
              >
                지우기
              </Button>
            ) : null}
          </div>
        </div>
      </div>

      <input
        ref={inputRef}
        type="file"
        accept="image/png,image/jpeg,image/webp"
        className="hidden"
        onChange={(event) => pick(event.target.files?.[0])}
      />

      {error ? (
        <Notice tone="error" className="mt-2">
          {error}
        </Notice>
      ) : null}
    </div>
  );
}
