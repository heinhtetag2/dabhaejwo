"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";

import type { OpsLoginResponse } from "@/entities/operator";
import { ApiError, api } from "@/shared/api/http-client";
import { Button } from "@/shared/common/button";
import { Field, TextInput } from "@/shared/common/field";
import { ROUTES } from "@/shared/config/routes";
import { useAuthStore } from "@/shared/lib/auth-store";
import { errorMessage } from "@/shared/lib/error-message";

/** 1단계 응답. 토큰이 없다 — 메일로 간 코드를 맞혀야 나온다. */
interface OtpChallenge {
  challengeId: string;
  maskedEmail: string;
  ttlMinutes: number;
}

/**
 * 운영자 로그인.
 *
 * 두 단계다. 비밀번호가 맞으면 인증 코드가 메일로 가고, 그 코드를 맞혀야 들어온다.
 * 이 콘솔은 전 업체의 데이터를 볼 수 있어 업체 계정보다 위험이 크므로 건너뛰지 않는다.
 * 기획서 §8 의 SSO 는 아직이다 (CLAUDE.md Stub 목록).
 *
 * features/auth 예외 — 로그인은 CRUD 리소스가 아닌 use-case 라 상태를 여기 둔다
 * (fsd-rules). 응답 타입은 entities 에서 가져온다.
 */
export function LoginView() {
  const [challenge, setChallenge] = useState<OtpChallenge | null>(null);

  return (
    <Shell>
      {challenge ? (
        <OtpStep challenge={challenge} onBack={() => setChallenge(null)} />
      ) : (
        <PasswordStep onIssued={setChallenge} />
      )}
    </Shell>
  );
}

function Shell({ children }: { children: React.ReactNode }) {
  return (
    <main className="flex min-h-dvh items-center justify-center bg-paper px-4">
      <div className="w-full max-w-[380px]">
        <div className="mb-6 flex items-center gap-2.5">
          <span className="grid size-[26px] place-items-center rounded-md bg-seal font-mono text-[11px] font-semibold text-white">
            OPS
          </span>
          <div>
            <h1 className="font-semibold tracking-[-0.01em]">답해줘 운영</h1>
            <p className="font-mono text-[10.5px] tracking-[0.08em] text-slate-2">
              내부 운영 도구
            </p>
          </div>
        </div>

        {children}

        <p className="mt-4 text-center text-[11.5px] leading-relaxed text-slate-2">
          이 콘솔은 고객 데이터에 접근할 수 있습니다.
          <br />
          대리 접속과 대화 로그 열람은 사유와 함께 기록됩니다.
        </p>
      </div>
    </main>
  );
}

function PasswordStep({ onIssued }: { onIssued: (challenge: OtpChallenge) => void }) {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setPending(true);
    setError(null);
    try {
      onIssued(
        await api<OtpChallenge>("/api/auth/ops/login", {
          method: "POST",
          body: { email, password },
        }),
      );
    } catch (caught) {
      // 임시 비밀번호로 들어왔다. 재설정 화면으로 보낸다 —
      // 여기서 로그인시키면 메일로 보낸 임시값이 사실상 영구 비밀번호가 된다.
      if (caught instanceof ApiError && caught.code === "PASSWORD_CHANGE_REQUIRED") {
        router.push(`${ROUTES.forgotPassword}?step=reset&email=${encodeURIComponent(email)}`);
        return;
      }
      // 실패 사유를 구분해 보여주지 않는다 — 서버도 구분하지 않는다.
      // 어떤 이메일이 등록돼 있는지 알려주는 수단이 되기 때문이다.
      setError(errorMessage(caught, "이메일 또는 비밀번호가 올바르지 않습니다"));
    } finally {
      setPending(false);
    }
  }

  return (
    <>
      <form onSubmit={handleSubmit} className="rounded-card border border-line bg-card px-6 py-6">
        <Field label="이메일">
          {(id) => (
            <TextInput
              id={id}
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              autoComplete="username"
              required
            />
          )}
        </Field>

        <Field label="비밀번호">
          {(id) => (
            <TextInput
              id={id}
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
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
          {pending ? "인증 코드를 보내는 중" : "다음"}
        </Button>
      </form>

      <p className="mt-3.5 text-center text-[12px]">
        <Link href={ROUTES.forgotPassword} className="text-slate underline hover:text-ink">
          비밀번호를 잊으셨나요?
        </Link>
      </p>
    </>
  );
}

function OtpStep({ challenge, onBack }: { challenge: OtpChallenge; onBack: () => void }) {
  const router = useRouter();
  const signIn = useAuthStore((state) => state.signIn);

  const [code, setCode] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setPending(true);
    setError(null);
    try {
      const result = await api<OpsLoginResponse>("/api/auth/ops/login/otp", {
        method: "POST",
        body: { challengeId: challenge.challengeId, code },
      });
      signIn(
        { accessToken: result.accessToken, refreshToken: result.refreshToken },
        result.operator,
      );
      router.replace(ROUTES.today);
    } catch (caught) {
      // 여기는 사유를 그대로 보여준다 — 남은 시도 횟수를 알려줘야 사용자가 판단할 수 있고,
      // 이 단계는 이미 비밀번호를 통과한 뒤라 계정 존재 여부가 새지 않는다.
      setError(errorMessage(caught, "인증 코드가 올바르지 않습니다"));
    } finally {
      setPending(false);
    }
  }

  return (
    <>
      <form onSubmit={handleSubmit} className="rounded-card border border-line bg-card px-6 py-6">
        <p className="mb-4 text-[12.5px] leading-relaxed text-slate">
          <b className="font-medium text-ink">{challenge.maskedEmail}</b> 으로 6자리 코드를
          보냈습니다. {challenge.ttlMinutes}분 안에 입력해 주세요.
        </p>

        <Field label="인증 코드">
          {(id) => (
            <TextInput
              id={id}
              value={code}
              onChange={(e) => setCode(e.target.value)}
              inputMode="numeric"
              autoComplete="one-time-code"
              maxLength={6}
              placeholder="000000"
              autoFocus
              required
              className="tabular text-center text-[18px] tracking-[0.3em]"
            />
          )}
        </Field>

        {error ? (
          <p role="alert" className="mb-3 text-[12.5px] text-brick">
            {error}
          </p>
        ) : null}

        <Button type="submit" variant="primary" className="w-full justify-center" disabled={pending}>
          {pending ? "확인 중" : "로그인"}
        </Button>
      </form>

      <p className="mt-3.5 text-center text-[12px] text-slate-2">
        메일이 오지 않았나요?{" "}
        <button type="button" onClick={onBack} className="text-slate underline hover:text-ink">
          처음부터 다시
        </button>
      </p>
    </>
  );
}
