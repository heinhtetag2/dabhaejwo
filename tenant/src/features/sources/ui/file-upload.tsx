"use client";

import { useRef, useState } from "react";

import {
  ALLOWED_UPLOAD_EXTENSIONS,
  MAX_UPLOAD_MB,
  useUploadDocument,
} from "@/entities/chatbot/knowledge";
import { ApiError } from "@/shared/api/http-client";
import { Button } from "@/shared/common/button";
import { cn } from "@/shared/lib/cn";
import { Notice } from "@/shared/ui/notice";

/**
 * 파일 업로드.
 *
 * <p>여러 개를 고르면 <b>하나씩 순서대로</b> 올린다. 동시에 던지면 한도 검사가
 * 서로를 못 보고 지나쳐 한도를 넘겨 올라간다.
 *
 * <p>업로드가 끝나도 학습은 시작되지 않는다 — 임베딩 워커가 없다. 그 사실을 화면에
 * 그대로 적는다. "올렸으니 학습됐겠지"라고 오해하면 나중에 챗봇이 못 답하는 이유를 못 찾는다.
 */
export function FileUpload({ onUploaded }: { onUploaded?: () => void }) {
  const inputRef = useRef<HTMLInputElement>(null);
  const upload = useUploadDocument();

  const [dragging, setDragging] = useState(false);
  const [errors, setErrors] = useState<string[]>([]);
  const [done, setDone] = useState(0);
  const [total, setTotal] = useState(0);

  const send = async (files: FileList | null) => {
    if (!files || files.length === 0) {
      return;
    }
    setErrors([]);
    setDone(0);
    setTotal(files.length);

    const failed: string[] = [];
    for (const file of Array.from(files)) {
      try {
        await upload.mutateAsync(file);
      } catch (cause) {
        failed.push(
          `${file.name} — ${cause instanceof ApiError ? cause.message : "업로드하지 못했습니다"}`,
        );
      }
      setDone((count) => count + 1);
    }

    setErrors(failed);
    setTotal(0);
    if (inputRef.current) {
      // 같은 파일을 다시 고를 수 있게 비운다.
      inputRef.current.value = "";
    }
    if (failed.length < files.length) {
      onUploaded?.();
    }
  };

  const busy = total > 0;

  return (
    <div>
      <div
        onDragOver={(event) => {
          event.preventDefault();
          setDragging(true);
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={(event) => {
          event.preventDefault();
          setDragging(false);
          void send(event.dataTransfer.files);
        }}
        className={cn(
          "rounded-card border border-dashed px-5 py-7 text-center transition-colors",
          dragging ? "border-seal bg-seal-soft/40" : "border-line bg-paper",
        )}
      >
        <p className="text-[13.5px] font-medium">
          {busy ? `올리는 중… ${done}/${total}` : "파일을 여기에 끌어다 놓으세요"}
        </p>
        <p className="mt-1.5 text-[11.5px] text-slate-2">
          {ALLOWED_UPLOAD_EXTENSIONS.join(" · ")} · 파일당 {MAX_UPLOAD_MB}MB 까지
        </p>

        <input
          ref={inputRef}
          type="file"
          multiple
          accept={ALLOWED_UPLOAD_EXTENSIONS.join(",")}
          className="sr-only"
          id="knowledge-file"
          onChange={(event) => void send(event.target.files)}
        />
        <Button
          size="sm"
          className="mt-3.5"
          disabled={busy}
          onClick={() => inputRef.current?.click()}
        >
          파일 고르기
        </Button>
      </div>

      {errors.length > 0 ? (
        <Notice tone="error" className="mt-3">
          <span className="block font-medium">올리지 못한 파일이 있습니다</span>
          <span className="mt-1 block space-y-0.5">
            {errors.map((message) => (
              <span key={message} className="block">
                {message}
              </span>
            ))}
          </span>
        </Notice>
      ) : null}

      {/* 업로드 응답 시점에는 아직 "대기 중"이다. 즉시 학습됐다고 오해하게 두지 않는다. */}
      <Notice tone="info" className="mt-3">
        올린 파일은 <b className="font-semibold text-ink">잠시 뒤 자동으로 학습됩니다.</b> 그동안
        상태가 &quot;대기 중&quot;·&quot;처리 중&quot;으로 표시되며, 끝나면 &quot;학습 완료&quot;로
        바뀝니다. 글자가 없는 스캔 문서는 실패로 표시되니 그때는 글자가 든 파일로 다시 올려주세요.
      </Notice>
    </div>
  );
}
