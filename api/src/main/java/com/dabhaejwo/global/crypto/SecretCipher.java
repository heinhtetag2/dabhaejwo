package com.dabhaejwo.global.crypto;

import com.dabhaejwo.global.config.AppProperties;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 저장용 대칭 암호. 공급사 API 키처럼 <b>다시 읽어야 하는</b> 비밀에만 쓴다.
 *
 * <p>비밀번호는 여기를 쓰지 않는다 — 되돌릴 수 없어야 하므로 BCrypt 단방향 해시다.
 *
 * <p>AES-256-GCM 을 쓰는 이유는 기밀성과 <b>무결성</b>을 함께 얻기 위해서다. CBC 같은
 * 모드는 암호문이 조작돼도 복호화가 그럴듯하게 성공한다 — DB 를 쓸 수 있는 공격자가
 * 키를 자기 것으로 바꿔치기해도 알아채지 못한다.
 *
 * <p><b>마스터 키가 없으면 조용히 평문으로 떨어지지 않고 거부한다.</b> 그 편이 안전한
 * 것처럼 보이는 상태로 운영에 나가는 것보다 낫다.
 */
@Component
public class SecretCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;

    /**
     * 스프링이 쓰는 생성자.
     *
     * <p><b>{@code @Autowired} 를 지우지 말 것.</b> 생성자가 둘 이상이면 스프링은 어느 것을
     * 쓸지 고르지 못하고 기본 생성자를 찾다가 기동에 실패한다
     * ({@code No default constructor found}) — 아래 테스트용 생성자가 생겼을 때 실제로 났다.
     * 생성자가 하나뿐일 때만 자동으로 선택된다.
     */
    @Autowired
    public SecretCipher(AppProperties properties) {
        this(properties.security());
    }

    /**
     * 쓰는 것만 받는 형태.
     *
     * <p>{@code AppProperties} 전체를 받으면 테스트가 <b>모든 컴포넌트를 위치로 채워야</b> 하고,
     * 설정 항목이 늘 때마다 관계없는 테스트가 깨진다(실제로 세 번 깨졌다).
     *
     * <p>package-private 다 — public 이면 생성자가 둘이 되어 Spring 이 어느 쪽으로
     * 빈을 만들지 못한다. 같은 패키지의 테스트만 쓴다.
     */
    SecretCipher(AppProperties.Security security) {
        this.key = resolveKey(security == null ? null : security.encryptionKey());
    }

    /**
     * 마스터 키. 길이를 맞추기 위해 SHA-256 으로 한 번 접는다 — 운영자가 32바이트를
     * 정확히 맞춰 넣게 강요하지 않기 위해서다.
     *
     * @return 설정이 비어 있으면 null. 그때는 쓰는 시점에 거부한다
     */
    private SecretKey resolveKey(String configured) {
        if (configured == null || configured.isBlank()) {
            return null;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(configured.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(digest, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("암호화 키를 준비하지 못했습니다", e);
        }
    }

    public boolean available() {
        return key != null;
    }

    /** @return {@code base64(iv || ciphertext || tag)} */
    public String encrypt(String plaintext) {
        requireKey();
        try {
            byte[] iv = new byte[IV_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("암호화에 실패했습니다", e);
        }
    }

    public String decrypt(String encoded) {
        requireKey();
        try {
            byte[] combined = Base64.getDecoder().decode(encoded);
            byte[] iv = new byte[IV_BYTES];
            System.arraycopy(combined, 0, iv, 0, IV_BYTES);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(
                    cipher.doFinal(combined, IV_BYTES, combined.length - IV_BYTES),
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 마스터 키가 바뀌었거나 암호문이 조작됐다. 어느 쪽이든 이 값을 쓰면 안 된다.
            throw new BusinessException(ErrorCode.CREDENTIAL_UNREADABLE,
                    "저장된 자격증명을 복호화할 수 없습니다. 암호화 키가 바뀌었는지 확인하세요");
        }
    }

    /**
     * 화면 표시용 힌트. 앞 4자 + 뒤 4자만 남긴다.
     *
     * <p>짧은 값은 통째로 가린다 — 8자짜리 키를 4+4 로 보여주면 전부 보여주는 셈이 된다.
     */
    public String hint(String plaintext) {
        if (plaintext == null || plaintext.length() < 12) {
            return "••••";
        }
        return plaintext.substring(0, 4) + "…" + plaintext.substring(plaintext.length() - 4);
    }

    private void requireKey() {
        if (key == null) {
            throw new BusinessException(ErrorCode.ENCRYPTION_UNAVAILABLE,
                    "ENCRYPTION_KEY 가 설정되지 않아 자격증명을 다룰 수 없습니다");
        }
    }
}
