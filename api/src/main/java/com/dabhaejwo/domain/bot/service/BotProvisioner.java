package com.dabhaejwo.domain.bot.service;

import com.dabhaejwo.domain.bot.entity.Bot;
import com.dabhaejwo.domain.bot.repository.BotRepository;
import com.dabhaejwo.domain.botsettings.entity.BotSettings;
import com.dabhaejwo.domain.botsettings.repository.BotSettingsRepository;
import com.dabhaejwo.domain.tenant.entity.AllowedOrigin;
import com.dabhaejwo.domain.tenant.repository.AllowedOriginRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * 서비스를 만드는 <b>유일한 경로</b>.
 *
 * <p>가입(첫 서비스)과 서비스 추가가 각자 만들면 반드시 갈린다 — 한쪽만 기본 설정을 넣거나
 * 한쪽만 허용 주소를 등록하는 식이다. 실제로 가입 흐름이 세 줄로 손수 만들고 있었다.
 *
 * <p>한 서비스가 성립하려면 셋이 함께 있어야 한다:
 * <ul>
 *   <li>{@code bots} — 정체성과 위젯 키
 *   <li>{@code bot_settings} — 설정이 없는 상태를 화면이 다루지 않아도 되게
 *   <li>{@code allowed_origins} — 없으면 위젯이 어디에서도 뜨지 않는다
 * </ul>
 *
 * <p>요금제 상한 검사는 여기 두지 않는다 — 가입은 상한과 무관하게 첫 서비스를 만들어야 한다.
 * 검사는 "서비스 추가" 경로가 한다.
 */
@Service
public class BotProvisioner {

    private static final String KEY_PREFIX = "pk_live_";
    private static final String KEY_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int KEY_LENGTH = 12;
    private static final int KEY_ATTEMPTS = 5;

    private final BotRepository botRepository;
    private final BotSettingsRepository botSettingsRepository;
    private final AllowedOriginRepository originRepository;
    private final SecureRandom random = new SecureRandom();

    public BotProvisioner(BotRepository botRepository,
                          BotSettingsRepository botSettingsRepository,
                          AllowedOriginRepository originRepository) {
        this.botRepository = botRepository;
        this.botSettingsRepository = botSettingsRepository;
        this.originRepository = originRepository;
    }

    /**
     * 서비스 한 벌을 만든다.
     *
     * @param key 미리 발급한 공개 키. 가입 흐름이 {@code tenants} 에도 같은 값을 적어야 해서
     *            밖에서 받는다 — 그 컬럼은 V16 이후 유물이지만 아직 NOT NULL 이다
     */
    @Transactional
    public Bot provision(UUID tenantId, String name, String host, String key, boolean isDefault) {
        Bot bot = botRepository.save(Bot.of(tenantId, name, host, key, isDefault));
        botSettingsRepository.save(BotSettings.defaults(bot.scope(), bot.getName()));
        originRepository.save(AllowedOrigin.of(bot.scope(), host));
        return bot;
    }

    /**
     * 공개 키. 남의 사이트 소스에 그대로 노출되므로 추측 가능성이 낮아야 한다 —
     * 순번이나 시각 기반이면 다른 서비스의 키를 만들어낼 수 있다.
     *
     * <p>중복은 사실상 나지 않지만({@code 36^12}), 났을 때 조용히 넘어가면 두 서비스가
     * 같은 키를 갖게 되고 <b>위젯이 어느 쪽인지 정할 수 없다.</b> 확인하고 다시 뽑는다.
     */
    @Transactional(readOnly = true)
    public String issueKey() {
        for (int attempt = 0; attempt < KEY_ATTEMPTS; attempt++) {
            StringBuilder key = new StringBuilder(KEY_PREFIX);
            for (int i = 0; i < KEY_LENGTH; i++) {
                key.append(KEY_ALPHABET.charAt(random.nextInt(KEY_ALPHABET.length())));
            }
            String candidate = key.toString();
            if (botRepository.findByPublishableKey(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("공개 키를 발급하지 못했습니다");
    }
}
