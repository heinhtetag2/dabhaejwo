import type { Language } from "@/shared/lib/language";

/**
 * 랜딩 화면(v1·v2 공통) 텍스트.
 *
 * <p>두 뷰가 지금은 완전한 복제본이라 콘텐츠도 한 곳에 둔다 — 갈라지면 그때 각자
 * 갖는다. `landing-view.tsx`/`landing-view-v2.tsx` 참조.
 */
export interface LandingText {
  heroBadge: string;
  heroTitle: [string, string, string];
  heroSubtitle: string;
  heroCtaPrimary: string;
  heroCtaSecondary: string;
  heroCtaNote: string;
  heroCodeLabel: string;
  heroCodeNote: string;
  stepsEyebrow: string;
  stepsTitle: string;
  stepsDescription: string;
  steps: { title: string; body: string }[];
  benefitsEyebrow: string;
  benefitsTitle: string;
  benefitsDescription: string;
  benefits: { title: string; body: string }[];
  faqEyebrow: string;
  faqTitle: string;
  faqs: { q: string; a: string }[];
  closingTitle: string;
  closingSubtitle: string;
  closingCta: string;
}

export const LANDING_TEXT: Record<Language, LandingText> = {
  en: {
    heroBadge: "The chatbot that lives on your site",
    heroTitle: ["Add one line,", "and a chatbot trained on your site", "answers visitor questions."],
    heroSubtitle:
      "Just give us your site's address and we'll read and learn from your pages. Questions it can't answer are collected for you — answer one once, and the chatbot handles it from then on.",
    heroCtaPrimary: "Start free for 14 days",
    heroCtaSecondary: "See pricing",
    heroCtaNote: "No card required. You won't be charged during the trial.",
    heroCodeLabel: "This is the entire install",
    heroCodeNote:
      "When you sign up, this snippet is generated with your key. Even without a developer, we walk you through Cafe24, Imweb, and WordPress right in the dashboard.",
    stepsEyebrow: "3 steps",
    stepsTitle: "Tell us your address, paste the code, done",
    stepsDescription: "Sign up, and your chatbot is live on your site within 30 minutes.",
    steps: [
      {
        title: "Give us your site's address",
        body: "We'll find your subpages and list them for you. You choose which ones to train on.",
      },
      {
        title: "Wait for training to finish",
        body: "It only takes a few minutes. You can even try questions in the preview while it trains.",
      },
      {
        title: "Paste the code",
        body: "Just one line of script. Once a real call comes in, it's automatically marked as installed.",
      },
    ],
    benefitsEyebrow: "Results",
    benefitsTitle: "Fewer inquiries, and nothing missed",
    benefitsDescription:
      "Questions the chatbot can't answer aren't lost — they pile up in a list. Answer one once, and the chatbot takes over from there.",
    benefits: [
      {
        title: "Stop fielding the same questions",
        body: "Shipping, returns, store hours — the chatbot answers the questions that come in every day, first.",
      },
      {
        title: "It answers at night, too",
        body: "Even during busy evenings and weekends, visitors don't have to wait.",
      },
      {
        title: "No missed question goes unnoticed",
        body: "Unanswered questions are collected for you. Answer once, and the chatbot handles it from then on.",
      },
    ],
    faqEyebrow: "FAQ",
    faqTitle: "Frequently asked questions",
    faqs: [
      {
        q: "Can I install it without a developer?",
        a: "Yes. For Cafe24, Imweb, WordPress, and Gnuboard, we walk you through it step by step right in the dashboard. If your site is custom-built, you can copy a link with the code and install instructions to send to your developer.",
      },
      {
        q: "What happens if I go over my limit?",
        a: "The chatbot stops answering and shows a notice instead. You're never charged overage fees. We'll let you know once you hit 80% of your limit.",
      },
      {
        q: "Do saved answers count against my limit?",
        a: "No. Pre-written answers skip the AI entirely, so they show up instantly and don't count as conversations.",
      },
      {
        q: "Is it a problem if the install key is exposed?",
        a: "It's fine. Our server only allows it to run on addresses you've registered. It won't work if pasted onto another site.",
      },
      {
        q: "What happens to my data if I cancel?",
        a: "After cancellation, we keep a 30-day grace period before deleting your training data. If you restart within that window, everything picks up right where it left off.",
      },
    ],
    closingTitle: "Get started with just your site's address",
    closingSubtitle: "No card required. You won't be charged for 14 days.",
    closingCta: "Start free for 14 days",
  },
  ko: {
    heroBadge: "홈페이지에 붙이는 챗봇",
    heroTitle: ["한 줄만 붙이면,", "우리 사이트를 학습한 챗봇이", "방문자 질문에 답합니다."],
    heroSubtitle:
      "사이트 주소만 알려주시면 페이지를 읽어 학습합니다. 답하지 못한 질문은 모아서 보여드리고, 답을 한 번 달아두면 다음부터는 챗봇이 대신 답합니다.",
    heroCtaPrimary: "14일 무료로 시작하기",
    heroCtaSecondary: "요금제 보기",
    heroCtaNote: "카드 등록이 필요 없습니다. 체험 기간에는 요금이 청구되지 않습니다.",
    heroCodeLabel: "설치는 이게 전부입니다",
    heroCodeNote:
      "가입하면 이 코드가 키와 함께 만들어집니다. 개발자가 없어도 붙일 수 있도록 카페24·아임웹·워드프레스 안내를 화면에서 드립니다.",
    stepsEyebrow: "3단계",
    stepsTitle: "주소를 알려주고, 코드를 붙이면 끝입니다",
    stepsDescription: "가입하고 30분이면 챗봇이 사이트에 떠 있습니다.",
    steps: [
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
    ],
    benefitsEyebrow: "효과",
    benefitsTitle: "문의를 줄이고, 놓친 질문은 남깁니다",
    benefitsDescription:
      "답하지 못한 질문은 사라지지 않고 목록에 쌓입니다. 답을 한 번 달아두면 다음부터는 챗봇이 대신 답합니다.",
    benefits: [
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
    ],
    faqEyebrow: "FAQ",
    faqTitle: "자주 묻는 질문",
    faqs: [
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
    ],
    closingTitle: "사이트 주소 하나로 시작합니다",
    closingSubtitle: "카드 등록이 필요 없습니다. 14일 동안 요금이 청구되지 않습니다.",
    closingCta: "14일 무료로 시작하기",
  },
};
