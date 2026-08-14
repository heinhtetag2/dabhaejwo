import type { Language } from "@/shared/lib/language";

export const SIGNUP_TEXT: Record<
  Language,
  {
    title: string;
    subtitle: string;
    emailLabel: string;
    passwordLabel: string;
    passwordHint: string;
    tenantLabel: string;
    tenantHint: string;
    domainLabel: string;
    domainHint: string;
    consentBefore: string;
    termsLabel: string;
    consentMiddle: string;
    privacyLabel: string;
    consentAfter: string;
    genericError: string;
    creating: string;
    startFree: string;
    hasAccount: string;
    logIn: string;
  }
> = {
  en: {
    title: "Start free for 14 days",
    subtitle: "No card required. Just tell us your site's address and training starts right away.",
    emailLabel: "Email",
    passwordLabel: "Password",
    passwordHint: "At least 8 characters",
    tenantLabel: "Company name",
    tenantHint: "Becomes your chatbot's default name. You can change it later.",
    domainLabel: "Site address",
    domainHint: "We'll train on this address, and the chatbot only appears there.",
    consentBefore: "I agree to the ",
    termsLabel: "Terms of Service",
    consentMiddle: " and ",
    privacyLabel: "Privacy Policy",
    consentAfter: ".",
    genericError: "Couldn't sign up. Please try again in a moment.",
    creating: "Creating…",
    startFree: "Start free",
    hasAccount: "Already have an account?",
    logIn: "Log in",
  },
  ko: {
    title: "14일 무료로 시작하기",
    subtitle: "카드 등록이 필요 없습니다. 사이트 주소만 알려주시면 바로 학습을 시작합니다.",
    emailLabel: "이메일",
    passwordLabel: "비밀번호",
    passwordHint: "8자 이상",
    tenantLabel: "업체명",
    tenantHint: "챗봇 이름의 기본값이 됩니다. 나중에 바꿀 수 있습니다.",
    domainLabel: "홈페이지 주소",
    domainHint: "이 주소를 학습하고, 이 주소에서만 챗봇이 뜹니다.",
    consentBefore: "",
    termsLabel: "이용약관",
    consentMiddle: "과 ",
    privacyLabel: "개인정보처리방침",
    consentAfter: "에 동의합니다.",
    genericError: "가입하지 못했습니다. 잠시 후 다시 시도해 주세요.",
    creating: "만드는 중…",
    startFree: "무료로 시작하기",
    hasAccount: "이미 계정이 있으신가요?",
    logIn: "로그인",
  },
};
