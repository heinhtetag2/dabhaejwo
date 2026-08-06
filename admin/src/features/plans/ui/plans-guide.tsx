"use client";

import { GuideButton, GuideList, GuideSection, GuideWarning } from "@/shared/ui/guide-button";

/**
 * 요금제 화면 사용 가이드.
 *
 * <p>화면에 없는 것을 적지 않는다 — 가이드가 실제와 어긋나면 화면보다 가이드를 먼저
 * 의심하게 되고, 그때부터 아무도 읽지 않는다.
 */
export function PlansGuide() {
  return (
    <GuideButton title="요금제 — 사용 가이드">
      <GuideSection heading="이 화면이 정하는 것">
        <p>
          업체가 <b>얼마를 내고 무엇을 얼마나 쓸 수 있는지</b>를 정합니다. 여기서 바꾼 한도는
          그 요금제를 쓰는 <b>모든 업체</b>에 즉시 적용됩니다.
        </p>
      </GuideSection>

      <GuideSection heading="한도의 뜻">
        <GuideList
          items={[
            <>
              <b>월 대화</b> — 방문자가 위젯 패널을 연 횟수입니다. 질문 수가 아닙니다. 열어보고
              안 물어본 방문자도 한 건으로 셉니다
            </>,
            <>
              <b>문서</b> — 학습시킬 수 있는 파일 수입니다. 지운 문서는 세지 않습니다
            </>,
            <>
              <b>월 요금</b> — 원 단위 정수입니다. 협의가(기업)는 0 으로 두고 개별 계약으로
              처리합니다
            </>,
          ]}
        />
      </GuideSection>

      <GuideSection heading="판매 중단은 삭제가 아닙니다">
        <p>
          요금제는 지울 수 없습니다. 쓰고 있는 업체가 남아 있기 때문입니다. 신규 가입만 막으려면{" "}
          <b>판매 중단</b>으로 내리면 됩니다 — 기존 업체는 그대로 씁니다.
        </p>
      </GuideSection>

      <GuideSection heading="한도를 넘겼을 때">
        <p>넘긴 업체의 챗봇을 어떻게 할지 정합니다. 기본은 멈추고 안내하는 쪽입니다.</p>
        <GuideList
          items={[
            <>
              <b>멈추고 안내</b> — 방문자에게 안내 문구만 나갑니다. 원가가 더 나가지 않습니다
            </>,
            <>
              <b>초과분 과금</b> — 계속 답합니다. <b>결제 연동 전에는 실제로 청구되지 않습니다</b>
            </>,
            <>
              <b>알림만</b> — 계속 답하고 알림만 보냅니다. 원가는 그대로 나갑니다
            </>,
          ]}
        />
      </GuideSection>

      <GuideWarning>
        한도를 낮추면 이미 그 이상 쓰고 있던 업체가 <b>즉시 한도 초과 상태</b>가 됩니다.
        낮추기 전에 수익성 화면에서 실제 사용량을 먼저 보세요.
      </GuideWarning>
    </GuideButton>
  );
}
