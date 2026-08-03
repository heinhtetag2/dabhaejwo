package com.dabhaejwo.global.config;

import org.apache.commons.logging.Log;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 저장소 루트의 {@code .env} 를 프로퍼티로 읽어들인다. 개발 편의용이다.
 *
 * <p><b>왜 애플리케이션 안에 있는가.</b> 처음에는 Gradle {@code bootRun} 에만 걸었는데,
 * IDE 에서 main 클래스를 직접 실행하면 그 경로를 타지 않아 {@code localhost} 기본값으로
 * 조용히 떨어졌다. 실행 방식이 무엇이든 같게 동작해야 해서 여기로 옮겼다.
 *
 * <p><b>운영에서는 동작하지 않는다.</b> {@code production} 프로파일이면 파일을 읽지 않는다.
 * 운영 자격증명은 배포 환경의 시크릿 관리로 주입한다.
 *
 * <p>우선순위는 가장 낮게 둔다({@code addLast}) — 실제 환경변수가 항상 {@code .env} 를 이긴다.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String FILE_NAME = ".env";
    private static final String PROPERTY_SOURCE_NAME = "dotenv";
    private static final String PRODUCTION_PROFILE = "production";

    /** 실행 위치가 저장소 루트일 수도, api/ 일 수도 있다. 몇 단계만 거슬러 올라가며 찾는다. */
    private static final int MAX_PARENT_DEPTH = 3;

    private final Log log;

    public DotenvEnvironmentPostProcessor(DeferredLogFactory logFactory) {
        this.log = logFactory.getLog(DotenvEnvironmentPostProcessor.class);
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (isProduction(environment)) {
            return;
        }

        Path file = locate();
        if (file == null) {
            return;
        }

        Map<String, Object> values = parse(file);
        if (values.isEmpty()) {
            return;
        }

        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, values));
        // 어디서 값이 왔는지 모르면 "왜 localhost 로 붙지" 를 한참 헤맨다. 키 이름만 남긴다.
        log.info("개발용 " + FILE_NAME + " 를 읽었습니다: " + file.toAbsolutePath()
                + " (" + values.size() + "개 — " + String.join(", ", values.keySet()) + ")");
    }

    private boolean isProduction(ConfigurableEnvironment environment) {
        for (String profile : environment.getActiveProfiles()) {
            if (PRODUCTION_PROFILE.equalsIgnoreCase(profile)) {
                return true;
            }
        }
        // 이 시점에는 프로파일이 아직 확정되지 않았을 수 있어 원본 프로퍼티도 본다.
        String active = environment.getProperty("spring.profiles.active");
        return active != null && active.toLowerCase().contains(PRODUCTION_PROFILE);
    }

    private Path locate() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int depth = 0; depth <= MAX_PARENT_DEPTH && dir != null; depth++) {
            Path candidate = dir.resolve(FILE_NAME);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        return null;
    }

    private Map<String, Object> parse(Path file) {
        Map<String, Object> values = new LinkedHashMap<>();
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            // 읽기 실패로 기동을 막지 않는다. 값이 없으면 뒤에서 기본값이나 환경변수가 쓰인다.
            log.warn(FILE_NAME + " 를 읽지 못했습니다: " + e.getMessage());
            return values;
        }

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int separator = line.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = line.substring(0, separator).trim();
            // 값에 '=' 가 들어갈 수 있다(비밀번호). 첫 '=' 에서만 자른다.
            String value = unquote(line.substring(separator + 1).trim());
            if (!key.isEmpty()) {
                values.put(key, value);
            }
        }
        return values;
    }

    private String unquote(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                    || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
