plugins {
	java
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.dabhaejwo"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	// Spring Boot 4.1 BOM 은 spring-boot-starter-aop 를 더 이상 관리하지 않는다.
	// spring-aop 는 spring-context 로 딸려오므로 @Aspect 파싱에 필요한 weaver 만 직접 건다.
	implementation("org.aspectj:aspectjweaver:1.9.24")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	// 초대·OTP·비밀번호 재설정 메일. 미설정이면 발송을 거부한다(UnavailableMailer).
	implementation("org.springframework.boot:spring-boot-starter-mail")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	// 알림 실시간 전달. STOMP 는 쓰지 않는다 — 서버가 미는 한 방향이라 프레임 규약이면 충분하다.
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("org.flywaydb:flyway-database-postgresql")

	// pgvector 타입 매핑 (knowledge_chunks.embedding)
	implementation("com.pgvector:pgvector:0.1.6")

	// 파일 저장소. Cloudflare R2 는 S3 호환이라 AWS SDK 를 그대로 쓴다.
	// apache-client 를 명시하는 이유는 기본 HTTP 클라이언트를 SDK 가 런타임에 찾는데,
	// 없으면 기동은 되고 첫 업로드에서 터진다.
	implementation("software.amazon.awssdk:s3:2.50.3")
	implementation("software.amazon.awssdk:apache-client:2.50.3")

	// 문서에서 글자 뽑기. Tika 대신 필요한 파서만 직접 건다 —
	// tika-parsers-standard 는 수십 개 포맷의 의존성을 통째로 끌고 오고, 우리는 6개만 받는다.
	implementation("org.apache.pdfbox:pdfbox:3.0.8")
	implementation("org.apache.poi:poi-ooxml:5.5.1")

	// JWT — 운영자 / 업체 담당자 / 대리 로그인 토큰
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

	compileOnly("org.projectlombok:lombok")
	runtimeOnly("org.postgresql:postgresql")
	annotationProcessor("org.projectlombok:lombok")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// 라이브러리용 plain jar 를 만들지 않는다.
//
// 기본값으로 두면 build/libs 에 `-plain.jar` 가 함께 생기고, Dockerfile 의
// `COPY build/libs/*.jar app.jar` 가 **두 파일에 매칭돼 빌드가 실패한다**
// (대상이 디렉터리가 아닌데 소스가 여럿). 이 프로젝트는 실행 가능한 부트 jar 하나만
// 필요하므로 애초에 만들지 않는 편이 낫다 — Dockerfile 에서 파일명을 고정하면
// 버전이 바뀔 때마다 같이 고쳐야 한다.
tasks.named<Jar>("jar") {
	enabled = false
}

// .env 로딩은 여기가 아니라 애플리케이션 안에 있다
// (DotenvEnvironmentPostProcessor). bootRun 에만 걸면 IDE 에서 main 을 직접
// 실행할 때 건너뛰어 localhost 기본값으로 조용히 붙는다.
