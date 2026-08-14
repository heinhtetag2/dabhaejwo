import type { Language } from "@/shared/lib/language";

export const PASSWORD_RESET_TEXT: Record<
  Language,
  {
    forgotTitle: string;
    forgotSubtitle: string;
    emailLabel: string;
    forgotGenericError: string;
    sending: string;
    getTemporaryPassword: string;
    forgotNote: string;
    backToLogin: string;
    resetTitle: string;
    resetSubtitle: string;
    temporaryPasswordLabel: string;
    temporaryPasswordHint: string;
    newPasswordLabel: string;
    newPasswordHint: string;
    confirmPasswordLabel: string;
    resetGenericError: string;
    changing: string;
    changePassword: string;
  }
> = {
  en: {
    forgotTitle: "Forgot your password?",
    forgotSubtitle: "We'll send a temporary password to the email you signed up with.",
    emailLabel: "Email",
    forgotGenericError: "Couldn't send the email. Please try again in a moment.",
    sending: "Sending…",
    getTemporaryPassword: "Get temporary password",
    forgotNote: "If that address is registered, an email is on its way. If it doesn't arrive, check your spam folder.",
    backToLogin: "Back to log in",
    resetTitle: "Set a new password",
    resetSubtitle: "Enter the temporary password from your email, then choose a new one.",
    temporaryPasswordLabel: "Temporary password",
    temporaryPasswordHint: "Enter it exactly as it appears in the email.",
    newPasswordLabel: "New password",
    newPasswordHint: "At least 8 characters",
    confirmPasswordLabel: "Confirm new password",
    resetGenericError: "Couldn't change the password. Please try again in a moment.",
    changing: "Changing…",
    changePassword: "Change password",
  },
  ko: {
    forgotTitle: "비밀번호 찾기",
    forgotSubtitle: "가입하신 이메일로 임시 비밀번호를 보내드립니다.",
    emailLabel: "이메일",
    forgotGenericError: "메일을 보내지 못했습니다. 잠시 후 다시 시도해 주세요.",
    sending: "보내는 중…",
    getTemporaryPassword: "임시 비밀번호 받기",
    forgotNote: "가입된 주소라면 메일이 갑니다. 오지 않으면 스팸함을 확인해 주세요.",
    backToLogin: "로그인으로 돌아가기",
    resetTitle: "새 비밀번호 설정",
    resetSubtitle: "메일로 받은 임시 비밀번호를 입력하고 새 비밀번호를 정해 주세요.",
    temporaryPasswordLabel: "임시 비밀번호",
    temporaryPasswordHint: "메일에 적힌 값을 그대로 입력해 주세요.",
    newPasswordLabel: "새 비밀번호",
    newPasswordHint: "8자 이상",
    confirmPasswordLabel: "새 비밀번호 확인",
    resetGenericError: "비밀번호를 바꾸지 못했습니다. 잠시 후 다시 시도해 주세요.",
    changing: "바꾸는 중…",
    changePassword: "비밀번호 바꾸기",
  },
};
