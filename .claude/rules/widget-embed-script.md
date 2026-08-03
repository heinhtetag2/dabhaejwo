# widget-embed-script — 제3자 사이트에 삽입되는 임베드 위젯

## 적용 조건

- 산출물이 **남의 웹사이트에서 실행되는 단일 스크립트**(`<script src="...">`)인 프로젝트. 챗봇 위젯, 피드백 버튼, 예약 버블 등.
- npm 패키지(`library-npm-typescript`)와 다르다 — 소비자가 빌드에 포함하는 게 아니라 **런타임에 CDN에서 로드**한다. 버전 고정이 불가능하므로 하위 호환이 훨씬 엄격하다.
- 앱(`frontend-rules`)과도 다르다 — 라우터·SSR·전역 CSS가 없고, 우리가 페이지의 주인이 아니다.

## 스택

- TypeScript strict. 빌드는 **Vite library mode** — IIFE 단일 파일 출력(`format: 'iife'`), `inlineDynamicImports` 로 청크 분할 방지.
  로더가 `<script async>` 하나로 끝나야 하므로 청크가 쪼개지면 안 된다.
- UI는 **Preact**(~4KB) 또는 vanilla. React는 런타임이 무거워 부적합.
- CSS는 `?inline` 임포트로 문자열화해 Shadow Root에 주입. 별도 `.css` 파일 산출 금지 — 호스트가 로드해 주지 않는다.
- 런타임 의존성 최소. 추가는 되돌리기 어려운 결정으로 취급하고 CLAUDE.md 핵심 결정에 사유와 함께 기록.
- 구체 버전은 생성 시점에 확인.

## 디렉토리 구조

```
widget/
├── src/
│   ├── loader.ts        # 진입점. 설정 읽기 → host element → attachShadow → mount. 여기서만 document 를 만진다
│   ├── config.ts        # window 전역에서 설정 파싱 + 기본값 + 검증
│   ├── api/             # 백엔드 호출. 공개 키만 사용
│   ├── ui/              # 컴포넌트 (bubble, nudge, panel, message, lead-form)
│   ├── styles.css       # Shadow Root 에 주입될 스타일 전량
│   └── types.ts
├── demo/
│   └── index.html       # 가상 호스트 사이트. 실제 임베드 형태로 확인
├── tests/
├── vite.config.ts
└── package.json
```

## 레이어/의존성 규칙

- `loader` → `ui` → `api` → `config`/`types`. 역방향 금지.
- **`document` 직접 접근은 `loader.ts` 안에서만.** UI 컴포넌트는 Shadow Root 안에서만 산다. `document.querySelector` 가 UI 레이어에 나타나면 호스트 페이지를 침범하는 코드다.
- `api/` 는 DOM을 모른다. `ui/` 는 fetch를 모른다.

## 컨벤션 — 호스트 사이트 침범 금지 (이 모듈의 핵심)

우리는 남의 페이지에 세들어 산다. 아래는 전부 실제로 사고가 나는 항목이다.

- **Shadow DOM 필수.** host element 하나만 `document.body` 에 붙이고 나머지는 전부 shadow root 안. 전역 CSS 셀렉터 금지.
- **폰트를 강요하지 않는다.** `@import`/`@font-face` 로 웹폰트를 로드하면 방문자에게 우리 폰트 다운로드를 강제하고 호스트의 LCP를 망친다. system font stack 을 쓴다.
- **전역 오염 최소.** `window` 에는 설정 네임스페이스 하나(`window.{brand}`)만. 전역 함수·클래스 등록 금지.
- **`console` 오염 금지.** 정상 동작 중 로그를 남기지 않는다. 디버그 로그는 설정 플래그로 잠근다.
- **호스트를 깨뜨리지 않는다.** 로더 전체를 `try/catch` 로 감싸고 **절대 throw 하지 않는다.** 위젯이 실패해도 호스트 페이지는 멀쩡해야 한다. 실패 시 조용히 마운트를 포기한다.
- **z-index** 는 단일 상수로 관리하고 최댓값을 쓰지 않는다(`2147483647` 은 다른 위젯과 전면전이 된다). 호스트가 덮으면 설정으로 조정.
- **중복 삽입 방어.** 스크립트가 두 번 로드될 수 있다(SPA 라우팅·태그 매니저). host element 존재 여부로 멱등 보장.
- `prefers-reduced-motion` 존중. 접근성: 키보드 조작, `aria-label`, 패널 열림 시 focus trap, `Esc` 로 닫기.

## 컨벤션 — 보안

- **공개 키만 사용한다.** 시크릿을 받는 필드를 config 타입에 두지 않아 오배치를 컴파일 타임에 차단한다.
- 신뢰 경계는 서버다. Origin 검증·레이트 리밋·테넌트 식별은 전부 백엔드에서. 위젯의 어떤 값도 신뢰하지 않는다.
- 서버 응답을 `innerHTML` 로 넣지 않는다. 텍스트는 `textContent`, 마크업이 필요하면 화이트리스트 렌더러.
- 방문자 입력을 로컬에 저장하지 않는다(호스트 도메인의 storage 를 오염시킨다). 세션 유지는 서버 발급 식별자로.

## 컨벤션 — 사이즈

- **사이즈 예산을 README에 명시하고, 초과하면 마일스톤 완료 금지.** 위젯은 사이즈가 곧 제품 품질이다.
- 예산 초과 시 순서: ① 패널을 동적 임포트로 분리(버블만 먼저) → ② 의존성 제거 → ③ 기능 축소.
- 빌드 산출물 크기를 검증 명령에 포함해 회귀를 막는다.

## 테스트

- vitest + happy-dom. **Shadow Root 안에서** 쿼리해 검증한다(`host.shadowRoot!.querySelector`). document 로 찾아지면 격리가 깨진 것이므로 그 자체가 실패 케이스다.
- 필수 케이스: 중복 삽입 멱등 / 잘못된 설정에서 조용히 실패(throw 안 함) / API 오류 시 UI 복구 / 키보드 전용 조작.
- 호스트 침범 회귀 테스트: 마운트 후 `document.body` 직계 자식이 1개만 늘었는지, 전역 스타일시트가 추가되지 않았는지.
- 실 백엔드 스모크는 CI 별도 잡. 로컬 기본 테스트는 키 없이도 그린이어야 한다.

## 검증 명령

| 대상 | 명령 |
|------|------|
| 타입체크 | `npm run typecheck` (`tsc --noEmit`) |
| 테스트 | `npm test` (`vitest run`) |
| 빌드 | `npm run build` (`vite build`) |
| 사이즈 | `npm run size` (gzip 크기 예산 초과 시 실패) |
| 데모 | `npm run dev` → `demo/index.html` 에서 실제 임베드 확인 |
