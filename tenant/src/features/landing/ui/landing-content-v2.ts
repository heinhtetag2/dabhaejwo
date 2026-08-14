import type { Language } from "@/shared/lib/language";

/**
 * 랜딩 리디자인(v2) 전용 텍스트 — v1 과 구조가 완전히 달라져 `landing-content.ts`
 * (v1·v2 공통 시절 파일)에서 갈라져 나왔다(그 파일의 "갈라지면 각자 갖는다" 예고대로).
 *
 * <p>Figma 참조(node 65:11820)는 채널톡 홈페이지다 — 실제 고객 사례(회사명·인물·
 * "AI 상담 해결률" %), 채널웍스·AI CoS·규칙 엔진·마케팅·전화·CRM·파트너처럼 우리에게
 * 없는 제품, 브랜드명을 입력하면 데모를 만들어주는 기능은 전부 없는 것을 있는 척하는
 * 셈이라 담지 않는다(workflow-rules). 대신 같은 시각 리듬(배지+캐러셀+큰 카드+탭)을
 * 유지하면서 내용은 답해줘가 실제로 가진 것으로 채웠다 — 자세한 매핑은 커밋 사유 참조.
 */
export interface LandingTextV2 {
  heroTitle: [string, string];
  heroSubtitle: string;
  heroCta: string;
  docsBadge: string;
  docsTitle: [string, string];
  docsSubtitle: string;
  docsCard: { badge: string; title: [string, string]; body: string };
  docsMiniCards: { title: string; body: string }[];
  strengthsTitle: string;
  strengthsSubtitle: string;
  strengths: { tab: string; stat: string; title: string; body: string }[];
  flywheelEyebrow: string;
  flywheelTitle: string;
  flywheelSubtitle: string;
  flywheelTabs: { label: string; title: string; body: string }[];
  flowEyebrow: string;
  flowTitle: string;
  flowSubtitle: string;
  flowLoop: string;
  nightEyebrow: string;
  nightTitle: string;
  nightBody: string;
  faqHeading: string;
  faqs: { q: string; a: string }[];
}

