import { z } from "zod";

/** 키는 api-contracts.md 의 JSON 키와 동일하다. 변환 레이어를 만들지 않는다. */

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

export const otpFormSchema = z.object({
  code: z.string().regex(/^[0-9]{6}$/, "6자리 숫자를 입력하세요"),
});

export const forgotFormSchema = z.object({
  email: z.email("이메일 형식이 아닙니다"),
});

export const resetFormSchema = z
  .object({
    email: z.email("이메일 형식이 아닙니다"),
    temporaryPassword: z.string().min(1, "메일로 받은 임시 비밀번호를 입력하세요"),
    newPassword: z.string().min(8, "비밀번호는 8자 이상이어야 합니다"),
    // 확인 재입력은 화면에서 맞춘다. 서버에 두 값을 보내면 다를 때 무엇이 맞는지 정할 근거가 없다.
    confirmPassword: z.string(),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    path: ["confirmPassword"],
    message: "비밀번호가 서로 다릅니다",
  });

export const invitePreviewSchema = z.object({
  tenantName: z.string(),
  email: z.string(),
  name: z.string().nullable(),
  role: memberRoleSchema,
});

export const inviteAcceptFormSchema = z
  .object({
    password: z.string().min(8, "비밀번호는 8자 이상이어야 합니다"),
    confirmPassword: z.string(),
  })
  .refine((values) => values.password === values.confirmPassword, {
    path: ["confirmPassword"],
    message: "비밀번호가 서로 다릅니다",
  });

export const loginResultSchema = z.object({
  accessToken: z.string(),
  refreshToken: z.string(),
  member: memberSchema,
});

/** 로그인 폼. 서버도 같은 규칙으로 검증한다 — 여기 검증은 UX 용이다. */
export const loginFormSchema = z.object({
  email: z.email("이메일 형식이 아닙니다"),
  password: z.string().min(1, "비밀번호를 입력하세요"),
});

export type LoginFormValues = z.infer<typeof loginFormSchema>;
export type OtpChallenge = z.infer<typeof otpChallengeSchema>;
export type OtpFormValues = z.infer<typeof otpFormSchema>;
export type ForgotFormValues = z.infer<typeof forgotFormSchema>;
export type ResetFormValues = z.infer<typeof resetFormSchema>;
export type InvitePreview = z.infer<typeof invitePreviewSchema>;
export type InviteAcceptFormValues = z.infer<typeof inviteAcceptFormSchema>;
