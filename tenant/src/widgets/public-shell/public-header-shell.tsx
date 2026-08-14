"use client";

import type { ReactNode } from "react";
import { useEffect, useRef, useState } from "react";

/**
 * 헤더의 스크롤 반응 껍데기.
 *
 * <p>아래로 스크롤하면 부드럽게 숨고, 위로 스크롤하거나 맨 위 근처면 다시 보인다(사용자 요청).
 * 내용(로고·메뉴·버튼)은 서버에서 렌더된 children 그대로 받는다 — 클라이언트가 되는 건
 * 이 껍데기뿐이라 언어 번역이 걸린 나머지 헤더 내용은 계속 Server Component 로 남는다.
 *
 * <p>`sticky` 를 유지한 채 `translateY` 만 바꾼다 — 자리는 계속 차지하므로 히어로가 헤더
 * 자리 밑으로 끌어올려진 오프셋(`pricing-view-v2.tsx`)이 숨김 상태와 무관하게 그대로 맞는다.
 *
 * <p>투명한 채로 바로 사라지면 밑 콘텐츠 위에서 헤더가 뚝 끊기듯 없어지는 것처럼 보인다
 * (사용자 지적). 그래서 맨 위에서 조금이라도 내려가면 — 숨기기 직전부터 — 옅은 흰 배경을
 * 먼저 얹는다. `hidden` 과는 별도 상태(`scrolled`)라 완전히 숨겨지기 전에 배경이 자연스럽게
 * 옅어지고, 다시 맨 위로 오면 함께 투명해진다.
 */
export function PublicHeaderShell({ children }: { children: ReactNode }) {
  const [hidden, setHidden] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const lastScrollY = useRef(0);
  const ticking = useRef(false);

  useEffect(() => {
    lastScrollY.current = window.scrollY;

    function handleScroll() {
      if (ticking.current) return;
      ticking.current = true;
      requestAnimationFrame(() => {
        const currentY = window.scrollY;
        const scrolledDown = currentY > lastScrollY.current;
        const pastThreshold = currentY > 80;
        setHidden(scrolledDown && pastThreshold);
        setScrolled(currentY > 8);
        lastScrollY.current = currentY;
        ticking.current = false;
      });
    }

    window.addEventListener("scroll", handleScroll, { passive: true });
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  return (
    <header
      className={`sticky top-0 z-30 transition-all duration-300 ease-in-out ${
        hidden ? "-translate-y-full" : "translate-y-0"
      } ${scrolled ? "bg-white/80 backdrop-blur-sm" : "bg-transparent"}`}
    >
      {children}
    </header>
  );
}
