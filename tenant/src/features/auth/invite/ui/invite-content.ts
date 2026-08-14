import type { Language } from "@/shared/lib/language";

export const INVITE_TEXT: Record<
  Language,
  {
    roleLabel: Record<string, string>;
    invalidTitle: string;
    invalidNotice: string;
    backToLogin: string;
    checking: string;
    invitedTitle: (tenantName: string) => string;
    subtitle: string;
    emailLabel: string;
    roleFieldLabel: string;
    passwordLabel: string;
    passwordHint: string;
    confirmPasswordLabel: string;
    genericError: string;
    settingUp: string;
    setPasswordAndStart: string;
  }
> = {
  en: {
    roleLabel: { OWNER: "Owner", EDITOR: "Editor", VIEWER: "Viewer" },
    invalidTitle: "Can't open this invite link",
    invalidNotice: "The link has expired or has already been used. Ask whoever invited you to send a new one.",
    backToLogin: "Back to log in",
    checking: "Checking your invite…",
    invitedTitle: (tenantName) => `You've been invited to ${tenantName}`,
    subtitle: "Set a password and you're ready to go.",
    emailLabel: "Email",
    roleFieldLabel: "Role",
    passwordLabel: "Password",
    passwordHint: "At least 8 characters",
    confirmPasswordLabel: "Confirm password",
    genericError: "Couldn't set the password. Please try again in a moment.",
    settingUp: "Setting up…",
    setPasswordAndStart: "Set password and get started",
  },
  ko: {
    roleLabel: { OWNER: "소유자", EDITOR: "편집", VIEWER: "보기만" },
    invalidTitle: "초대 링크를 열 수 없습니다",
    invalidNotice: "링크가 만료되었거나 이미 사용되었습니다. 초대한 분께 다시 보내달라고 요청해 주세요.",
    backToLogin: "로그인으로 돌아가기",
    checking: "초대를 확인하는 중…",
    invitedTitle: (tenantName) => `${tenantName} 팀에 초대되었습니다`,
    subtitle: "비밀번호를 정하시면 바로 시작할 수 있습니다.",
    emailLabel: "이메일",
    roleFieldLabel: "권한",
    passwordLabel: "비밀번호",
    passwordHint: "8자 이상",
    confirmPasswordLabel: "비밀번호 확인",
    genericError: "비밀번호를 설정하지 못했습니다. 잠시 후 다시 시도해 주세요.",
    settingUp: "설정하는 중…",
    setPasswordAndStart: "비밀번호 설정하고 시작하기",
  },
};
