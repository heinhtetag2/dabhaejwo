"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

import { useCreateBot } from "@/entities/tenant/bot";
import { ApiError } from "@/shared/api/http-client";
import { Button } from "@/shared/common/button";
import { Card, CardBody, CardHeader } from "@/shared/common/card";
import { controlClass } from "@/shared/common/control";
import { ROUTES, botRoute } from "@/shared/config/routes";
import { Notice } from "@/shared/ui/notice";

/**
 * 서비스 추가.
 *
 * <p>입력이 둘뿐인 것이 의도다 — 말투·모양은 기본값으로 만들고 나중에 고치게 한다.
 * 만들 때 다 물으면 "코드 붙이기"라는 다음 행동이 뒤로 밀린다.
 *
 * <p>모달이 아니라 페이지인 이유: 상한 도달을 모달 안에서 알리면 요금제 안내로 넘어갈
 * 자리가 없다.
 */
export function BotNewView() {
  const router = useRouter();
  const create = useCreateBot();
  const [name, setName] = useState("");
  const [domain, setDomain] = useState("");

  const submit = (event: React.FormEvent) => {
    event.preventDefault();
    if (create.isPending) {
      return;
    }
    create.mutate(
      { name: name.trim(), primaryDomain: domain.trim() },
      {
        // 새 서비스의 유일한 다음 행동은 코드 붙이기다. 바로 그리로 보낸다.
        onSuccess: (bot) => router.replace(botRoute(bot.id, "install")),
      },
    );
  };

  const error = create.error;
  const limitReached = error instanceof ApiError && error.code === "BOT_LIMIT_REACHED";

  return (
    <Card className="max-w-[560px]">
      <CardHeader title="서비스 추가" />
      <CardBody>
        <form onSubmit={submit} noValidate>
          <div className="mb-4.5">
            <label htmlFor="bot-name" className="mb-1.5 block text-[12.5px] font-medium">
              서비스 이름
            </label>
            <input
              id="bot-name"
              value={name}
              onChange={(event) => setName(event.target.value)}
              placeholder="쇼핑몰"
              className={controlClass("md")}
            />
            <p className="mt-1.5 text-[11.5px] text-slate-2">
              사이드바에서 서비스를 고를 때 보이는 이름입니다.
            </p>
          </div>

          <div className="mb-4.5">
            <label htmlFor="bot-domain" className="mb-1.5 block text-[12.5px] font-medium">
              홈페이지 주소
            </label>
            <input
              id="bot-domain"
              value={domain}
              onChange={(event) => setDomain(event.target.value)}
              placeholder="shop.example.com"
              className={controlClass("md")}
            />
            <p className="mt-1.5 text-[11.5px] leading-relaxed text-slate-2">
              이 주소에서만 챗봇이 동작합니다. 다른 주소는 만든 뒤 설치 화면에서 더할 수 있습니다.
            </p>
          </div>

          {error ? (
            <Notice tone={limitReached ? "warn" : "error"} className="mb-4">
              {error instanceof ApiError ? error.message : "서비스를 만들지 못했습니다"}
              {limitReached ? (
                <>
                  {" "}
                  <a href={ROUTES.plan} className="underline">
                    요금제 보기
                  </a>
                </>
              ) : null}
            </Notice>
          ) : null}

          <div className="flex gap-2">
            <Button
              type="submit"
              variant="accent"
              size="sm"
              disabled={create.isPending || name.trim().length === 0 || domain.trim().length === 0}
            >
              {create.isPending ? "만드는 중…" : "만들기"}
            </Button>
            <Button type="button" size="sm" onClick={() => router.push(ROUTES.bots)}>
              취소
            </Button>
          </div>
        </form>
      </CardBody>
    </Card>
  );
}
