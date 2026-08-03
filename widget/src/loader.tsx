import { render } from "preact";

import { readConfig } from "./config";
import styles from "./styles.css?inline";
import { WidgetApp } from "./ui/widget-app";

/** 중복 삽입 판별용. 태그 매니저나 SPA 라우팅으로 스크립트가 두 번 로드될 수 있다. */
const HOST_TAG = "dabhaejwo-widget";

/**
 * 진입점. document 를 만지는 유일한 파일이다.
 *
 * 이 함수는 어떤 경우에도 throw 하지 않는다 — 위젯이 실패해도 호스트 페이지는
 * 멀쩡해야 한다. 우리는 남의 페이지에 세들어 산다.
 */
export function mount(scope: Window = window): HTMLElement | null {
  try {
    if (scope.document.querySelector(HOST_TAG)) {
      // 이미 떠 있다. 두 번 붙이지 않는다.
      return scope.document.querySelector(HOST_TAG);
    }

    const config = readConfig(scope);
    if (!config) {
      // 설정이 없거나 키가 비어 있다. 조용히 포기한다.
      return null;
    }

    const host = scope.document.createElement(HOST_TAG);
    const shadow = host.attachShadow({ mode: "open" });

    const style = scope.document.createElement("style");
    style.textContent = styles;
    shadow.appendChild(style);

    const mountPoint = scope.document.createElement("div");
    shadow.appendChild(mountPoint);

    scope.document.body.appendChild(host);
    render(<WidgetApp config={config} />, mountPoint);

    return host;
  } catch {
    // 정상 동작 중에는 console 을 오염시키지 않는다. 실패도 조용히 넘긴다.
    return null;
  }
}

if (typeof document !== "undefined") {
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => mount(), { once: true });
  } else {
    mount();
  }
}
