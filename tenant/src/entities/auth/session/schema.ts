import { z } from "zod";

import type { Language } from "@/shared/lib/language";

/** 키는 api-contracts.md 의 JSON 키와 동일하다. 변환 레이어를 만들지 않는다. */

/** 폼 검증 메시지 — 언어별. 스키마가 함수인 이유는 이 값을 렌더 시점에 골라야 해서다. */
const VALIDATION_TEXT: Record<
  Language,
  {
    invalidEmail: string;
    passwordRequired: string;
    passwordMinLength: string;
    passwordMismatch: string;
    otpFormat: string;
    temporaryPasswordRequired: string;
  }
> = {
  en: {
    invalidEmail: "Not a valid email address",
    passwordRequired: "Enter your password",
    passwordMinLength: "Password must be at least 8 characters",
    passwordMismatch: "Passwords don't match",
    otpFormat: "Enter the 6-digit code",
    temporaryPasswordRequired: "Enter the temporary password from your email",
  },
  ko: {
    invalidEmail: "이메일 형식이 아닙니다",
    passwordRequired: "비밀번호를 입력하세요",
    passwordMinLength: "비밀번호는 8자 이상이어야 합니다",
    passwordMismatch: "비밀번호가 서로 다릅니다",
    otpFormat: "6자리 숫자를 입력하세요",
    temporaryPasswordRequired: "메일로 받은 임시 비밀번호를 입력하세요",
  },
};

export const memberRoleSchema = z.enum(["OWNER", "EDITOR", "VIEWER"]);
export const inviteStateSchema = z.enum(["PENDING", "ACCEPTED"]);
export const tenantStatusSchema = z.enum(["TRIAL", "ACTIVE", "SUSPENDED", "CHURNED"]);

export const memberSchema = z.object({
  id: z.string(),
  name: z.string().nullable(),
  email: z.string(),
  role: memberRoleSchema,
  inviteState: inviteStateSchema,
  /** 계정이 막혔을 때 연락할 수단. 초대할 때 함께 받는다. */
  phone: z.string().nullable(),
  lastSeenAt: z.string().nullable(),
});

export const appContextSchema = z.object({
  member: memberSchema.nullable(),
  tenant: z.object({
    id: z.string(),
    name: z.string(),
    primaryDomain: z.string(),
    publishableKey: z.string(),
    status: tenantStatusSchema,
    plan: z.object({ id: z.string(), name: z.string(), monthlyFee: z.number() }),
  }),
  usage: z.object({
    convCount: z.number(),
    convLimit: z.number(),
    docCount: z.number(),
    docLimit: z.number(),
  }),
  impersonation: z
    .object({ sessionId: z.string(), reason: z.string(), expiresAt: z.string() })
    .nullable(),
});

/**
 * 1단계 응답. <b>토큰이 없다</b> — 메일로 간 코드를 맞혀야 토큰이 나온다.
 */
export const otpChallengeSchema = z.object({
  challengeId: z.string(),
  maskedEmail: z.string(),
  ttlMinutes: z.number(),
});

export function otpFormSchema(language: Language) {
  const t = VALIDATION_TEXT[language];
  return z.object({
    code: z.string().regex(/^[0-9]{6}$/, t.otpFormat),
  });
}

export function forgotFormSchema(language: Language) {
  const t = VALIDATION_TEXT[language];
  return z.object({
    email: z.email(t.invalidEmail),
  });
}

export function resetFormSchema(language: Language) {
  const t = VALIDATION_TEXT[language];
  return z
    .object({
      email: z.email(t.invalidEmail),
      temporaryPassword: z.string().min(1, t.temporaryPasswordRequired),
      newPassword: z.string().min(8, t.passwordMinLength),
      // 확인 재입력은 화면에서 맞춘다. 서버에 두 값을 보내면 다를 때 무엇이 맞는지 정할 근거가 없다.
      confirmPassword: z.string(),
    })
    .refine((values) => values.newPassword === values.confirmPassword, {
      path: ["confirmPassword"],
      message: t.passwordMismatch,
    });
}

export const invitePreviewSchema = z.object({
  tenantName: z.string(),
  email: z.string(),
  name: z.string().nullable(),
  role: memberRoleSchema,
});

export function inviteAcceptFormSchema(language: Language) {
  const t = VALIDATION_TEXT[language];
  return z
    .object({
      password: z.string().min(8, t.passwordMinLength),
      confirmPassword: z.string(),
    })
    .refine((values) => values.password === values.confirmPassword, {
      path: ["confirmPassword"],
      message: t.passwordMismatch,
    });
}

export const loginResultSchema = z.object({
  accessToken: z.string(),
  refreshToken: z.string(),
  member: memberSchema,
});

/** 로그인 폼. 서버도 같은 규칙으로 검증한다 — 여기 검증은 UX 용이다. */
export function loginFormSchema(language: Language) {
  const t = VALIDATION_TEXT[language];
  return z.object({
    email: z.email(t.invalidEmail),
    password: z.string().min(1, t.passwordRequired),
  });
}

export type LoginFormValues = z.infer<ReturnType<typeof loginFormSchema>>;
export type OtpChallenge = z.infer<typeof otpChallengeSchema>;
export type OtpFormValues = z.infer<ReturnType<typeof otpFormSchema>>;
export type ForgotFormValues = z.infer<ReturnType<typeof forgotFormSchema>>;
export type ResetFormValues = z.infer<ReturnType<typeof resetFormSchema>>;
export type InvitePreview = z.infer<typeof invitePreviewSchema>;
export type InviteAcceptFormValues = z.infer<ReturnType<typeof inviteAcceptFormSchema>>;
