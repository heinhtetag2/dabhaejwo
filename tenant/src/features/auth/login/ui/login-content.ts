import type { Language } from "@/shared/lib/language";

export const LOGIN_TEXT: Record<
  Language,
  {
    title: string;
    subtitle: string;
    emailLabel: string;
    passwordLabel: string;
    genericError: string;
    sending: string;
    next: string;
    forgotPassword: string;
    noAccount: string;
    startFree: string;
    invitedNote: string;
    otpTitle: string;
    otpSubtitleBefore: string;
    otpSubtitleAfter: (minutes: number) => string;
    otpLabel: string;
    otpGenericError: string;
    verifying: string;
    logIn: string;
    otpNotReceived: string;
    otpRetry: string;
    otpRetrySuffix: string;
  }
> = {
  en: {
    title: "Log in",
    subtitle: "Manage the chatbot on your site from here.",
    emailLabel: "Email",
    passwordLabel: "Password",
    genericError: "That email or password isn't right.",
    sending: "Sending verification code…",
    next: "Next",
    forgotPassword: "Forgot your password?",
    noAccount: "Don't have an account yet?",
    startFree: "Start free",
    invitedNote: "Invited by a teammate? Use the link from your invite email instead.",
    otpTitle: "Enter verification code",
    otpSubtitleBefore: "We sent a 6-digit code to ",
    otpSubtitleAfter: (minutes) => `. Enter it within ${minutes} minutes.`,
    otpLabel: "Verification code",
    otpGenericError: "Couldn't verify. Please try again in a moment.",
    verifying: "Verifying…",
    logIn: "Log in",
    otpNotReceived: "Didn't get an email? Check your spam folder, and",
    otpRetry: "start over",
    otpRetrySuffix: "",
  },
  ko: {
    title: "로그인",
    subtitle: "홈페이지에 붙인 챗봇을 여기서 관리합니다.",
    emailLabel: "이메일",
    passwordLabel: "비밀번호",
    genericError: "이메일 또는 비밀번호가 올바르지 않습니다.",
    sending: "인증 코드를 보내는 중…",
    next: "다음",
    forgotPassword: "비밀번호를 잊으셨나요?",
    noAccount: "아직 계정이 없으신가요?",
    startFree: "무료로 시작하기",
    invitedNote: "팀원으로 초대받으셨다면 초대 메일의 링크로 들어와 주세요.",
    otpTitle: "인증 코드 입력",
    otpSubtitleBefore: "",
    otpSubtitleAfter: (minutes) => ` 으로 6자리 코드를 보냈습니다. ${minutes}분 안에 입력해 주세요.`,
    otpLabel: "인증 코드",
    otpGenericError: "인증하지 못했습니다. 잠시 후 다시 시도해 주세요.",
    verifying: "확인 중…",
    logIn: "로그인",
    otpNotReceived: "메일이 오지 않았나요? 스팸함을 확인해 보시고,",
    otpRetry: "처음부터 다시",
    otpRetrySuffix: "시도해 주세요.",
  },
};
