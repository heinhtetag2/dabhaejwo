import { z } from "zod";

export const faqSchema = z.object({
  id: z.string(),
  question: z.string(),
  answer: z.string(),
  links: z.array(z.string()),
  followUpFaqIds: z.array(z.string()),
  shown: z.boolean(),
  sortOrder: z.number(),
  hitCount: z.number(),
});

export const faqListSchema = z.array(faqSchema);

/** 편집 폼. 서버도 같은 규칙으로 검증한다 — 여기 검증은 UX 용이다. */
export const faqFormSchema = z.object({
  question: z.string().trim().min(1, "질문을 입력하세요").max(120, "너무 깁니다"),
  answer: z.string().trim().min(1, "답변을 입력하세요").max(4000, "너무 깁니다"),
  /** 화면에서는 쉼표로 입력받고 저장할 때 배열로 바꾼다. */
  linksText: z.string(),
  shown: z.boolean(),
});

export type FaqFormValues = z.infer<typeof faqFormSchema>;
