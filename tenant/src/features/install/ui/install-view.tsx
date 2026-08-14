"use client";

import { useState } from "react";

import { useAppContextQuery } from "@/entities/auth/session";
import { useBotSettingsQuery, useSaveBotSettings } from "@/entities/chatbot/bot-settings";
import {
  useAddAllowedOrigin,
  useAllowedOriginsQuery,
  useRemoveAllowedOrigin,
} from "@/entities/tenant/allowed-origin";
import { ApiError } from "@/shared/api/http-client";
import { Button } from "@/shared/common/button";
import { env } from "@/shared/config/env";
import { Card, CardBody, CardHeader } from "@/shared/common/card";
import { ErrorState, LoadingState } from "@/shared/common/states";
import { canEdit } from "@/shared/lib/auth-store";
import { Notice } from "@/shared/ui/notice";
import { StatusBadge } from "@/shared/ui/status-badge";
import { controlClass } from "@/shared/common/control";
import { date } from "@/shared/lib/format";
import { useCurrentBotId } from "@/shared/lib/current-bot";

/**
 * 위젯 CDN. 배포 환경마다 다르므로 **빌드 시점에 주입**한다(`NEXT_PUBLIC_WIDGET_SRC`).
 * 코드에 박아두면 스테이징에서 운영 위젯을 불러오게 된다.
 */
const WIDGET_SRC = env.widgetSrc;

const PLATFORM_GUIDES = [
  { name: "카페24", steps: "쇼핑몰 관리자 → 디자인 → HTML 편집 → 공통 레이아웃의 body 끝에 붙여넣습니다." },
  { name: "아임웹", steps: "사이트 관리 → 고급 설정 → 외부 스크립트 → body 영역에 붙여넣습니다." },
  { name: "워드프레스", steps: "테마의 footer.php 를 열거나, 스크립트 삽입 플러그인의 Footer 칸에 붙여넣습니다." },
  { name: "그누보드", steps: "스킨의 tail.php 맨 아래, </body> 바로 위에 붙여넣습니다." },
];

/**
 * 설치. <b>가입 후 가장 먼저 넘어야 할 관문이다</b> (tenant-plan.md §4.8).
 *
 * <p>코드를 붙였는데 안 뜨는 상황이 가장 흔한 이탈 지점이므로, 실제 호출이 들어왔는지를
 * 주소별로 보여준다. 업체가 "됐는지 안 됐는지" 헤매지 않게 하는 것이 이 화면의 핵심이다.
 */
