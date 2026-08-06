"use client";

import { useState } from "react";

import { useAppContextQuery } from "@/entities/auth/session";
import {
  LAUNCHER_BACKGROUND_LABEL,
  LAUNCHER_SIZE_LABEL,
  useBotSettingsDraft,
  type LauncherBackground,
  type LauncherSize,
} from "@/entities/chatbot/bot-settings";
import { useFaqListQuery } from "@/entities/chatbot/faq";
import { Button } from "@/shared/common/button";
import { Card, CardBody, CardHeader, Eyebrow } from "@/shared/common/card";
import { controlClass } from "@/shared/common/control";
import { ErrorState, LoadingState } from "@/shared/common/states";
import { env } from "@/shared/config/env";
import { canEdit } from "@/shared/lib/auth-store";
import { cn } from "@/shared/lib/cn";
import { Notice } from "@/shared/ui/notice";
import { SettingsField, SettingsToggle } from "@/shared/ui/settings-field";
import { WidgetPreview } from "@/shared/ui/widget-preview";

import { ImageUploadField } from "./image-upload-field";

const PRESET_COLORS = ["#17222E", "#1B6B5C", "#BF3F2B", "#3B5BDB"];
const SIZES: LauncherSize[] = ["SMALL", "MEDIUM", "LARGE"];
const BACKGROUNDS: LauncherBackground[] = ["BRAND", "WHITE", "NONE"];

/**
 * 위젯 관리 — 방문자에게 <b>어떻게 보일지</b>.
 *
 * <p>말투와 나눈 이유 — 한 화면에 있을 때 "챗봇이 뭐라고 말하나"와 "챗봇이 어떻게 생겼나"가
 * 섞여 있어, 색을 바꾸러 들어온 사람이 프롬프트를 스크롤로 지나쳐야 했다. 고치는 빈도도
 * 다르다: 모양은 처음 한 번, 말투는 답변을 보며 계속 다듬는다.
 */
