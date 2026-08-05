# GitLab CI/CD Variables — 서버 준비 체크리스트

> Settings → CI/CD → Variables. **Masked + Protected** 권장.
> 아래 목록은 문서를 옮긴 것이 아니라 `application-production.yml`·`application.yml`·
> `@Value` 를 코드에서 훑어 만든 것이다. 값이 빠지면 어떻게 되는지를 함께 적었다.

---

## 1. 없으면 **기동 자체가 실패**한다

운영 프로파일은 기본값을 주지 않는다. 조용히 `localhost` 로 뜨는 것보다 못 뜨는 편이 낫다.

| 변수 | 예시 | 비고 |
|---|---|---|
| `POSTGRES_HOST` | `192.168.0.254` | |
| `POSTGRES_PORT` | `6001` | |
| `POSTGRES_DB` | `dabhaejwo` | |
| `POSTGRES_USER` | `dabhaejwo` | |
| `POSTGRES_PASSWORD` | — | Masked |
| `JWT_SECRET` | 랜덤 32바이트 이상 | **바뀌면 전 사용자 로그아웃.** 한 번 정하고 고정 |
| `CORS_ALLOWED_ORIGINS` | `https://dabhaejwo.tagoplus.co.kr,https://dabhaejwo-mng.tagoplus.co.kr` | 스킴 포함, **끝에 `/` 없이**, 쉼표 구분 |
| `OPS_IP_ALLOWLIST` | `203.0.113.0/24` | 값을 **요구하지만 코드가 읽지 않는다** (§5 참조) |
| `LLM_DEFAULT_PROVIDER` | `GOOGLE` | |
| `R2_BUCKET` | `dabaejwo` | |
| `R2_ENDPOINT` | `https://<account>.r2.cloudflarestorage.com` | |
| `R2_ACCESS_KEY_ID` | — | Masked |
| `R2_SECRET_ACCESS_KEY` | — | Masked |

> `CORS_ALLOWED_ORIGINS` 에 **위젯 도메인(cdn)은 넣지 않는다.** 방문자 출처는 미리 알 수
> 없어 `/api/widget/**` 은 별도로 열려 있고, 실제 판정은 공개 키 + 업체가 등록한 주소로 한다.
> 이 목록은 **알림 WebSocket 의 허용 Origin 이기도 하다** — 빠뜨리면 소켓만 조용히 안 붙는다.

---

## 2. 기동은 되지만 **아무도 로그인할 수 없다**

`application.yml` 이 빈 기본값을 줘서 앱은 뜬다. 그래서 더 위험하다 — 배포는 성공으로 보인다.

| 변수 | 없으면 |
|---|---|
| `MAIL_HOST` `MAIL_PORT` `MAIL_USERNAME` `MAIL_PASSWORD` `MAIL_FROM` | **로그인이 불가능하다.** 2단계 인증 코드가 메일로 나가는데, 미설정이면 `UnavailableMailer` 가 1단계에서 거절한다(`FEATURE_NOT_READY`). 초대·비밀번호 찾기도 같이 죽는다 |
| `ENCRYPTION_KEY` | 운영 콘솔에서 **공급사 API 키를 등록할 수 없다** → 챗봇이 답하지 못한다. 아래 §4 의 주의 참조 |
| `APP_BASE_URL` `OPS_BASE_URL` | 초대·비밀번호 재설정 **메일 링크가 `localhost` 를 가리킨다.** 각각 `https://dabhaejwo.tagoplus.co.kr`, `https://dabhaejwo-mng.tagoplus.co.kr` |
| `MAIL_FROM_NAME` | 보내는 이름이 기본값(`답해줘`)이 된다. 무해 |

---

## 3. 프론트 번들에 **박히는** 값 (빌드 시점)

**런타임에 못 바꾼다.** 컨테이너 환경변수를 고쳐도 아무 일도 일어나지 않는다 — 다시 빌드해야 한다.

| 변수 | 예시 | 없으면 |
|---|---|---|
| `NEXT_PUBLIC_API_BASE_URL` | `https://dabhaejwo-api.tagoplus.co.kr` | **빌드 실패** (compose 가 막는다) |
| `NEXT_PUBLIC_WIDGET_SRC` | `https://dabhaejwo-cdn.tagoplus.co.kr/w.js` | **빌드 실패** |
| `WIDGET_API_BASE_URL` | 위와 같은 API 주소 | 미설정 시 `NEXT_PUBLIC_API_BASE_URL` 을 쓴다 |

---

## 4. 선택 — 기본값으로 돌아간다

| 변수 | 기본 | 언제 바꾸나 |
|---|---|---|
| `SERVER_PORT` | `4310` | compose 포트 매핑과 함께 바꿔야 한다 |
| `DB_POOL_SIZE` | `10` | |
| `MAX_FILE_SIZE_MB` | `20` | `application.yml` 의 multipart 상한·Apache `LimitRequestBody` 와 **셋이 같이** 움직인다 |
| `AUTH_COOKIE_SAME_SITE` | `Lax` | API 를 **다른 등록가능 도메인**으로 옮길 때만 `None` (그때는 Secure 필수) |
| `JWT_ACCESS_TTL_MINUTES` | `30` | |
| `JWT_REFRESH_TTL_DAYS` | `14` | 리프레시 쿠키 수명과 같다 |
| `SLACK_OPS_WEBHOOK_URL` | 빈값 | 미구현(stub). 넣어도 안 나간다 |
| `GEMINI_API_KEY` `ANTHROPIC_API_KEY` `OPENAI_API_KEY` | 빈값 | **넣지 않는 것을 권장.** 공급사 키는 운영 콘솔에서 암호화 등록한다 |