export function InstallView() {
  const { data: context } = useAppContextQuery();
  const origins = useAllowedOriginsQuery();
  const settings = useBotSettingsQuery();
  const saveSettings = useSaveBotSettings();

  const addOrigin = useAddAllowedOrigin();
  const removeOrigin = useRemoveAllowedOrigin();

  const [newOrigin, setNewOrigin] = useState("");
  const [notice, setNotice] = useState<{ tone: "info" | "error"; text: string } | null>(null);
  const [copied, setCopied] = useState(false);

  /*
   * 키는 **서비스의 것**이다. 업체에서 읽으면 서비스가 둘일 때 어느 쪽 스니펫인지 알 수 없고,
   * 잘못 붙이면 방문자가 옆 사이트 챗봇을 보게 된다.
   */
  const botId = useCurrentBotId();
  const editable = canEdit(context?.member?.role);
  const publishableKey =
    context?.bots.find((bot) => bot.id === botId)?.publishableKey ?? "";

  if (origins.isPending || settings.isPending) {
    return <LoadingState />;
  }
  if (origins.isError) {
    return <ErrorState message="설치 정보를 불러오지 못했습니다" onRetry={() => void origins.refetch()} />;
  }

  /*
   * `charset="utf-8"` 을 붙인다.
   *
   * 스크립트에 charset 이 없으면 브라우저는 <b>호스트 페이지의 인코딩</b>으로 w.js 를
   * 해석한다. 업체 사이트가 EUC-KR 이면 위젯 안의 한글이 그 사이트에서만 깨진다 —
   * 우리 쪽에서는 멀쩡히 보이므로 원인을 찾기 어렵다.
   *
   * 서버도 `charset=utf-8` 을 붙여 보내지만(deploy/apache/dabhaejwo-cdn.conf), 업체가
   * 자체 CDN·프록시를 앞에 두면 헤더가 사라질 수 있다. 여기가 그 마지막 방어선이다.
   */
  const snippet = `<!-- 답해줘 챗봇 -->
<script>
  window.dabhaejwo = { key: "${publishableKey}" };
</script>
<script src="${WIDGET_SRC}" charset="utf-8" async></script>`;

  const connected = origins.data.some((origin) => origin.lastCalledAt !== null);

  const run = (action: () => Promise<unknown>, successText?: string) => {
    setNotice(null);
    void action()
      .then(() => successText && setNotice({ tone: "info", text: successText }))
      .catch((cause: unknown) =>
        setNotice({
          tone: "error",
          text: cause instanceof ApiError ? cause.message : "요청을 처리하지 못했습니다",
        }),
      );
  };

  return (
    <div className="grid gap-4 xl:grid-cols-[1fr_340px]">
      {/* min-w-0 — 코드 블록의 긴 줄이 이 열을 벌려 오른쪽 칸을 화면 밖으로 밀어냈다. */}
      <div className="min-w-0">
        <Card className="mb-4">
          <CardHeader
            title="홈페이지에 붙일 코드"
            aside={
              connected ? (
                <StatusBadge tone="ok" label="연결 확인됨" />
              ) : (
                <StatusBadge tone="idle" label="아직 호출 없음" />
              )
            }
          />
          <CardBody>
            <p className="mb-3.5 text-[13px] text-slate">
              아래 코드를 홈페이지의{" "}
              <code className="rounded bg-paper px-1.5 py-0.5 font-mono text-[12px]">&lt;/body&gt;</code>{" "}
              바로 위에 붙여넣으세요. 모든 페이지에 들어가야 합니다.
            </p>

            <div className="relative">
              <pre className="overflow-x-auto rounded-lg bg-ink px-4 py-3.5 font-mono text-[12px] leading-relaxed text-code">
                {snippet}
              </pre>
              <Button
                size="sm"
                className="absolute top-2.5 right-2.5"
                onClick={() => {
                  void navigator.clipboard.writeText(snippet).then(() => setCopied(true));
                }}
              >
                {copied ? "복사됨" : "복사"}
              </Button>
            </div>

            <p className="mt-3 text-[11.5px] leading-relaxed text-slate-2">
              이 키는 공개돼도 괜찮습니다. 아래에 등록한 주소에서만 작동하기 때문입니다.
            </p>

            {/*
              가장 흔한 이탈 지점이다. 등록되지 않은 주소에서는 위젯이 **아무것도 그리지 않고
              사라진다** — 남의 사이트에 우리 오류를 그리지 않기 위해서다. 그래서 업체 눈에는
              "코드를 붙였는데 안 뜬다"로만 보이고, 원인이 화면 어디에도 없었다.
            */}
            {connected ? null : (
              <Notice tone="info" className="mt-3">
                <b>코드를 붙였는데 안 보이나요?</b> 주소가 아래 목록과 정확히 같아야 합니다 —{" "}
                <code className="font-mono">www.</code> 가 붙은 주소는 <b>별도로 등록</b>해야
                하고, 하위 도메인도 각각 등록해야 합니다. 등록되지 않은 주소에서는 챗봇이
                오류 없이 그냥 나타나지 않습니다.
              </Notice>
            )}
          </CardBody>
        </Card>

        {notice ? (
          <Notice tone={notice.tone} className="mb-4">
            {notice.text}
          </Notice>
        ) : null}

        <Card className="mb-4">
          <CardHeader
            title="허용할 주소"
            aside={
              editable ? (
                <>
                  <label className="sr-only" htmlFor="new-origin">
                    추가할 주소
                  </label>
                  <input
                    id="new-origin"
                    value={newOrigin}
                    onChange={(event) => setNewOrigin(event.target.value)}
                    placeholder="shop.example.com"
                    className={controlClass("sm", "w-[190px]")}
                  />
                  <Button
                    size="sm"
                    disabled={newOrigin.trim().length === 0 || addOrigin.isPending}
                    onClick={() =>
                      run(
                        () =>
                          addOrigin.mutateAsync(newOrigin.trim()).then(() => setNewOrigin("")),
                        "주소를 추가했습니다.",
                      )
                    }
                  >
                    주소 추가
                  </Button>
                </>
              ) : null
            }
          />
          <ul>
            {origins.data.map((origin) => (
              <li
                key={origin.id}
                className="flex flex-wrap items-center gap-3 border-b border-line-2 px-3.5 py-3 last:border-b-0"
              >
                <span className="min-w-0 flex-1 truncate font-mono text-[13px]">{origin.origin}</span>
                {origin.lastCalledAt ? (
                  <StatusBadge tone="ok" label="작동 중" />
                ) : (
                  <StatusBadge tone="idle" label="호출 없음" />
                )}
                <span className="w-[120px] text-[11.5px] text-slate-2">
                  {origin.lastCalledAt
                    ? `마지막 ${date(origin.lastCalledAt)}`
                    : "—"}
                </span>
                {editable ? (
                  <Button
                    size="sm"
                    variant="danger"
                    disabled={removeOrigin.isPending}
                    onClick={() => run(() => removeOrigin.mutateAsync(origin.id), "주소를 지웠습니다.")}
                  >
                    삭제
                  </Button>
                ) : null}
              </li>
            ))}
          </ul>
        </Card>

        <Card>
          <CardHeader title="어디에 붙일지 모르겠다면" />
          <CardBody className="space-y-3.5">
            {PLATFORM_GUIDES.map((guide, index) => (
              <div key={guide.name} className="flex gap-3">
                <span className="tabular grid size-6 shrink-0 place-items-center rounded-full bg-paper text-[11.5px] font-semibold">
                  {index + 1}
                </span>
                <p className="text-[13px]">
                  <b className="font-semibold">{guide.name}</b>
                  <span className="mt-0.5 block text-[12.5px] leading-relaxed text-slate">
                    {guide.steps}
                  </span>
                </p>
              </div>
            ))}
          </CardBody>
        </Card>
      </div>

      <div className="xl:sticky xl:top-6 xl:self-start">
        <Card>
          <CardHeader title="어디에 띄울까요" />
          <CardBody className="space-y-4">
            <div>
              <label htmlFor="position" className="mb-1.5 block text-[12.5px] font-medium">
                버블 위치
              </label>
              <select
                id="position"
                value={settings.data?.widgetPosition}
                disabled={!editable || saveSettings.isPending}
                onChange={(event) =>
                  settings.data &&
                  run(
                    () =>
                      saveSettings.mutateAsync({
                        ...settings.data,
                        widgetPosition: event.target.value as "BOTTOM_RIGHT" | "BOTTOM_LEFT",
                      }),
                    "저장했습니다.",
                  )
                }
                className={controlClass("md")}
              >
                <option value="BOTTOM_RIGHT">오른쪽 아래</option>
                <option value="BOTTOM_LEFT">왼쪽 아래</option>
              </select>
            </div>

            <div>
              <label htmlFor="nudge" className="mb-1.5 block text-[12.5px] font-medium">
                자동으로 말 걸기
              </label>
              <select
                id="nudge"
                value={String(settings.data?.nudgeDelaySeconds ?? 15)}
                disabled={!editable || saveSettings.isPending}
                onChange={(event) =>
                  settings.data &&
                  run(
                    () =>
                      saveSettings.mutateAsync({
                        ...settings.data,
                        nudgeDelaySeconds: Number(event.target.value),
                      }),
                    "저장했습니다.",
                  )
                }
                className={controlClass("md")}
              >
                <option value="0">띄우지 않기</option>
                <option value="5">5초 후</option>
                <option value="15">15초 후</option>
              </select>
              <p className="mt-1.5 text-[11.5px] text-slate-2">
                방문자가 먼저 묻지 않아도 인사말 말풍선을 띄웁니다.
              </p>
            </div>
          </CardBody>
        </Card>
      </div>
    </div>
  );
}
