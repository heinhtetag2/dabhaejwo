package com.dabhaejwo.global.crypto;

import com.dabhaejwo.global.config.AppProperties;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 공급사 API 키를 감싸는 암호다. 여기가 틀리면 키가 평문으로 남거나, 조작된 키가
 * 그대로 쓰인다 — 둘 다 조용히 일어나므로 테스트로 고정한다.
 */
class SecretCipherTest {

    /** 쓰는 것만 넘긴다 — AppProperties 에 항목이 늘어도 이 테스트는 안 깨진다. */
    private SecretCipher cipher(String key) {
        return new SecretCipher(new AppProperties.Security(key));
    }

    @Test
    @DisplayName("암호화한 값을 다시 복호화하면 원문이 나온다")
    void roundTrip() {
        SecretCipher cipher = cipher("test-master-key");

        String encrypted = cipher.encrypt("AIzaSyExampleKey1234");

        assertEquals("AIzaSyExampleKey1234", cipher.decrypt(encrypted));
    }

    @Test
    @DisplayName("같은 값을 두 번 암호화해도 암호문이 다르다 — IV 가 매번 새로 만들어진다")
    void usesFreshIv() {
        SecretCipher cipher = cipher("test-master-key");

        // 같은 암호문이 나오면 "이 두 업체가 같은 키를 쓴다"가 DB 만 봐도 드러난다.
        assertNotEquals(cipher.encrypt("same-value"), cipher.encrypt("same-value"));
    }

    @Test
    @DisplayName("암호문이 조작되면 복호화가 실패한다 — 조용히 성공하지 않는다")
    void detectsTampering() {
        SecretCipher cipher = cipher("test-master-key");
        byte[] raw = Base64.getDecoder().decode(cipher.encrypt("AIzaSyExampleKey1234"));
        // 마지막 바이트를 하나 뒤집는다. GCM 태그가 이걸 잡아야 한다.
        raw[raw.length - 1] ^= 0x01;

        BusinessException error = assertThrows(BusinessException.class,
                () -> cipher.decrypt(Base64.getEncoder().encodeToString(raw)));

        assertEquals(ErrorCode.CREDENTIAL_UNREADABLE, error.errorCode());
    }

    @Test
    @DisplayName("다른 마스터 키로는 복호화할 수 없다")
    void wrongMasterKeyFails() {
        String encrypted = cipher("key-one").encrypt("AIzaSyExampleKey1234");

        assertThrows(BusinessException.class, () -> cipher("key-two").decrypt(encrypted));
    }

    @Test
    @DisplayName("마스터 키가 없으면 평문으로 떨어지지 않고 거부한다")
    void refusesWithoutMasterKey() {
        SecretCipher cipher = cipher("");

        assertFalse(cipher.available());
        BusinessException error = assertThrows(BusinessException.class,
                () -> cipher.encrypt("AIzaSyExampleKey1234"));
        assertEquals(ErrorCode.ENCRYPTION_UNAVAILABLE, error.errorCode());
    }

    @Test
    @DisplayName("힌트는 앞뒤 4자만 남기고, 짧은 값은 통째로 가린다")
    void hintMasksKey() {
        SecretCipher cipher = cipher("test-master-key");

        assertEquals("AIza…1234", cipher.hint("AIzaSyExampleKey1234"));
        // 8자짜리를 4+4 로 보여주면 전부 보여주는 셈이다.
        assertEquals("••••", cipher.hint("short123"));
        assertTrue(cipher.hint(null).startsWith("•"));
    }
}
