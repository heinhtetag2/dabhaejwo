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
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	// MariaDB 는 flyway-mysql 모듈이 담당한다 (별도 flyway-database-mariadb 는 없다)
	implementation("org.flywaydb:flyway-mysql")

	// JWT — 운영자 / 업체 담당자 / 대리 로그인 토큰
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

	compileOnly("org.projectlombok:lombok")
	runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
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

/**
 * 저장소 루트의 .env 를 bootRun 의 환경변수로 넣어준다.
 *
 * Spring Boot 는 .env 를 읽지 않는다 — Node/Docker 쪽 관례라서다. 이게 없으면
 * .env 에 적어둔 접속 정보가 앱에 전달되지 않고, application-local.yml 의
 * 기본값(localhost)으로 조용히 붙어버린다.
 *
 * 개발 편의용이므로 bootRun 에만 건다. 운영은 배포 환경의 시크릿 관리로 주입한다.
 */
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
	val dotenv = project.file("../.env")
	if (dotenv.exists()) {
		dotenv.readLines()
			.map(String::trim)
			.filter { it.isNotEmpty() && !it.startsWith("#") && it.contains('=') }
			.forEach { line ->
				// 값에 '=' 가 들어갈 수 있다(비밀번호). 첫 '=' 에서만 자른다.
				val key = line.substringBefore('=').trim()
				val value = line.substringAfter('=').trim()
				if (key.isNotEmpty()) {
					environment(key, value)
				}
			}
	}
}
