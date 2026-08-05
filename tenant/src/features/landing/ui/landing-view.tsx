import { LinkButton } from "@/shared/common/button";
import { ROUTES } from "@/shared/config/routes";
import { PublicCard, PublicSection } from "@/shared/ui/public-section";

/**
 * 서비스 안내.
 *
 * <p>목적은 설명이 아니라 가입이다 (tenant-public-plan.md §1.3).
 * 기능을 나열하지 않고, 이 제품의 가장 큰 강점인 "붙이기 쉽다"를 코드로 보여준다.
 *
 * <p>Server Component 다. 클라이언트 상태가 없다.
 */
export function LandingView() {
  return (
    <>
      <Hero />

      <PublicSection
        tone="fill"
        eyebrow="3단계"
        title="주소를 알려주고, 코드를 붙이면 끝입니다"
        description="가입하고 30분이면 챗봇이 사이트에 떠 있습니다."
      >
        <ol className="grid gap-4 sm:grid-cols-3">
          {STEPS.map((step, index) => (
            <PublicCard as="li" key={step.title} className="bg-card">
              <span
                aria-hidden
                className="grid size-8 place-items-center rounded-full bg-ink text-[13px] font-bold text-mark"
              >
                {index + 1}
              </span>
              <h3 className="mt-5 text-[17px] font-bold tracking-[-0.02em]">{step.title}</h3>
              <p className="mt-2.5 text-[14.5px] leading-[1.7] text-slate">{step.body}</p>
            </PublicCard>
          ))}
        </ol>
      </PublicSection>

      <PublicSection
        eyebrow="효과"
        title="문의를 줄이고, 놓친 질문은 남깁니다"
        description="답하지 못한 질문은 사라지지 않고 목록에 쌓입니다. 답을 한 번 달아두면 다음부터는 챗봇이 대신 답합니다."
      >
        <div className="grid gap-4 sm:grid-cols-3">
          {BENEFITS.map((benefit) => (
            <PublicCard key={benefit.title} className="bg-fill">
              <h3 className="text-[17px] font-bold tracking-[-0.02em] text-balance">
                {benefit.title}
              </h3>
              <p className="mt-2.5 text-[14.5px] leading-[1.7] text-slate">{benefit.body}</p>
            </PublicCard>
          ))}
        </div>
      </PublicSection>

      <PublicSection tone="fill" eyebrow="FAQ" title="자주 묻는 질문">
        <dl className="grid gap-3">
          {FAQS.map((faq) => (
            <div key={faq.q} className="rounded-block bg-card px-6 py-6 sm:px-7">
              <dt className="text-[15.5px] font-bold tracking-[-0.02em]">{faq.q}</dt>
              <dd className="mt-2.5 text-[14.5px] leading-[1.75] text-slate">{faq.a}</dd>
            </div>
          ))}
        </dl>
      </PublicSection>

      <section className="mx-auto max-w-[1080px] px-5 pt-4 pb-24 sm:pb-28">
        <div className="rounded-panel bg-ink px-6 py-16 text-center sm:px-10 sm:py-20">
          <h2 className="text-[26px] leading-[1.35] font-bold tracking-[-0.035em] text-balance text-white sm:text-[34px]">
            사이트 주소 하나로 시작합니다
          </h2>
          <p className="mt-4 text-[15.5px] leading-[1.7] text-slate-2">
            카드 등록이 필요 없습니다. 14일 동안 요금이 청구되지 않습니다.
          </p>
          <LinkButton
            href={ROUTES.signup}
            size="lg"
            className="mt-8 border-mark bg-mark text-ink hover:brightness-105"
          >
            14일 무료로 시작하기
          </LinkButton>
        </div>
      </section>
    </>
  );
}