> **`ENCRYPTION_KEY` 주의** — 지금 쓰는 개발 DB 를 그대로 운영에 쓴다면, 이 값이 로컬
> `.env` 와 **같아야 한다.** 다르면 이미 등록된 Gemini 키를 복호화하지 못하고
> `CREDENTIAL_UNREADABLE` 로 실패한다. 새 DB 라면 아무 값이나 정한 뒤 콘솔에서 키를 다시 등록한다.
> 이 값은 **한 번 정하면 바꿀 수 없다**(교체 경로 미구현 — IMPROVEMENTS P1).

> `OPS_SEED_EMAIL` 은 CI 가 `.env` 에 쓰지만 **운영에서는 아무 일도 하지 않는다.**
> 운영자 시더가 `local` 프로파일 전용이기 때문이다 — §5 를 보라.

---

## 5. 변수만으로는 안 되는 것 두 가지

### ① 운영자 계정이 하나도 없다 → 콘솔에 못 들어간다

시더(`OpsDemoSeeder`)가 `@Profile("local")` 이라 운영에서는 `operators` 가 **비어 있다.**
첫 계정은 DB 에 직접 넣어야 한다.

```bash
# 1) BCrypt 해시 생성 (Apache 를 쓰므로 apache2-utils 가 이미 있을 가능성이 높다)
htpasswd -bnBC 10 "" '원하는비밀번호' | tr -d ':\n'
# → $2y$10$.... (Spring 이 $2y$ 를 그대로 받아들인다 — 실제로 확인함)

# 2) 계정 삽입
psql -h $POSTGRES_HOST -p $POSTGRES_PORT -U $POSTGRES_USER -d $POSTGRES_DB -c "
INSERT INTO operators (email, name, role, active, password_hash)
VALUES ('본인@메일주소', '이름', 'OPS_ADMIN', true, '\$2y\$10\$....');"
```

- 이메일은 **실제로 받을 수 있는 주소**여야 한다. 로그인 2단계 코드가 그리로 간다
- 한 명만 넣으면 된다. 나머지는 콘솔의 **관리자 계정 관리** 화면에서 만든다
- 등록 후 이메일은 바꿀 수 없다(감사 기록이 가리키는 사람이다)

### ② 운영 콘솔 IP 제한은 Apache 가 유일한 방어선이다

`OPS_IP_ALLOWLIST` 를 필수로 요구하면서 **그 값을 읽는 코드가 없다**(P0 등록됨).
`deploy/apache/dabhaejwo-mng.conf` 의 `Require ip` 블록 주석을 **반드시 풀어야** 한다.
풀지 않으면 로그인만 통과하면 누구나 전 업체 데이터를 볼 수 있다.

---

## 6. 붙여넣기용 — 변수 이름만

```
POSTGRES_HOST  POSTGRES_PORT  POSTGRES_DB  POSTGRES_USER  POSTGRES_PASSWORD
JWT_SECRET  ENCRYPTION_KEY
CORS_ALLOWED_ORIGINS  OPS_IP_ALLOWLIST  LLM_DEFAULT_PROVIDER
R2_BUCKET  R2_ENDPOINT  R2_ACCESS_KEY_ID  R2_SECRET_ACCESS_KEY
MAIL_HOST  MAIL_PORT  MAIL_USERNAME  MAIL_PASSWORD  MAIL_FROM  MAIL_FROM_NAME
APP_BASE_URL  OPS_BASE_URL  OPS_SEED_EMAIL
NEXT_PUBLIC_API_BASE_URL  NEXT_PUBLIC_WIDGET_SRC  WIDGET_API_BASE_URL
```

---

## 7. 첫 배포 뒤 확인 순서

```bash
docker compose -f deploy/docker-compose.yml ps      # 3개 up, api healthy
curl -fsS https://dabhaejwo-api.tagoplus.co.kr/actuator/health
curl -fsS https://dabhaejwo-cdn.tagoplus.co.kr/w.js | head -c 60
```

1. **로그인** — 운영 콘솔에서 §5 로 만든 계정. 메일로 코드가 오는지까지 확인
2. **공급사 키 등록** — 모델과 프롬프트 → 공급사 연결 (`.env` 가 아니라 콘솔)
3. **가입 → 문서 업로드 → 위젯 질문** 한 바퀴. 여기까지 되면 실제로 동작하는 것이다
4. **알림 소켓** — 콘솔에서 벨이 뜨는지. 안 뜨면 `proxy_wstunnel` 을 의심한다

문제가 생기면 `docker compose logs -f api` 가 첫 번째로 볼 곳이다.
기동 실패는 대개 위 §1 의 변수 하나가 빠진 것이고, 로그에 이름이 그대로 찍힌다.
