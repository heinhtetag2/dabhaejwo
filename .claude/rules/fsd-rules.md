# Feature-Sliced Design (FSD) Rules

> **적용 대상**: `admin/src/`, `tenant/src/`. 두 프로젝트는 설정을 공유하지 않지만 구조 규칙은 동일하다.
> `widget/` 은 FSD를 쓰지 않는다 — `widget-embed-script.md` 참조.
> 이 프로젝트는 **TanStack Query + Zustand 를 사용한다** (`kickoff-prompt.md` §1.3).

## 레이어 순서 (위 → 아래, 단방향 의존)
```
app → widgets → features → entities → shared
```
✅ 상위 레이어는 하위 레이어를 임포트할 수 있다
❌ 하위 레이어에서 상위 레이어 임포트 금지 (순환 의존 금지)
❌ 같은 레이어 내 슬라이스 간 직접 임포트 금지 — 조합은 상위 레이어에서

---

## entities — 도메인 데이터 레이어

백엔드 리소스와 1:1 대응하는 타입·스키마·쿼리·뮤테이션을 담는다.

```
entities/{domain}/{resource}/
  types.ts      ← 도메인 인터페이스·enum·상수
  schema.ts     ← zod 스키마. 키는 api-contracts.md 의 JSON 키와 동일
  query.ts      ← useXxxQuery — TanStack Query 훅
  mutation.ts   ← useCreateXxx / useUpdateXxx / useDeleteXxx
  index.ts      ← public barrel (외부 진입점)
```

- Query key: `[{리소스}, 'list', params]` / `[{리소스}, 'detail', id]`. mutation 성공 시 invalidate.
- ✅ entities는 UI 없음 — 순수 데이터 레이어
- ✅ 외부에서는 반드시 `index.ts` 통해서만 임포트
- ❌ entities 안에 React 컴포넌트 금지
- ❌ features의 타입을 직접 재정의 금지 — entities에서 import

**언제 entities에 두나**: Tenant, Faq, Conversation처럼 백엔드에 API가 존재하는 리소스

---

## features — UI 기능 레이어

사용자가 수행하는 기능 단위 UI. 데이터 타입은 entities에서 가져온다.

```
features/{domain}/{feature}/
  ui/
    {feature}-view.tsx         ← 페이지 오케스트레이터 (상태·뮤테이션 조합)
    {feature}-table.tsx        ← DataTable 래퍼
    {feature}-search.tsx       ← 검색·필터 툴바
    {feature}-form-modal.tsx   ← 등록/수정 Modal
  index.ts                     ← FeatureView export만
```

- ✅ features는 UI 전용 — 타입은 `@entities/...`에서 import
- ❌ 도메인 타입·스키마·쿼리 직접 정의 금지
- ❌ 300줄 초과 컴포넌트 금지 — 분리

> **features/auth 예외**: 로그인은 CRUD 리소스가 아닌 use-case이므로 `model/` 훅을 features 안에 둘 수 있다. 단 백엔드 리소스 타입은 `entities/` 에 둔다.

---

## widgets — 화면 블록

사이드바, 헤더, 대리접속 배너처럼 여러 화면에 걸치는 조합 블록.

---

## shared — 공통 레이어

```
shared/common/   ← 순수 원자 UI (Button, Input, Modal, DataTable)
shared/ui/       ← 복합 UI (StatusBadge, ConfirmModal, Pagination, CostRatioBar)
shared/api/      ← httpClient 단일 인스턴스
shared/lib/      ← 유틸 (cn, format, store)
shared/config/   ← env(zod 검증), 라우트 상수
```

- ✅ shared는 도메인 지식 없음 — 어느 프로젝트에서도 재사용 가능
- ❌ shared에 특정 도메인 비즈니스 로직 금지
- ✅ **둘 이상의 feature가 함께 쓰는 UI 컴포넌트는 `shared/ui/`에 둔다.** (실제 사고: 재사용 카드를 `features/home/ui/`에 두고 다른 feature가 import → features→features 교차슬라이스 위반)

---

## 배치 판단

리소스 타입/통신 → entities · 사용자 행동(동사) → features · 화면 블록 → widgets · 도메인 무관 → shared.
애매하면 상위 레이어에 두고 두 번째 사용처가 생길 때 내린다. **선제적 shared 승격 금지.**

---

## 명명 규칙

- 폴더: `kebab-case`
- 컴포넌트 파일: `kebab-case.tsx` (예: `tenants-view.tsx`)
- 훅: `use-kebab-case.ts`
- 타입/스키마/쿼리/뮤테이션 파일: `kebab-case.ts`

---

## 체크리스트

- [ ] 새 리소스 작업 시 `entities/{domain}/{resource}/index.ts` public API 정의
- [ ] features UI는 `@entities/...`에서 타입 import — 직접 정의 금지
- [ ] 레이어 간 의존 방향 위반 없음
- [ ] 같은 레이어 내 슬라이스 간 직접 임포트 없음
- [ ] shared에 도메인 누수 없음
- [ ] zod 스키마 키 = `docs/architecture/api-contracts.md` 의 JSON 키