export function WidgetStyleView() {
  const { data: context } = useAppContextQuery();
  const { data: faqs } = useFaqListQuery();
  const { query, save, draft, dirty, patch, reset, submit } = useBotSettingsDraft();
  const [error, setError] = useState<string | null>(null);

  const editable = canEdit(context?.member?.role);
  const shownFaqs = (faqs ?? []).filter((faq) => faq.shown);

  if (query.isPending || draft === undefined) {
    return <LoadingState />;
  }
  if (query.isError) {
    return <ErrorState message="설정을 불러오지 못했습니다" onRetry={() => void query.refetch()} />;
  }

  return (
    <div className="grid gap-4 xl:grid-cols-[1fr_380px]">
      <div className="min-w-0">
        {/*
          노출 카드가 맨 앞이다. 급히 챗봇을 내려야 하는 상황에서 스크롤을 내려가며
          찾게 만들면 안 된다 — 학습이 덜 된 채로 공개됐거나 답변이 이상할 때 쓰는 스위치다.
        */}
        <Card className="mb-4">
          <CardHeader title="노출" />
          <CardBody className="space-y-3">
            <SettingsToggle
              label="홈페이지에 챗봇 띄우기"
              hint="끄면 방문자에게 챗봇이 보이지 않습니다. 홈페이지에 붙인 코드는 그대로 두셔도 됩니다."
              checked={draft.widgetEnabled}
              disabled={!editable}
              onChange={(widgetEnabled) => patch({ widgetEnabled })}
            />
            {draft.widgetEnabled ? null : (
              <Notice tone="warn">
                지금은 방문자에게 챗봇이 보이지 않습니다. 다시 켜면 바로 나타납니다.
              </Notice>
            )}
          </CardBody>
        </Card>

        <Card className="mb-4">
          <CardHeader title="브랜딩" />
          <CardBody>
            <ImageUploadField
              kind="logo"
              label="업체 로고"
              hint="왼쪽 메뉴에 표시됩니다. 아래 런처 아이콘을 따로 올리지 않으면 챗봇 버튼에도 이 로고가 쓰입니다. PNG · JPG · WEBP, 512KB 이하."
              currentUrl={draft.logoUrl}
            />
            <ImageUploadField
              kind="launcher-icon"
              label="챗봇 버튼 아이콘"
              hint="로고 대신 챗봇 버튼에만 쓸 그림입니다. 동그랗게 잘리므로 정사각형 그림이 가장 잘 맞습니다. 올리지 않으면 기본 말풍선 아이콘이 나옵니다."
              currentUrl={draft.launcherIconUrl}
              round
            />
          </CardBody>
        </Card>

        <Card className="mb-4">
          <CardHeader title="모양" />
          <CardBody>
            <div className="grid gap-4 sm:grid-cols-2">
              <SettingsField id="bot-name" label="챗봇 이름">
                <input
                  id="bot-name"
                  value={draft.botName}
                  disabled={!editable}
                  onChange={(event) => patch({ botName: event.target.value })}
                  className={controlClass("md")}
                />
              </SettingsField>

              <SettingsField id="brand-color" label="버블 색상">
                <span className="flex flex-wrap items-center gap-2">
                  {PRESET_COLORS.map((color) => (
                    <button
                      key={color}
                      type="button"
                      aria-label={`색상 ${color}`}
                      aria-pressed={draft.brandColor.toUpperCase() === color}
                      disabled={!editable}
                      onClick={() => patch({ brandColor: color })}
                      className="size-[30px] rounded-[7px] border-2"
                      style={{
                        backgroundColor: color,
                        borderColor:
                          draft.brandColor.toUpperCase() === color ? "#17222E" : "transparent",
                      }}
                    />
                  ))}
                  <input
                    id="brand-color"
                    value={draft.brandColor}
                    disabled={!editable}
                    onChange={(event) => patch({ brandColor: event.target.value })}
                    className={controlClass("sm", "w-24 font-mono")}
                  />
                </span>
              </SettingsField>
            </div>

            <SettingsField id="greeting" label="첫 인사말">
              <input
                id="greeting"
                value={draft.greeting}
                disabled={!editable}
                onChange={(event) => patch({ greeting: event.target.value })}
                className={controlClass("md")}
              />
            </SettingsField>

            {/*
              픽셀을 직접 받지 않는다. 업체는 자기 사이트에서 어떻게 보일지 감으로 정하게 되고,
              남의 화면을 가릴 만큼 크거나 손가락으로 못 누를 만큼 작은 값이 나온다.
            */}
            <SettingsField id="launcher-size" label="챗봇 버튼 크기">
              <span className="flex flex-wrap gap-1.5">
                {SIZES.map((size) => (
                  <button
                    key={size}
                    type="button"
                    aria-pressed={draft.launcherSize === size}
                    disabled={!editable}
                    onClick={() => patch({ launcherSize: size })}
                    className={`rounded-[7px] border px-3 py-1.5 text-[12.5px] transition-colors ${
                      draft.launcherSize === size
                        ? "border-ink bg-ink text-white"
                        : "border-line bg-card hover:bg-line-2/60"
                    }`}
                  >
                    {LAUNCHER_SIZE_LABEL[size]}
                  </button>
                ))}
              </span>
            </SettingsField>

            {/*
              PNG 의 투명은 흰색이 아니라 아무것도 안 칠한 것이라, 그 자리로 뒤에 있는 게
              그대로 올라온다. 브랜드 색 하나로 두면 흰 바탕 기준 로고는 진한 색 위에서
              뭉개지고, 밝은 브랜드 색에 흰 로고를 올리면 아예 안 보인다.
            */}
            <SettingsField id="launcher-background" label="챗봇 버튼 배경">
              <span className="flex flex-wrap items-center gap-3">
                <span className="flex flex-wrap gap-1.5">
                  {BACKGROUNDS.map((background) => (
                    <button
                      key={background}
                      type="button"
                      aria-pressed={draft.launcherBackground === background}
                      disabled={!editable}
                      onClick={() => patch({ launcherBackground: background })}
                      className={`rounded-[7px] border px-3 py-1.5 text-[12.5px] transition-colors ${
                        draft.launcherBackground === background
                          ? "border-ink bg-ink text-white"
                          : "border-line bg-card hover:bg-line-2/60"
                      }`}
                    >
                      {LAUNCHER_BACKGROUND_LABEL[background]}
                    </button>
                  ))}
                </span>
                <LauncherSwatch
                  imageUrl={draft.launcherIconUrl ?? draft.logoUrl}
                  background={draft.launcherBackground}
                  brandColor={draft.brandColor}
                />
              </span>
              {/*
                설정이 조용히 무시되는 상태를 밝힌다. 안 밝히면 업체는 골라놓고
                왜 안 바뀌는지 모른다 — 이번 문의가 바로 그 모양이었다.
              */}
              {draft.launcherIconUrl || draft.logoUrl ? null : (
                <span className="mt-1.5 block text-[11.5px] leading-relaxed text-slate-2">
                  올린 이미지가 없어 이 설정은 아직 쓰이지 않습니다. 기본 말풍선 아이콘은 흰 선으로
                  그려져 흰 바탕·투명 위에서는 보이지 않기 때문입니다.
                </span>
              )}
            </SettingsField>

            <SettingsField id="widget-position" label="화면에서의 위치">
              <span className="flex gap-1.5">
                {(["BOTTOM_RIGHT", "BOTTOM_LEFT"] as const).map((position) => (
                  <button
                    key={position}
                    type="button"
                    aria-pressed={draft.widgetPosition === position}
                    disabled={!editable}
                    onClick={() => patch({ widgetPosition: position })}
                    className={`rounded-[7px] border px-3 py-1.5 text-[12.5px] transition-colors ${
                      draft.widgetPosition === position
                        ? "border-ink bg-ink text-white"
                        : "border-line bg-card hover:bg-line-2/60"
                    }`}
                  >
                    {position === "BOTTOM_RIGHT" ? "오른쪽 아래" : "왼쪽 아래"}
                  </button>
                ))}
              </span>
            </SettingsField>
          </CardBody>
        </Card>
      </div>

      <div className="xl:sticky xl:top-6 xl:self-start">
        <Eyebrow className="mb-2 block">미리보기 · 실제로 이렇게 보입니다</Eyebrow>
        <WidgetPreview
          botName={draft.botName}
          greeting={draft.greeting}
          brandColor={/^#[0-9a-fA-F]{6}$/.test(draft.brandColor) ? draft.brandColor : "#17222E"}
          faqs={shownFaqs.map((faq) => ({
            id: faq.id,
            question: faq.question,
            answer: faq.answer,
            links: faq.links,
            followUpFaqIds: faq.followUpFaqIds,
          }))}
        />

        {error ? (
          <Notice tone="error" className="mt-3">
            {error}
          </Notice>
        ) : null}
        {save.isSuccess && !error ? (
          <Notice tone="info" className="mt-3">
            저장했습니다. 방문자 화면에 바로 반영됩니다.
          </Notice>
        ) : null}

        {editable ? (
          <div className="mt-3 flex gap-2">
            <Button className="flex-1" disabled={!dirty} onClick={reset}>
              되돌리기
            </Button>
            <Button
              variant="accent"
              className="flex-1"
              disabled={save.isPending}
              onClick={() => {
                setError(null);
                submit(setError);
              }}
            >
              {save.isPending ? "저장 중…" : "저장"}
            </Button>
          </div>
        ) : (
          <Notice tone="info" className="mt-3">
            보기 전용 권한입니다. 설정 변경은 편집 권한이 필요합니다.
          </Notice>
        )}
      </div>
    </div>
  );
}

/**
 * 고른 배경 위에 실제 이미지를 얹어 보여준다.
 *
 * <p>이 설정을 만든 계기가 "PNG 인데 왜 색이 보이나"였다. 골라놓고 저장한 뒤 남의 사이트에
 * 가서야 확인할 수 있으면 같은 질문이 다시 나온다 — 고르는 자리에서 바로 보여준다.
 *
 * <p>이미지가 없으면 그리지 않는다. 서버가 그때는 브랜드 색을 강제하므로,
 * 여기서 흰 원을 보여주면 실제와 다른 말을 하는 셈이다.
 */
function LauncherSwatch({
  imageUrl,
  background,
  brandColor,
}: {
  imageUrl: string | null;
  background: LauncherBackground;
  brandColor: string;
}) {
  if (!imageUrl) {
    return null;
  }
  return (
    <span
      className={cn(
        "grid size-11 shrink-0 place-items-center overflow-hidden rounded-full",
        // 투명을 고르면 격자를 깔아 "비어 있음"을 보여준다. 흰 배경 위에 그냥 두면
        // 흰색과 구분되지 않아 두 선택이 같아 보인다.
        background === "NONE" && "bg-[repeating-conic-gradient(#e9e5de_0_25%,#fff_0_50%)] bg-[length:10px_10px]",
        background !== "NONE" && "shadow-sm",
      )}
      style={background === "BRAND" ? { backgroundColor: brandColor } : undefined}
    >
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img
        src={new URL(imageUrl, env.apiBaseUrl).toString()}
        alt=""
        className="size-full object-cover"
      />
    </span>
  );
}
