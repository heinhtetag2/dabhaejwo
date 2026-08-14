package com.dabhaejwo.domain.bot.service;

import com.dabhaejwo.domain.bot.dto.request.BotSaveRequest;
import com.dabhaejwo.domain.bot.dto.response.BotResponse;
import com.dabhaejwo.domain.bot.entity.Bot;
import com.dabhaejwo.domain.bot.repository.BotRepository;
import com.dabhaejwo.domain.plan.entity.Plan;
import com.dabhaejwo.domain.plan.repository.PlanRepository;
import com.dabhaejwo.domain.tenant.entity.AllowedOrigin;
import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.repository.AllowedOriginRepository;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.global.common.HostName;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.CurrentAuth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 서비스 관리 — 업체가 스스로 한다.
 *
 * <p>운영자를 기다리지 않는다. 주말에 서비스를 하나 올리려는 업체가 월요일까지 기다려야 하면
 * 그건 기능이 아니라 신청서다.
 *
 * <p>기획: {@code docs/plan/service-plan.md}
 */
@Service
public class BotService {

    private final BotRepository botRepository;
    private final BotProvisioner provisioner;
    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;
    private final AllowedOriginRepository originRepository;

    public BotService(BotRepository botRepository,
                      BotProvisioner provisioner,
                      TenantRepository tenantRepository,
                      PlanRepository planRepository,
                      AllowedOriginRepository originRepository) {
        this.botRepository = botRepository;
        this.provisioner = provisioner;
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
        this.originRepository = originRepository;
    }

    /**
     * 서비스 목록. 전 역할이 본다 — 어느 서비스를 보고 있는지는 권한 문제가 아니다.
     *
     * <p>{@code lastCalledAt} 을 함께 준다. 화면의 작동 상태 점이 이 값으로 결정되는데,
     * 목록마다 따로 부르면 서비스 수만큼 왕복이 는다.
     */
    @Transactional(readOnly = true)
    public List<BotResponse> list() {
        UUID tenantId = CurrentAuth.tenantUser().tenantId();
        List<Bot> bots = botRepository.findAllByTenantIdOrderByCreatedAtAsc(tenantId);
        Map<UUID, OffsetDateTime> lastCalled = lastCalledByBot(bots);
        return bots.stream()
                .map(bot -> BotResponse.from(bot, lastCalled.get(bot.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public BotResponse get(UUID botId) {
        Bot bot = find(botId);
        return BotResponse.from(bot, lastCalledByBot(List.of(bot)).get(bot.getId()));
    }

    /**
     * 서비스 추가.
     *
     * <p><b>업체 행을 먼저 잠근다.</b> "세고 나서 만드는" 사이에 다른 요청이 끼면 상한을
     * 넘겨 만들 수 있다. 사람이 대시보드에서 누르는 드문 행위라 잠금 비용이 사실상 0이다.
     *
     * <p>삭제 유예 중인 서비스도 센다 — 지우자마자 새로 만들어 유예를 우회하지 못하게.
     */
    @Transactional
    public BotResponse create(BotSaveRequest request) {
        CurrentAuth.requireOwner();
        CurrentAuth.rejectIfImpersonating();

        UUID tenantId = CurrentAuth.tenantUser().tenantId();
        Tenant tenant = tenantRepository.findByIdForUpdate(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND));
        Plan plan = planRepository.findById(tenant.getPlanId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

        long current = botRepository.countByTenantId(tenantId);
        if (current >= plan.getBotLimit()) {
            throw new BusinessException(ErrorCode.BOT_LIMIT_REACHED,
                    plan.getName() + " 요금제는 서비스 " + plan.getBotLimit()
                            + "개까지 만들 수 있습니다. 요금제를 올리면 더 만들 수 있습니다");
        }

        String host = requireHost(request.primaryDomain());
        requireUniqueName(tenantId, request.name().strip(), null);

        // 첫 서비스와 같은 경로로 만든다 — 여기서 손수 만들면 가입 흐름과 갈린다.
        Bot bot = provisioner.provision(tenantId, request.name(), host, provisioner.issueKey(), false);
        return BotResponse.from(bot, null);
    }

    /**
     * 이름·대표 도메인 변경.
     *
     * <p><b>허용 주소를 건드리지 않는다.</b> 대표 도메인은 표시용이고, 위젯이 실제로 도는
     * 주소는 허용 목록이 정한다 — 여기서 같이 바꾸면 잘 돌던 위젯이 이름만 고쳤는데 죽는다.
     */
    @Transactional
    public BotResponse update(UUID botId, BotSaveRequest request) {
        CurrentAuth.requireEditor();
        Bot bot = find(botId);
        requireUniqueName(bot.getTenantId(), request.name().strip(), botId);
        bot.rename(request.name(), requireHost(request.primaryDomain()));
        return BotResponse.from(bot, lastCalledByBot(List.of(bot)).get(bot.getId()));
    }

    private Bot find(UUID botId) {
        return botRepository.findByIdAndTenantId(botId, CurrentAuth.tenantUser().tenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BOT_NOT_FOUND));
    }

    private String requireHost(String raw) {
        String host = HostName.normalize(raw);
        if (host.isBlank() || !host.contains(".")) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "주소 형식이 올바르지 않습니다. 예: shop.example.com");
        }
        return host;
    }

    /**
     * 같은 이름이 둘이면 서비스 선택기에서 구분할 수 없다.
     *
     * <p>DB 에도 유니크가 걸려 있지만 여기서 먼저 본다 — 제약 위반은 500 으로 새어 나가고,
     * 업체는 무엇이 잘못됐는지 알 수 없다.
     */
    private void requireUniqueName(UUID tenantId, String name, UUID exceptBotId) {
        boolean taken = botRepository.findAllByTenantIdOrderByCreatedAtAsc(tenantId).stream()
                .filter(bot -> exceptBotId == null || !bot.getId().equals(exceptBotId))
                .anyMatch(bot -> bot.getName().equalsIgnoreCase(name));
        if (taken) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "같은 이름의 서비스가 이미 있습니다");
        }
    }

    /** 서비스마다 "가장 최근에 위젯이 부른 시각". 주소가 여럿이면 그중 가장 최근 것이다. */
    private Map<UUID, OffsetDateTime> lastCalledByBot(List<Bot> bots) {
        return bots.stream()
                .flatMap(bot -> originRepository.findAllByBotId(bot.getId()).stream())
                .filter(origin -> origin.getLastCalledAt() != null)
                .collect(Collectors.toMap(
                        AllowedOrigin::getBotId,
                        AllowedOrigin::getLastCalledAt,
                        // 주소가 여럿이면 가장 최근 호출이 그 서비스의 신호다.
                        (a, b) -> a.isAfter(b) ? a : b));
    }
}
