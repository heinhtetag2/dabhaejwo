"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useState, type FormEvent } from "react";

import { api } from "@/shared/api/http-client";
import { Button } from "@/shared/common/button";
import { Field, TextInput } from "@/shared/common/field";
import { ROUTES } from "@/shared/config/routes";
import { errorMessage } from "@/shared/lib/error-message";

/**
 * 운영자 비밀번호 찾기.
 *
 * 이메일 입력 → 임시 비밀번호 발송 → 임시 비밀번호로 본인 확인 → 새 비밀번호 설정.
 *
 * 단계를 URL 로 남긴다(`?step=reset`) — 메일을 확인하러 갔다가 돌아오는 흐름이라
 * 상태를 메모리에만 두면 탭을 옮기는 순간 처음으로 돌아간다.
 */
export function PasswordResetView() {
  const params = useSearchParams();
  const [step, setStep] = useState<"forgot" | "reset">(
    params.get("step") === "reset" ? "reset" : "forgot",
  );
  const [email, setEmail] = useState(params.get("email") ?? "");

  return (
    <main className="flex min-h-dvh items-center justify-center bg-paper px-4">
      <div className="w-full max-w-[380px]">
        <div className="mb-6 flex items-center gap-2.5">
          <span className="grid size-[26px] place-items-center rounded-md bg-seal font-mono text-[11px] font-semibold text-white">
            OPS
          </span>
          <h1 className="font-semibold tracking-[-0.01em]">비밀번호 찾기</h1>
        </div>

        {step === "forgot" ? (
          <ForgotStep
            email={email}
            onEmailChange={setEmail}
            onSent={() => setStep("reset")}
          />
        ) : (
          <ResetStep email={email} onEmailChange={setEmail} />
        )}

        <p className="mt-4 text-center text-[12px]">
          <Link href={ROUTES.login} className="text-slate underline hover:text-ink">
            로그인으로 돌아가기
          </Link>
        </p>
      </div>
    </main>
  );
}

function ForgotStep({
  email,
  onEmailChange,
  onSent,
}: {
  email: string;
  onEmailChange: (value: string) => void;
  onSent: () => void;
}) {
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setPending(true);
    setError(null);
    try {
      await api("/api/auth/ops/password/forgot", { method: "POST", body: { email } });
      onSent();
    } catch (caught) {
      setError(errorMessage(caught, "메일을 보내지 못했습니다"));
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="rounded-card border border-line bg-card px-6 py-6">
      <Field label="이메일" hint="등록된 주소라면 임시 비밀번호가 발송됩니다.">
        {(id) => (
          <TextInput
            id={id}
            type="email"
            value={email}
            onChange={(e) => onEmailChange(e.target.value)}
            autoComplete="username"
            required
          />
        )}
      </Field>

      {error ? (
        <p role="alert" className="mb-3 text-[12.5px] text-brick">
          {error}
        </p>
      ) : null}

      <Button type="submit" variant="primary" className="w-full justify-center" disabled={pending}>
        {pending ? "보내는 중" : "임시 비밀번호 받기"}
      </Button>
    </form>
  );
}

function ResetStep({
  email,
  onEmailChange,
}: {
  email: string;
  onEmailChange: (value: string) => void;
}) {
  const router = useRouter();
  const [temporaryPassword, setTemporaryPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    // 확인 재입력은 화면에서 맞춘다. 서버에 두 값을 보내면 다를 때 무엇이 맞는지 정할 근거가 없다.
    if (newPassword !== confirmPassword) {
      setError("비밀번호가 서로 다릅니다");
      return;
    }
    setPending(true);
    setError(null);
    try {
      await api("/api/auth/ops/password/reset", {
        method: "POST",
        body: { email, temporaryPassword, newPassword },
      });
      router.replace(`${ROUTES.login}?reset=done`);
    } catch (caught) {
      setError(errorMessage(caught, "비밀번호를 바꾸지 못했습니다"));
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="rounded-card border border-line bg-card px-6 py-6">
      <Field label="이메일">
        {(id) => (
          <TextInput
            id={id}
            type="email"
            value={email}
            onChange={(e) => onEmailChange(e.target.value)}
            autoComplete="username"
            required
          />
        )}
      </Field>

      <Field label="임시 비밀번호" hint="메일에 적힌 값을 그대로 입력해 주세요.">
        {(id) => (
          <TextInput
            id={id}
            value={temporaryPassword}
            onChange={(e) => setTemporaryPassword(e.target.value)}
            autoComplete="one-time-code"
            className="font-mono"
            required
          />
        )}
      </Field>

      <Field label="새 비밀번호" hint="8자 이상">
        {(id) => (
          <TextInput
            id={id}
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            autoComplete="new-password"
            minLength={8}
            required
          />
        )}
      </Field>

      <Field label="새 비밀번호 확인">
        {(id) => (
          <TextInput
            id={id}
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            autoComplete="new-password"
            required
          />
        )}
      </Field>

      {error ? (
        <p role="alert" className="mb-3 text-[12.5px] text-brick">
          {error}
        </p>
      ) : null}

      <Button type="submit" variant="primary" className="w-full justify-center" disabled={pending}>
        {pending ? "바꾸는 중" : "비밀번호 바꾸기"}
      </Button>
    </form>
  );
}
