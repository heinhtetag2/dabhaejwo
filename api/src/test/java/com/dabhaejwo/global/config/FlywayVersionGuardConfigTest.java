package com.dabhaejwo.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 버전 판정은 실제 사고를 막는 장치다. 실제로 10.11 서버에 마이그레이션이 걸려
 * 17개 테이블이 반쯤 만들어진 적이 있다. 문자열 형태를 테스트로 고정한다.
 */
class FlywayVersionGuardConfigTest {

    @Test
    @DisplayName("사고가 났던 그 버전 문자열을 거부한다")
    void rejectsTheVersionThatCausedTheIncident() {
        assertFalse(FlywayVersionGuardConfig.isSupported("10.11.13-MariaDB-0ubuntu0.24.04.1-log"));
    }

    @Test
    @DisplayName("11.8 이상은 통과한다")
    void acceptsRequiredAndNewer() {
        assertTrue(FlywayVersionGuardConfig.isSupported("11.8.0-MariaDB"));
        assertTrue(FlywayVersionGuardConfig.isSupported("11.8.3-MariaDB-1:11.8.3+maria~ubu2404"));
        assertTrue(FlywayVersionGuardConfig.isSupported("11.9.1-MariaDB"));
        assertTrue(FlywayVersionGuardConfig.isSupported("12.0.0-MariaDB"));
    }

    @Test
    @DisplayName("같은 메이저의 낮은 마이너는 거부한다")
    void rejectsLowerMinorOnSameMajor() {
        assertFalse(FlywayVersionGuardConfig.isSupported("11.7.2-MariaDB"));
        assertFalse(FlywayVersionGuardConfig.isSupported("11.0.0-MariaDB"));
    }

    @Test
    @DisplayName("읽을 수 없는 값은 통과시키지 않는다 — 모르는 채로 DDL 을 걸지 않는다")
    void rejectsUnparseable() {
        assertFalse(FlywayVersionGuardConfig.isSupported(null));
        assertFalse(FlywayVersionGuardConfig.isSupported(""));
        assertFalse(FlywayVersionGuardConfig.isSupported("unknown"));
        assertFalse(FlywayVersionGuardConfig.isSupported("8.0.36-MySQL"));
    }
}
