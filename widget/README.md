# widget

업체 사이트에 임베드되는 방문자 챗봇.

```html
<script>
  window.dabhaejwo = { key: "pk_live_..." };
</script>
<script src="https://cdn.dabhaejwo.com/w.js" async></script>
```

## 사이즈 예산

**gzip 30KB.** 초과하면 `npm run size` 가 실패한다.

남의 사이트 로딩에 얹히므로 사이즈가 곧 제품 품질이다. 초과 시 대응 순서는
① 패널을 동적 임포트로 분리 → ② 의존성 제거 → ③ 기능 축소.

## 확인

| 대상 | 명령 |
|---|---|
| 타입체크 | `npm run typecheck` |
| 테스트 | `npm test` |
| 빌드 | `npm run build` |
| 사이즈 | `npm run size` |
| 데모 | `npm run dev` → `/demo/index.html` |

데모 페이지에는 전역 `button` 스타일과 `.root { display:none }` 을 일부러 넣어 두었다.
버블이 정상으로 보이고 눌린다면 Shadow DOM 격리가 동작하는 것이다.

## 규칙

`../.claude/rules/widget-embed-script.md` 를 따른다. 요약하면:

- Shadow DOM 필수. 전역 CSS 셀렉터 금지
- 웹폰트를 로드하지 않는다 — 방문자에게 폰트 다운로드를 강제하지 않는다
- 로더는 **절대 throw 하지 않는다.** 위젯이 실패해도 호스트 페이지는 멀쩡해야 한다
- 정상 동작 중 `console` 을 오염시키지 않는다
- 공개 키만 받는다. 시크릿을 받는 필드를 타입에 두지 않는다
- 중복 삽입 멱등 — 태그 매니저·SPA 라우팅으로 두 번 로드될 수 있다
