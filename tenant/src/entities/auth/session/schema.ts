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
