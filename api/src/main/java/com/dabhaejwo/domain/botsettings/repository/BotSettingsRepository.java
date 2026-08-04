package com.dabhaejwo.domain.botsettings.repository;

import com.dabhaejwo.domain.botsettings.entity.BotSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BotSettingsRepository extends JpaRepository<BotSettings, UUID> {
}
