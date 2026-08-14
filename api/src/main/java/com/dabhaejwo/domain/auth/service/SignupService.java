package com.dabhaejwo.domain.auth.service;

import com.dabhaejwo.domain.bot.service.BotProvisioner;
import com.dabhaejwo.domain.auth.dto.request.SignupRequest;
import com.dabhaejwo.domain.auth.dto.response.AppLoginResponse;
import com.dabhaejwo.domain.member.dto.response.MemberResponse;
import com.dabhaejwo.domain.member.entity.TenantMember;
import com.dabhaejwo.domain.member.repository.TenantMemberRepository;
import com.dabhaejwo.domain.notification.service.NotificationEvents;
import com.dabhaejwo.domain.plan.entity.Plan;
import com.dabhaejwo.domain.plan.repository.PlanRepository;
import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.JwtProvider;
import com.dabhaejwo.global.security.TenantMemberRole;
import com.dabhaejwo.global.common.HostName;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    /** 소문자·숫자만. 대소문자를 섞으면 업체가 옮겨 적을 때 틀린다. */


    private final TenantRepository tenantRepository;
    private final TenantMemberRepository memberRepository;
    private final BotProvisioner botProvisioner;
    private final PlanRepository planRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final NotificationEvents notificationEvents;

    public SignupService(TenantRepository tenantRepository,
                         TenantMemberRepository memberRepository,
                         BotProvisioner botProvisioner,
                         PlanRepository planRepository,
                         PasswordEncoder passwordEncoder,
                         JwtProvider jwtProvider,
                         NotificationEvents notificationEvents) {
        this.tenantRepository = tenantRepository;
        this.memberRepository = memberRepository;
        this.botProvisioner = botProvisioner;
        this.planRepository = planRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.notificationEvents = notificationEvents;
    }

    @Transactional
    public AppLoginResponse signup(SignupRequest request) {
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        String host = HostName.normalize(request.primaryDomain());

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

        /*
         * 키를 먼저 뽑는다 — `tenants.publishable_key` 가 아직 NOT NULL 이라 업체 행에도
         * 같은 값을 적어야 한다. 그 컬럼은 V16 이후 **읽지도 갱신하지도 않는 유물**이고
         * (진실은 `bots.publishable_key`), 두 번째 서비스를 만들어도 건드리지 않는다.
         */
        String key = botProvisioner.issueKey();
        Tenant tenant = tenantRepository.save(Tenant.startTrial(
                request.tenantName().strip(), host, key, trial.getId(), TRIAL_DAYS));

        TenantMember owner = memberRepository.save(TenantMember.active(
                tenant.getId(), email, null, TenantMemberRole.OWNER,
                passwordEncoder.encode(request.password())));

        // 첫 서비스. 설정·허용 주소까지 한 곳에서 만든다 — 여기서 손수 만들면
        // "서비스 추가" 경로와 반드시 갈린다.
        botProvisioner.provision(tenant.getId(), tenant.getName(), host, key, true);

        // 가입은 영업이 가장 먼저 알아야 할 사건이다. 같은 트랜잭션에 둔다 —
        // 가입이 실패해 되감기면 알림도 남아서는 안 된다.
        notificationEvents.tenantSignedUp(tenant.getId(), tenant.getName(), trial.getName());

        // 가입 직후 로그인 상태로 대시보드에 도착한다. 다시 로그인시키지 않는다.
        return new AppLoginResponse(
                jwtProvider.issueTenantToken(owner.getId(), tenant.getId(), owner.getRole()),
                jwtProvider.issueRefreshToken(owner.getId(), AppAuthService.SCOPE_APP),
                MemberResponse.from(owner));
    }

}
