import { ChevronDown } from "lucide-react";

/**
 * pricing-view-v2 의 FAQ 아코디언(Figma node 57:2)에서 추출했다. landing-view-v2 도
 * 같은 디자인을 그대로 쓰기로 해(사용자 요청) 카피만 features 쪽에 두고 이 컴포넌트는
 * shared/ui 로 옮겼다 — fsd-rules: 둘 이상의 feature가 함께 쓰는 UI는 shared/ui.
 */
export function FaqAccordion({
  heading,
  faqs,
  className,
}: {
  heading: string;
  faqs: { q: string; a: string }[];
  className?: string;
}) {
  return (
    <section className={className ?? "bg-white py-16 sm:py-24"}>
      <div className="mx-auto max-w-[1160px] px-5">
        <div className="rounded-[28px] bg-black/[0.03] px-5 pt-16 pb-[70px] sm:px-10 sm:pt-20">
          <h2 className="text-center text-[32px] font-bold tracking-[-0.03em] text-black/85 sm:text-[52px] sm:tracking-[-1.5px]">
            {heading}
          </h2>

          <div className="mx-auto mt-11 flex max-w-[1080px] flex-col gap-4">
            {faqs.map((faq, index) => (
              <details key={faq.q} open={index === 0} className="group rounded-[22px] bg-white p-7">
                <summary className="flex cursor-pointer list-none items-center justify-between gap-5 [&::-webkit-details-marker]:hidden">
                  <span className="text-[18px] font-bold tracking-[-0.02em] text-black/60 sm:text-[20px]">
                    {faq.q}
                  </span>
                  <ChevronDown
                    aria-hidden
                    className="size-6 shrink-0 text-black/40 transition-transform sm:size-7 group-open:rotate-180"
                  />
                </summary>
                <p className="mt-5 text-[16px] leading-[1.45] tracking-[-0.01em] text-black/60 sm:text-[18px]">
                  {faq.a}
                </p>
              </details>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
