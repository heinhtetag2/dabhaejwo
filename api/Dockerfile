# syntax=docker/dockerfile:1

# ── build stage: Gradle + JDK 21 ────────────────────────────────────
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 의존성 캐시 레이어. 소스만 바뀌었을 때 재다운로드를 피한다.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# 테스트는 이미지 빌드에서 돌리지 않는다 — 실패한 코드를 이미지로 만들지 않기 위한 방어는
# CI 파이프라인(`./gradlew build`)이 맡는다. 여기서 또 돌리면 배포마다 몇 분씩 더 든다.
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# ── runtime stage: JRE 21 ───────────────────────────────────────────
# Alpine 이 아니라 Debian 계열인 이유 — PDFBox·POI 가 글꼴·네이티브 경로에 의존하는 코드
# 경로를 갖고 있어 musl 에서 드물게 다르게 동작한다. 학습 파이프라인이 조용히 깨지면
# 업체는 "학습이 안 된다"로만 겪는다.
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# 헬스체크용 curl + 비루트 유저
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r app && useradd -r -g app app

COPY --from=build /app/build/libs/*.jar app.jar
USER app

EXPOSE 4310
# 컨테이너에는 .env 가 없다. 운영 프로파일은 애초에 .env 를 읽지 않고
# 환경변수만 본다(DotenvEnvironmentPostProcessor) — 값이 빠지면 기동에 실패한다.
ENV SPRING_PROFILES_ACTIVE=production
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
