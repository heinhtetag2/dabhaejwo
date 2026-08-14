package com.dabhaejwo.domain.botsettings.repository;

import com.dabhaejwo.domain.botsettings.entity.BotSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BotSettingsRepository extends JpaRepository<BotSettings, UUID> {

    /**
     * 설정은 서비스에 속한다.
     *
     * <p>{@code findById(tenantId)} 를 쓰지 않는다 — PK 가 아직 {@code tenant_id} 라 컴파일은
     * 되지만, 서비스가 둘이 되는 순간 <b>둘 중 아무 설정이나</b> 돌려주게 된다.
     */
    Optional<BotSettings> findByBotId(UUID botId);
}