function Hero() {
  return (
    <section className="mx-auto max-w-[1080px] px-5 pt-16 pb-20 sm:pt-24 sm:pb-24 lg:pt-28">
      <div className="grid items-center gap-14 lg:grid-cols-[1.05fr_1fr] lg:gap-16">
        <div>
          <p className="inline-flex items-center gap-1.5 rounded-full bg-mark-soft px-3 py-1.5 text-[13px] font-semibold text-ink-2">
            홈페이지에 붙이는 챗봇
          </p>

          <h1 className="mt-5 text-[36px] leading-[1.28] font-bold tracking-[-0.045em] text-balance sm:text-[46px] lg:text-[52px]">
            한 줄만 붙이면,
            <br />
            우리 사이트를 학습한 챗봇이
            <br />
            방문자 질문에 답합니다.
          </h1>

          <p className="mt-6 max-w-[500px] text-[16px] leading-[1.75] text-slate sm:text-[17px]">
            사이트 주소만 알려주시면 페이지를 읽어 학습합니다. 답하지 못한 질문은 모아서
            보여드리고, 답을 한 번 달아두면 다음부터는 챗봇이 대신 답합니다.
          </p>

          <div className="mt-9 flex flex-wrap items-center gap-2.5">
            <LinkButton href={ROUTES.signup} variant="primary" size="lg">
              14일 무료로 시작하기
            </LinkButton>
            <LinkButton href={ROUTES.pricing} size="lg" className="border-edge bg-fill hover:bg-line-2">
              요금제 보기
            </LinkButton>
          </div>
          <p className="mt-4 text-[13.5px] text-slate-2">
            카드 등록이 필요 없습니다. 체험 기간에는 요금이 청구되지 않습니다.
          </p>
        </div>

        <div className="rounded-panel bg-fill p-6 sm:p-7">
          <p className="text-[13px] font-semibold text-slate-2">설치는 이게 전부입니다</p>
          <pre className="mt-4 overflow-x-auto rounded-block bg-ink px-5 py-5 font-mono text-[12.5px] leading-[1.8] text-code">
{`<script>
  window.dabhaejwo = { key: "pk_live_..." };
</script>
<script src="https://cdn.dabhaejwo.com/w.js" async></script>`}
          </pre>
          <p className="mt-4 text-[13.5px] leading-relaxed text-slate">
            가입하면 이 코드가 키와 함께 만들어집니다. 개발자가 없어도 붙일 수 있도록
            카페24·아임웹·워드프레스 안내를 화면에서 드립니다.
          </p>
        </div>
      </div>
    </section>
  );
}

const STEPS = [
  {
    title: "사이트 주소를 알려주세요",
    body: "하위 페이지를 찾아 목록으로 보여드립니다. 학습할 페이지는 직접 고르시면 됩니다.",
  },
  {
    title: "학습을 기다립니다",
    body: "몇 분이면 끝납니다. 학습되는 동안에도 미리보기에서 질문해 볼 수 있습니다.",
  },
  {
    title: "코드를 붙입니다",
    body: "스크립트 한 줄이면 됩니다. 실제 호출이 들어오면 설치 완료로 자동 표시됩니다.",
  },
];

const BENEFITS = [
  {
    title: "같은 질문을 반복해 받지 않습니다",
    body: "배송·반품·매장 위치처럼 매일 오는 질문을 챗봇이 먼저 답합니다.",
  },
  {
    title: "밤에도 답합니다",
    body: "문의가 몰리는 저녁과 주말에도 방문자를 기다리게 하지 않습니다.",
  },
  {
    title: "놓친 질문이 쌓이지 않습니다",
    body: "답하지 못한 질문을 모아서 보여드립니다. 한 번 답을 달면 다음부터는 챗봇이 답합니다.",
  },
];

const FAQS = [
  {
    q: "개발자가 없어도 설치할 수 있나요?",
    a: "네. 카페24·아임웹·워드프레스·그누보드는 화면에서 단계별로 안내합니다. 직접 만든 사이트라면 코드와 설치 안내를 담은 링크를 복사해 개발자에게 전달할 수 있습니다.",
  },
  {
    q: "한도를 넘으면 어떻게 되나요?",
    a: "챗봇이 답변을 멈추고 안내 문구만 표시합니다. 초과 요금은 청구되지 않습니다. 한도의 80%에 닿으면 미리 알려드립니다.",
  },
  {
    q: "공통 질문은 한도를 쓰나요?",
    a: "쓰지 않습니다. 미리 등록해 둔 답변은 AI를 거치지 않고 그대로 나가므로 즉시 표시되고 대화 수에도 잡히지 않습니다.",
  },
  {
    q: "설치 코드의 키가 노출돼도 괜찮나요?",
    a: "괜찮습니다. 등록한 주소에서만 동작하도록 서버가 확인합니다. 다른 사이트에 붙여도 작동하지 않습니다.",
  },
  {
    q: "해지하면 데이터는 어떻게 되나요?",
    a: "해지 후 30일 유예를 두고 학습 데이터를 삭제합니다. 유예 기간 안에 다시 시작하면 그대로 이어집니다.",
  },
];
