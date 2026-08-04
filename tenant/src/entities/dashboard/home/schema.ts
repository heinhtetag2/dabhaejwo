import { z } from "zod";

export const homeSummarySchema = z.object({
  todayConvCount: z.number(),
  todayConvDelta: z.number(),
  answerSuccessPercent: z.number().nullable(),
  answerSuccessPercentLastWeek: z.number().nullable(),
  openGapCount: z.number(),
  todayLeadCount: z.number(),
  weekLeadCount: z.number(),
  avgResponseMs: z.number().nullable(),
  knowledge: z.object({
    documentCount: z.number(),
    indexedCount: z.number(),
    processingCount: z.number(),
    failedCount: z.number(),
  }),
  topQuestions: z.array(z.object({ question: z.string(), askCount: z.number() })),
});
