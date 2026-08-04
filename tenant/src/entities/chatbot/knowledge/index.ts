export { useKnowledgeSourcesQuery, useKnowledgeDocumentsQuery, knowledgeKeys } from "./query";
export type { DocumentQuery } from "./query";
export {
  useChangeAutoRefresh,
  useChangeExcluded,
  useDeleteDocument,
  useRecrawlSource,
  useRetryFailed,
  useUploadDocument,
} from "./mutation";

/** 서버 화이트리스트와 같은 목록. 어긋나면 화면이 고른 파일을 서버가 거절한다. */
export const ALLOWED_UPLOAD_EXTENSIONS = [".pdf", ".docx", ".xlsx", ".txt", ".md", ".csv"];
export const MAX_UPLOAD_MB = 20;
export {
  DOCUMENT_STATUS_LABEL,
  SOURCE_TYPE_LABEL,
  describeError,
} from "./types";
export type {
  DocumentStatus,
  KnowledgeDocument,
  KnowledgeSource,
  SourceType,
} from "./types";
