package com.dabhaejwo.domain.auth.service;

import com.dabhaejwo.domain.auth.dto.request.SignupRequest;
import com.dabhaejwo.domain.auth.dto.response.AppLoginResponse;
import com.dabhaejwo.domain.botsettings.entity.BotSettings;
import com.dabhaejwo.domain.botsettings.repository.BotSettingsRepository;
import com.dabhaejwo.domain.member.dto.response.MemberResponse;
import com.dabhaejwo.domain.member.entity.TenantMember;
import com.dabhaejwo.domain.member.repository.TenantMemberRepository;
import com.dabhaejwo.domain.notification.service.NotificationEvents;
import com.dabhaejwo.domain.plan.entity.Plan;
import com.dabhaejwo.domain.plan.repository.PlanRepository;
import com.dabhaejwo.domain.tenant.entity.AllowedOrigin;
import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.repository.AllowedOriginRepository;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.JwtProvider;
import com.dabhaejwo.global.security.TenantMemberRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * 가입.
 *
 * <p><b>하나라도 실패하면 전부 되돌린다.</b> 업체는 있는데 담당자가 없거나, 담당자는 있는데
 * 허용 주소가 없는 상태가 만들어지면 그 계정은 아무것도 할 수 없고 본인은 이유를 모른다.
 * 단일 트랜잭션으로 묶는다 (tenant-public-plan.md §4.3).
 */
@Service
public class SignupService {

    /** 무료 체험 기간. 사이트를 학습시키고 답변을 다듬으려면 주말이 두 번 필요하다 (§5.1). */
    private static final int TRIAL_DAYS = 14;
    private static final String TRIAL_PLAN_CODE = "TRIAL";
    private static final String KEY_PREFIX = "pk_live_";
    /** 소문자·숫자만. 대소문자를 섞으면 업체가 옮겨 적을 때 틀린다. */
    private static final String KEY_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int KEY_LENGTH = 12;

    private final SecureRandom random = new SecureRandom();

    private final TenantRepository tenantRepository;
    private final TenantMemberRepository memberRepository;
    private final PlanRepository planRepository;
    private final BotSettingsRepository botSettingsRepository;
    private final AllowedOriginRepository originRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final NotificationEvents notificationEvents;

    public SignupService(TenantRepository tenantRepository,
                         TenantMemberRepository memberRepository,
                         PlanRepository planRepository,
                         BotSettingsRepository botSettingsRepository,
                         AllowedOriginRepository originRepository,
                         PasswordEncoder passwordEncoder,
                         JwtProvider jwtProvider,
                         NotificationEvents notificationEvents) {
        this.tenantRepository = tenantRepository;
        this.memberRepository = memberRepository;
        this.planRepository = planRepository;
        this.botSettingsRepository = botSettingsRepository;
        this.originRepository = originRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.notificationEvents = notificationEvents;
    }

    @Transactional
    public AppLoginResponse signup(SignupRequest request) {
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        String host = AllowedOrigin.normalizeHost(request.primaryDomain());

        if (host.isBlank() || !host.contains(".")) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "홈페이지 주소 형식이 올바르지 않습니다. 예: shop.example.com");
        }
        if (!memberRepository.findAllByEmail(email).isEmpty()) {
            // 이미 가입된 이메일이라는 사실은 알려준다. 로그인하면 되는 상황인데
            // 숨기면 사용자가 같은 시도를 반복한다.
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "이미 가입된 이메일입니다. 로그인해 주세요");
        }

        Plan trial = planRepository.findByCode(TRIAL_PLAN_CODE)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND,
                        "무료 체험 요금제가 등록되어 있지 않습니다"));

        Tenant tenant = tenantRepository.save(Tenant.startTrial(
                request.tenantName().strip(), host, issueKey(), trial.getId(), TRIAL_DAYS));

        TenantMember owner = memberRepository.save(TenantMember.active(
                tenant.getId(), email, null, TenantMemberRole.OWNER,
                passwordEncoder.encode(request.password())));

        botSettingsRepository.save(BotSettings.defaults(tenant.getId(), tenant.getName()));
        originRepository.save(AllowedOrigin.of(tenant.getId(), host));

        // 가입은 영업이 가장 먼저 알아야 할 사건이다. 같은 트랜잭션에 둔다 —
        // 가입이 실패해 되감기면 알림도 남아서는 안 된다.
        notificationEvents.tenantSignedUp(tenant.getId(), tenant.getName(), trial.getName());

        // 가입 직후 로그인 상태로 대시보드에 도착한다. 다시 로그인시키지 않는다.
        return new AppLoginResponse(
                jwtProvider.issueTenantToken(owner.getId(), tenant.getId(), owner.getRole()),
                jwtProvider.issueRefreshToken(owner.getId(), AppAuthService.SCOPE_APP),
                MemberResponse.from(owner));
    }

    /**
     * 공개 키. 남의 사이트 소스에 그대로 노출되므로 추측 가능성이 낮아야 한다 —
     * 순번이나 시각 기반이면 다른 업체의 키를 만들어낼 수 있다.
     *
     * <p>중복은 사실상 나지 않지만({@code 36^12}), 났을 때 조용히 넘어가면 두 업체가
     * 같은 키를 갖게 되므로 확인하고 다시 뽑는다.
     */
    private String issueKey() {
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder key = new StringBuilder(KEY_PREFIX);
            for (int i = 0; i < KEY_LENGTH; i++) {
                key.append(KEY_ALPHABET.charAt(random.nextInt(KEY_ALPHABET.length())));
            }
            String candidate = key.toString();
            if (tenantRepository.findByPublishableKey(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("공개 키를 발급하지 못했습니다");
    }
}