export const LANDING_TEXT_V2: Record<Language, LandingTextV2> = {
  en: {
    heroTitle: ["Add one line,", "and your chatbot answers questions"],
    heroSubtitle: "Give us your site's address — we'll learn your pages and use them as the chatbot's knowledge base.",
    heroCta: "Start free",
    docsBadge: "Better answers",
    docsTitle: ["The more documents you add,", "the better the answers get"],
    docsSubtitle: "Training and answering both happen inside dabhaejwo.",
    docsCard: {
      badge: "Accurate answers",
      title: ["Your documents meet the chatbot,", "and answers get sharper"],
      body: "The AI searches your uploaded documents for the parts relevant to a question, then answers from them.",
    },
    docsMiniCards: [
      { title: "One line of code, and you're connected", body: "Paste it just above </body> — that's it. The key is safe to expose, since it only works on domains you've registered." },
      { title: "Missed questions become saved answers", body: "Answer a missed question once, and it's instantly available from then on." },
    ],
    strengthsTitle: "Not a demo — these actually work",
    strengthsSubtitle: "See how install, training, pricing, branding, and security actually behave.",
    strengths: [
      { tab: "Who it's for", stat: "4 roles", title: "From store owners to marketers — everyone uses it differently", body: "A solo store owner installs it without a developer. CS refines saved answers. Marketers check leads and stats." },
      { tab: "Install", stat: "30 min", title: "One line of script is all it takes", body: "Sign up and the code is generated for you. Cafe24, Imweb, and WordPress get step-by-step guidance in the dashboard." },
      { tab: "Training", stat: "600 chars", title: "Documents are split and searched by chunk", body: "Pages are split into ~600-character chunks so the AI can find just the part relevant to a question." },
      { tab: "Pricing", stat: "14 days", title: "Overages don't raise your bill", body: "Try free for 14 days, no card required. Going over your limit pauses the bot instead of charging you more." },
      { tab: "Branding", stat: "3 sizes", title: "Match the chatbot to your brand", body: "Launcher size, colors, and logo can all be set to match your brand." },
      { tab: "Security", stat: "", title: "Only runs on addresses you register", body: "Even if the install key leaks, it won't work on a site you haven't registered." },
    ],
    flywheelEyebrow: "Saved-answer flywheel",
    flywheelTitle: "Gets smarter the more you use it, and looks like your brand",
    flywheelSubtitle: "Answer once and it goes out instantly from then on — logo and colors adjust to match your brand too.",
    flywheelTabs: [
      { label: "Saved answers", title: "Register a saved answer and it goes out instantly", body: "Add a frequent question to your saved answers and turn on exposure — visitors tap a button and get the saved answer immediately, without calling the AI." },
      { label: "Answer gaps", title: "Failed answers show up right in the conversation log", body: "Every visitor conversation is logged, and questions the chatbot couldn't answer get flagged. Find them in the log, then register an answer from the answer-gaps list." },
      { label: "Branding", title: "Match the logo and colors to your brand", body: "Set your logo, chatbot icon, bubble color, and greeting message right from Widget settings, and confirm the look in the live preview before saving." },
    ],
    flowEyebrow: "How it works",
    flowTitle: "The more it's used, the more saved answers pile up",
    flowSubtitle: "A visitor's question is answered by AI using your documents and tone settings — and a missed question becomes a saved answer once your team fills it in.",
    flowLoop: "Answers your team registers get added as saved answers — the same question goes out instantly next time, without calling the AI.",
    nightEyebrow: "Always on",
    nightTitle: "While your team sleeps, the chatbot keeps answering",
    nightBody: "Trained on your documents, it answers visitor questions instantly — day, night, or weekend.",
    faqHeading: "Frequently asked questions",
    faqs: [
      {
        q: "Is it hard to install?",
        a: "No — sign up and we generate a one-line script for you. Cafe24, Imweb, and WordPress get step-by-step guidance right in the dashboard.",
      },
      {
        q: "What does the chatbot learn from?",
        a: "Documents you upload from the dashboard. It searches them for the parts relevant to a question, then answers from those.",
      },
      {
        q: "Can I try it for free?",
        a: "Yes — 14 days, no card required. The trial is capped at 100 conversations and 30 documents.",
      },
      {
        q: "What happens if the chatbot doesn't know the answer?",
        a: "It doesn't guess. It tells the visitor it couldn't find an answer and offers a contact form so a person can follow up.",
      },
      {
        q: "Can teammates manage the dashboard together?",
        a: "Yes — invite teammates by email from the dashboard and manage saved answers, documents, and settings together.",
      },
    ],
  },
  ko: {
    heroTitle: ["한 줄만 붙이면,", "챗봇이 방문자 질문에 답합니다"],
    heroSubtitle: "사이트 주소만 알려주시면 페이지를 학습해 챗봇의 지식 데이터베이스로 활용합니다.",
    heroCta: "시작하기",
    docsBadge: "답변 정확도 개선",
    docsTitle: ["문서가 쌓일수록", "답변도 정확해져요"],
    docsSubtitle: "학습과 답변을 모두 답해줘 하나에서 처리하니까 가능합니다.",
    docsCard: {
      badge: "정확한 답변",
      title: ["학습한 문서와 챗봇이 만나", "더 정확해집니다"],
      body: "AI가 업로드된 문서 안에서 질문과 관련된 내용을 찾아 답변을 만듭니다.",
    },
    docsMiniCards: [
      { title: "코드 한 줄만 붙이면 연결이 끝나요", body: "</body> 바로 위에 붙여넣기만 하면 됩니다. 키가 노출돼도 등록한 주소에서만 동작해 안전해요." },
      { title: "놓친 질문은 답변 개선 목록에 쌓여요", body: "한 번 답을 등록하면 그 다음부터는 저장 답변으로 즉시 나갑니다." },
    ],
    strengthsTitle: "데모가 아니라, 실제로 동작하는 기능입니다",
    strengthsSubtitle: "설치, 학습, 요금, 브랜딩, 보안까지 — 실제로 어떻게 동작하는지 확인해 보세요.",
    strengths: [
      { tab: "이용 대상", stat: "4가지 역할", title: "쇼핑몰 운영자부터 마케터까지, 역할별로 다르게 씁니다", body: "혼자인 쇼핑몰 운영자는 개발자 없이 설치하고, CS 담당은 저장 답변을 다듬고, 마케터는 리드와 통계를 확인합니다." },
      { tab: "설치", stat: "30분", title: "스크립트 한 줄이면 끝", body: "가입하면 코드가 자동으로 만들어집니다. 카페24·아임웹·워드프레스는 화면에서 단계별로 안내합니다." },
      { tab: "학습", stat: "600자", title: "문서를 나눠 학습하고, 관련된 부분만 찾습니다", body: "페이지를 약 600자 단위로 나눠 학습해 질문과 관련된 부분만 정확히 찾아냅니다." },
      { tab: "요금", stat: "14일", title: "한도를 넘어도 요금이 늘지 않습니다", body: "카드 등록 없이 14일 무료로 시작하세요. 한도를 넘으면 요금이 아니라 챗봇이 잠시 멈춥니다." },
      { tab: "브랜딩", stat: "3단계", title: "우리 브랜드 색으로 챗봇을 꾸밉니다", body: "런처 크기, 색상, 로고까지 브랜드에 맞게 바꿀 수 있어요." },
      { tab: "보안", stat: "", title: "등록한 주소에서만 동작해요", body: "설치 키가 노출돼도 등록하지 않은 사이트에서는 동작하지 않습니다." },
    ],
    flywheelEyebrow: "저장 답변 플라이휠",
    flywheelTitle: "쓰면 쓸수록 똑똑해지고, 우리 브랜드처럼 보이는 챗봇",
    flywheelSubtitle: "답을 한 번 등록하면 다음부터는 AI 없이 즉시 나가고, 로고와 색상까지 브랜드에 맞게 다듬을 수 있습니다.",
    flywheelTabs: [
      { label: "저장 답변", title: "공통 질문으로 등록하면 AI 없이 바로 나갑니다", body: "공통 질문에 등록하고 노출을 켜면, 방문자가 버튼만 눌러도 저장해 둔 답변이 즉시 나갑니다." },
      { label: "답변 개선", title: "실패한 질문은 대화 로그에서 바로 보입니다", body: "대화 로그에서 실패 태그로 놓친 질문을 바로 확인하고, 답변 개선 목록에서 답을 등록하면 저장 답변이 됩니다." },
      { label: "브랜딩", title: "로고와 색상까지 우리 브랜드로 맞춥니다", body: "로고·버튼 아이콘·버블 색상·첫 인사말까지 위젯 관리 화면에서 바꾸고 미리보기로 바로 확인합니다." },
    ],
    flowEyebrow: "동작 원리",
    flowTitle: "쓸수록, 저장 답변이 쌓입니다",
    flowSubtitle: "방문자 질문은 학습한 문서와 말투 설정을 근거로 AI가 답하고, 놓친 질문은 담당자가 채우면 저장 답변이 됩니다.",
    flowLoop: "담당자가 등록한 답은 저장 답변에 더해져, 같은 질문에는 다음부터 AI 호출 없이 즉시 나갑니다.",
    nightEyebrow: "24시간 무중단 응대",
    nightTitle: "사람이 잠든 새벽에도 챗봇은 답합니다",
    nightBody: "학습해 둔 문서를 바탕으로 새벽이든 주말이든, 방문자 질문에 챗봇이 즉시 답합니다.",
    faqHeading: "자주 묻는 질문",
    faqs: [
      {
        q: "설치가 어렵나요?",
        a: "아니요. 가입하면 스크립트 한 줄이 자동으로 만들어집니다. 카페24·아임웹·워드프레스는 대시보드에서 단계별로 안내해 드려요.",
      },
      {
        q: "챗봇은 무엇을 학습하나요?",
        a: "대시보드에서 업로드한 문서를 학습합니다. AI가 문서 안에서 질문과 관련된 부분을 찾아 그 내용으로 답변합니다.",
      },
      {
        q: "무료로 체험할 수 있나요?",
        a: "네, 카드 등록 없이 14일간 무료로 체험할 수 있습니다. 체험 기간에는 대화 100건·문서 30건까지 이용 가능합니다.",
      },
      {
        q: "챗봇이 답을 모르면 어떻게 되나요?",
        a: "모르는 답을 지어내지 않습니다. 답변을 찾지 못했다는 안내와 함께 연락처를 남길 수 있는 폼을 보여드려, 담당자가 직접 확인할 수 있습니다.",
      },
      {
        q: "팀원과 함께 관리할 수 있나요?",
        a: "네. 대시보드에서 이메일로 팀원을 초대해 저장 답변·문서·설정을 함께 관리할 수 있습니다.",
      },
    ],
  },
};
