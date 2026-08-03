# LLM 공급사 계정 메모

> ⚠️ **이 문서에 키를 적지 않는다.** 자격증명은 `.env`(gitignore 대상), 키 이름은 `.env.example`.
> 이 파일의 이전 버전에 Gemini API 키가 평문으로 있었다. **해당 키는 폐기·재발급 대상이다.**

## 발급된 프로젝트

| 항목 | 값 |
|---|---|
| 공급사 | Google AI (Gemini) |
| 프로젝트명 | chatbot |
| 프로젝트 번호 | 625919626103 |
| 키 보관 위치 | `.env` 의 `GEMINI_API_KEY` |

## SDK 메모

- Node.js 18+ / `npm install @google/genai` (Python은 `pip install -U google-genai`)
- 구 패키지 `google-generativeai` 는 폐기됨. 예제에서 보이면 무시한다.
- 모델 별칭(`gemini-flash-latest` 등)과 고정 버전 중 **고정 버전을 쓴다.** 별칭은 공급사가 가리키는 대상을 바꾸면 원가가 소리 없이 변한다.

## 이 프로젝트에서의 취급

모델명·단가는 **코드에 넣지 않는다.** `model_prices` 테이블에서 관리하고, 호출 시점 단가로 원가를 확정 저장한다.
설계는 [architecture/llm-provider.md](architecture/llm-provider.md) 참조.
