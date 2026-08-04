import { z } from "zod";

export const sourceTypeSchema = z.enum(["WEBSITE", "FILE", "MANUAL"]);
export const documentStatusSchema = z.enum([
  "PENDING",
  "PROCESSING",
  "INDEXED",
  "FAILED",
  "EXCLUDED",
]);

export const knowledgeSourceSchema = z.object({
  id: z.string(),
  type: sourceTypeSchema,
  origin: z.string(),
  autoRefresh: z.boolean(),
  lastCrawledAt: z.string().nullable(),
  documentCount: z.number(),
});

export const knowledgeSourceListSchema = z.array(knowledgeSourceSchema);

export const knowledgeDocumentSchema = z.object({
  id: z.string(),
  sourceId: z.string(),
  title: z.string(),
  path: z.string().nullable(),
  status: documentStatusSchema,
  errorCode: z.string().nullable(),
  chunkCount: z.number(),
  sizeBytes: z.number().nullable(),
  indexedAt: z.string().nullable(),
  storageKey: z.string().nullable(),
  originalFilename: z.string().nullable(),
});

export const knowledgeDocumentPageSchema = z.object({
  content: z.array(knowledgeDocumentSchema),
  page: z.object({
    number: z.number(),
    size: z.number(),
    totalElements: z.number(),
    totalPages: z.number(),
  }),
});
