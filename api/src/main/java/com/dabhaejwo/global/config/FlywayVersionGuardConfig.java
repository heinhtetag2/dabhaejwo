package com.dabhaejwo.global.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 마이그레이션 전에 서버 버전을 확인하고, 못 미치면 <b>DB에 아무것도 쓰기 전에</b> 멈춘다.
 *
 * <p><b>왜 필요한가.</b> MariaDB 는 DDL 이 트랜잭션이 아니다. V1 이 중간에 실패하면
 * 앞쪽 테이블은 만들어진 채 남고 Flyway 는 되돌리지 못한다. 실제로 10.11 서버에
 * 걸었다가 17개 테이블과 실패 이력이 남아 손으로 치웠다.
 *
 * <p>SQL 안에서 막는 방법도 있지만 그러면 flyway_schema_history 에 실패 기록이 남아
 * {@code repair} 가 필요해진다. 여기서 막으면 DB 는 손도 대지 않은 상태로 남는다.
 */
@Configuration
public class FlywayVersionGuardConfig {

    /** VECTOR 타입이 GA 된 최소 버전. knowledge_chunks 가 이걸 요구한다. */
    static final int REQUIRED_MAJOR = 11;
    static final int REQUIRED_MINOR = 8;

    @Bean
    FlywayMigrationStrategy flywayVersionGuard() {
        return flyway -> {
            requireSupportedServer(flyway);
            flyway.migrate();
        };
    }

    private void requireSupportedServer(Flyway flyway) {
        DataSource dataSource = flyway.getConfiguration().getDataSource();
        String version;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT VERSION()")) {
            resultSet.next();
            version = resultSet.getString(1);
        } catch (SQLException e) {
            throw new IllegalStateException("DB 버전을 확인하지 못해 마이그레이션을 중단했습니다", e);
        }

        if (!isSupported(version)) {
            throw new IllegalStateException(
                    "마이그레이션을 중단했습니다. MariaDB " + REQUIRED_MAJOR + "." + REQUIRED_MINOR
                            + " 이상이 필요한데 서버는 " + version + " 입니다."
                            + " knowledge_chunks 의 VECTOR 타입이 그 아래 버전에는 없습니다."
                            + " MariaDB 는 DDL 이 트랜잭션이 아니라 도중에 실패하면 절반만 만들어진"
                            + " 스키마가 남으므로, 시작하기 전에 멈춥니다."
                            + " 업그레이드 절차는 README 의 'DB 준비' 를 보세요.");
        }
    }

    /** {@code 10.11.13-MariaDB-0ubuntu0.24.04.1-log} 같은 문자열에서 앞의 두 자리만 본다. */
    static boolean isSupported(String version) {
        if (version == null) {
            return false;
        }
        String[] parts = version.split("[.\\-]");
        if (parts.length < 2) {
            return false;
        }
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            return major > REQUIRED_MAJOR || (major == REQUIRED_MAJOR && minor >= REQUIRED_MINOR);
        } catch (NumberFormatException e) {
            // 형식을 못 읽으면 통과시키지 않는다 — 모르는 채로 DDL 을 거는 것보다 낫다.
            return false;
        }
    }
}
