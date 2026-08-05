package com.dabhaejwo.domain.provider.entity;

import com.dabhaejwo.global.llm.LlmProviderName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 공급사 API 키. 공급사당 한 행이며 {@code provider} 가 곧 PK 다.
 *
 * <p><b>암호문만 갖는다.</b> 평문을 필드로 들고 있지 않으므로 실수로 응답 DTO 에 실리거나
 * 로그에 찍힐 여지가 없다. 복호화는 쓰는 순간에만 {@code SecretCipher} 로 한다.
 *
 * <p>{@code toString} 을 만들지 않는 것도 의도다 — 엔티티를 통째로 로그에 찍는 습관이
 * 암호문을 흘리게 두지 않는다.
 */
@Entity
@Table(name = "provider_credentials")
public class ProviderCredential {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LlmProviderName provider;

    @Column(name = "api_key_cipher", nullable = false)
    private String apiKeyCipher;

    /** 화면 표시용. 이것만으로는 키를 복원할 수 없다. */
    @Column(name = "key_hint", nullable = false)
    private String keyHint;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ProviderCredential() {
    }

    public static ProviderCredential register(LlmProviderName provider,
                                              String cipher,
                                              String hint,
                                              UUID operatorId) {
        ProviderCredential credential = new ProviderCredential();
        credential.provider = provider;
        credential.apiKeyCipher = cipher;
        credential.keyHint = hint;
        credential.enabled = true;
        credential.updatedBy = operatorId;
        credential.createdAt = OffsetDateTime.now();
        credential.updatedAt = credential.createdAt;
        return credential;
    }

    /** 키 교체. 이전 키는 남기지 않는다 — 폐기한 키를 보관할 이유가 없다. */
    public void replaceKey(String cipher, String hint, UUID operatorId) {
        this.apiKeyCipher = cipher;
        this.keyHint = hint;
        this.updatedBy = operatorId;
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * 사용 중지. 키를 지우지 않고 끄기만 한다 — 잠깐 꺼 두고 되돌리는 경우가 있고,
     * 지우면 다시 발급받아 넣어야 한다.
     */
    public void changeEnabled(boolean next, UUID operatorId) {
        this.enabled = next;
        this.updatedBy = operatorId;
        this.updatedAt = OffsetDateTime.now();
    }

    public LlmProviderName getProvider() {
        return provider;
    }

    public String getApiKeyCipher() {
        return apiKeyCipher;
    }

    public String getKeyHint() {
        return keyHint;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
