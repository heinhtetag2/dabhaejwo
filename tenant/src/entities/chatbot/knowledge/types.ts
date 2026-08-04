/** 지식 소스·문서. 키는 api-contracts.md §9-2 와 동일하다. */

export type SourceType = "WEBSITE" | "FILE" | "MANUAL";

/**
 * EXCLUDED 는 업체가 뺀 것이라 실패가 아니다.
 * 요금제 한도에도, 홈의 3분류에도 들어가지 않는다.
 */
export type DocumentStatus = "PENDING" | "PROCESSING" | "INDEXED" | "FAILED" | "EXCLUDED";

export interface KnowledgeSource {
  id: string;
  type: SourceType;
  origin: string;
  autoRefresh: boolean;
  lastCrawledAt: string | null;
  documentCount: number;
}

export interface KnowledgeDocument {
  id: string;
  sourceId: string;
  title: string;
  path: string | null;
  status: DocumentStatus;
  /** 운영자용 원문 코드. 한글 설명은 프론트가 매핑한다. */
  errorCode: string | null;
  chunkCount: number;
  sizeBytes: number | null;
  indexedAt: string | null;
  /** 업로드한 원본이 있는 문서만 값이 있다. 웹페이지 문서는 null. */
  storageKey: string | null;
  originalFilename: string | null;
}

/** 업체는 크롤링·임베딩·청크라는 말을 모른다 (tenant-plan.md §1.3). */
export const SOURCE_TYPE_LABEL: Record<SourceType, string> = {
  WEBSITE: "웹페이지",
  FILE: "파일",
  MANUAL: "직접 입력",
};

export const DOCUMENT_STATUS_LABEL: Record<DocumentStatus, string> = {
  PENDING: "대기 중",
  PROCESSING: "처리 중",
  INDEXED: "학습 완료",
  FAILED: "실패",
  EXCLUDED: "제외됨",
};

/**
 * 실패 코드를 사람 말로. 모르는 코드는 원문을 그대로 보여준다 —
 * "알 수 없는 오류"로 뭉개면 업체가 문의할 때 단서가 사라진다.
 */
const ERROR_LABEL: Record<string, string> = {
  fetch_timeout: "페이지를 여는 데 너무 오래 걸렸습니다",
  pdf_parse_timeout: "PDF에서 글자를 뽑는 데 실패했습니다",
  pdf_no_text: "이 파일에서 글자를 찾지 못했습니다. 스캔한 문서로 보입니다",
  robots_blocked: "사이트가 수집을 허용하지 않습니다",
  too_large: "파일이 너무 큽니다",
};

export function describeError(code: string | null): string | null {
  if (!code) {
    return null;
  }
  return ERROR_LABEL[code] ?? code;
}
