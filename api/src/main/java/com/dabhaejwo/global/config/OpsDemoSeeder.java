package com.dabhaejwo.global.config;

import com.dabhaejwo.domain.operator.entity.Operator;
import com.dabhaejwo.domain.operator.repository.OperatorRepository;
import com.dabhaejwo.global.security.OperatorRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 로컬 개발용 운영자 계정.
 *
 * <p>역할을 넷 다 만드는 이유는 <b>권한 매트릭스가 실제로 막는지</b> 확인해야 하기 때문이다.
 * OPS_ADMIN 하나만 두면 화면은 전부 열리고, 다른 역할에서 무엇이 보이는지 알 수 없다.
 *
 * <p><b>{@code local} 프로파일에서만 돈다.</b> 운영에는 운영자 계정을 만드는 경로가
 * 아직 없다 — 초대·SSO 가 붙기 전까지는 DB 에 직접 넣어야 한다 (docs/IMPROVEMENTS.md 등록).
 *
 * <p>{@code DemoDataSeeder} 와 분리한 이유는 그쪽이 데모 업체가 이미 있으면 통째로
 * 건너뛰기 때문이다. 업체가 있든 없든 운영자는 있어야 콘솔에 들어갈 수 있다.
 *
 * <p><b>관리자 주소는 설정으로 뺀다.</b> 로그인이 2단계가 되면서 인증 코드를 받을 수 있는
 * 실제 사서함이 필요해졌다 — {@code @dabhaejwo.com} 은 존재하지 않는 주소라
 * 코드가 영영 오지 않는다. {@code OPS_SEED_EMAIL} 에 본인 주소를 넣어 쓴다.
 */
@Configuration
@Profile("local")
public class OpsDemoSeeder {

    private static final Logger log = LoggerFactory.getLogger(OpsDemoSeeder.class);

    /** 로컬 전용이라 문서와 코드에 그대로 둔다. */
    private static final String DEMO_PASSWORD = "demo1234";

    /** 실제로 로그인해 볼 관리자 주소. 인증 코드가 여기로 간다. */
    @Value("${OPS_SEED_EMAIL:admin@dabhaejwo.com}")
    private String adminEmail;

    @Bean
    ApplicationRunner seedOperators(OperatorRepository repository, PasswordEncoder passwordEncoder) {
        return args -> {
            String hash = passwordEncoder.encode(DEMO_PASSWORD);
            create(repository, adminEmail, "정OO", OperatorRole.OPS_ADMIN, hash);
            create(repository, "cs@dabhaejwo.com", "김OO", OperatorRole.CS, hash);
            create(repository, "sales@dabhaejwo.com", "박OO", OperatorRole.SALES, hash);
            create(repository, "dev@dabhaejwo.com", "이OO", OperatorRole.DEV, hash);
        };
    }

    /** 이미 있으면 아무것도 하지 않는다 — 재기동에 안전하고, 바꾼 비밀번호를 되돌리지 않는다. */
    private void create(OperatorRepository repository,
                        String email,
                        String name,
                        OperatorRole role,
                        String passwordHash) {
        if (repository.findByEmail(email).isPresent()) {
            return;
        }
        repository.save(Operator.create(email, name, role, passwordHash));
        log.info("운영자 계정을 만들었습니다 — {} ({}) · 비밀번호 {}", email, role, DEMO_PASSWORD);
    }
}
