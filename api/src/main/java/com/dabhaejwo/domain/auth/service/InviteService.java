package com.dabhaejwo.domain.auth.service;

import com.dabhaejwo.domain.auth.dto.request.InviteAcceptRequest;
import com.dabhaejwo.domain.auth.dto.response.InvitePreviewResponse;
import com.dabhaejwo.domain.member.entity.TenantMember;
import com.dabhaejwo.domain.member.repository.TenantMemberRepository;
import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.global.config.AppProperties;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.notify.Greeting;
import com.dabhaejwo.global.notify.Mailer;
import com.dabhaejwo.global.notify.MailTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 팀원 초대 링크.
 *
 * <p>초대는 <b>메일이 나가야 완결된다.</b> 행만 만들고 메일이 실패하면 초대한 사람은
 * 상대가 링크를 못 받은 사실을 영영 모른다 — 그래서 발송 실패는 초대 전체를 롤백시킨다.
 *
 * <p>토큰은 <b>해시로만 저장한다.</b> 원문은 메일에만 실린다. 유출된 DB 로 남의 계정을
 * 만들 수 있으면 초대 링크는 그냥 백도어다.
 *
 * <p>토큰 해시에 BCrypt 를 쓰지 않는 이유는 <b>토큰으로 행을 찾아야</b> 하기 때문이다.
 * BCrypt 는 같은 입력에도 매번 다른 값이 나와 조회 조건으로 쓸 수 없다. 대신 토큰이
 * 32바이트 난수라 사전 공격이 성립하지 않으므로 SHA-256 으로 충분하다.
 */
@Service
public class InviteService {

    private static final Logger log = LoggerFactory.getLogger(InviteService.class);

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TenantMemberRepository memberRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final Mailer mailer;
    private final AppProperties.Mail mailConfig;
    private final AppProperties.Invite inviteConfig;

    public InviteService(TenantMemberRepository memberRepository,
                         TenantRepository tenantRepository,
                         PasswordEncoder passwordEncoder,
                         Mailer mailer,
                         AppProperties properties) {
        this.memberRepository = memberRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailer = mailer;
        this.mailConfig = properties.mail();
        this.inviteConfig = properties.invite();
    }

    /**
     * 초대 링크를 만들어 메일로 보낸다. 다시 부르면 이전 링크는 무효가 된다.
     *
     * <p>{@code MemberService} 가 같은 트랜잭션에서 호출한다 — 메일이 실패하면 팀원 행도 함께 사라진다.
     */
    public void sendInvite(TenantMember member, String inviterName) {
        String token = randomToken();
        member.attachInviteToken(sha256(token), inviteConfig.ttlHours());

        Tenant tenant = tenantRepository.findById(member.getTenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND));

        String link = mailConfig.appBaseUrl() + "/invite?token=" + token;
        mailer.send(member.getEmail(), "[답해줘] %s 팀에 초대되었습니다".formatted(tenant.getName()),
                inviteBody(Greeting.of(member.getName(), member.getEmail()),
                        tenant.getName(), inviterName, link, inviteConfig.ttlHours()));

        log.info("초대 메일을 보냈습니다 — tenant={}, email={}", member.getTenantId(), member.getEmail());
    }

    /**
     * 링크를 열었을 때 보여줄 정보.
     *
     * <p>비밀번호를 정하기 전에 <b>어디에 초대됐는지</b>는 알려줘야 한다 — 모르는 업체 이름이
     * 뜨면 잘못 온 것이고, 그걸 비밀번호를 만든 뒤에 알면 늦다.
     */
    @Transactional(readOnly = true)
    public InvitePreviewResponse preview(String token) {
        TenantMember member = findByToken(token);
        Tenant tenant = tenantRepository.findById(member.getTenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITE_INVALID));

        return new InvitePreviewResponse(
                tenant.getName(), member.getEmail(), member.getName(), member.getRole());
    }

    /** 비밀번호를 정하고 초대를 수락한다. 토큰은 한 번 쓰고 버린다. */
    @Transactional
    public void accept(InviteAcceptRequest request) {
        TenantMember member = findByToken(request.token());
        member.acceptInvite(passwordEncoder.encode(request.password()));
        log.info("초대를 수락했습니다 — tenant={}, email={}", member.getTenantId(), member.getEmail());
    }

    private TenantMember findByToken(String token) {
        return memberRepository.findByInviteTokenHash(sha256(token))
                .filter(TenantMember::inviteUsable)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITE_INVALID));
    }

    /** URL 에 그대로 실리므로 URL-safe 로 만든다. 패딩(=)은 뺀다 — 링크가 깨져 보인다. */
    private String randomToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 을 쓸 수 없습니다", e);
        }
    }

    private MailTemplate.Body inviteBody(String name, String tenantName, String inviterName,
                                        String link, int ttlHours) {
        return MailTemplate.build(
                name,
                "'%s' 팀에 초대되었습니다".formatted(tenantName),
                "%s님이 챗봇 관리에 회원님을 초대했습니다. 아래에서 비밀번호를 정하시면 바로 시작할 수 있습니다."
                        .formatted(inviterName),
                null,
                new MailTemplate.Cta("비밀번호 정하고 시작하기", link),
                java.util.List.of(
                        "이 링크는 " + (ttlHours / 24) + "일 동안 유효하며 한 번만 쓸 수 있습니다.",
                        "초대를 요청한 적이 없다면 이 메일을 무시해 주세요."));
    }
}
