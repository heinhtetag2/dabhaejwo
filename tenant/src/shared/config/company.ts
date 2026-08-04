/**
 * 사업자 정보. 전자상거래법상 푸터에 표기해야 하는 항목이다.
 *
 * TODO(stub): 실제 값이 아직 없다. 정식 공개 전에 반드시 채워야 하며
 * docs/IMPROVEMENTS.md 에 P0 으로 등록되어 있다.
 * 값을 지어내지 않고 빈 문자열로 두었다 — 화면은 빈 항목을 표시하지 않는다.
 */
export const COMPANY = {
  name: "답해줘",
  /** 상호 (법인명) */
  legalName: "",
  /** 대표자 */
  representative: "",
  /** 사업자등록번호 */
  businessNumber: "",
  /** 통신판매업 신고번호 */
  mailOrderNumber: "",
  address: "",
  /** 유료 전환·문의 대표 채널 */
  contactEmail: "support@dabhaejwo.com",
  contactPhone: "",
} as const;

/** 표기할 값이 있는 항목만 추린다. 빈 항목을 "—" 로 채우면 없는 정보가 있는 것처럼 보인다. */
export function companyFacts(): Array<{ label: string; value: string }> {
  return [
    { label: "상호", value: COMPANY.legalName },
    { label: "대표자", value: COMPANY.representative },
    { label: "사업자등록번호", value: COMPANY.businessNumber },
    { label: "통신판매업신고", value: COMPANY.mailOrderNumber },
    { label: "주소", value: COMPANY.address },
  ].filter((item) => item.value.length > 0);
}
