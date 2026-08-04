/** 공통 질문 = 저장 답변. 키는 api-contracts.md §9-1 과 동일하다. */
export interface Faq {
  id: string;
  question: string;
  answer: string;
  /** 답변 아래에 링크로 붙는 문서 제목들. */
  links: string[];
  followUpFaqIds: string[];
  /**
   * 버튼 노출 여부일 뿐이다. false 여도 방문자가 비슷한 내용을 직접 입력하면 이 답변이 쓰인다.
   * 끄는 것과 지우는 것은 다르다.
   */
  shown: boolean;
  sortOrder: number;
  hitCount: number;
}

export interface FaqSaveInput {
  question: string;
  answer: string;
  links: string[];
  followUpFaqIds: string[];
  shown: boolean;
}
