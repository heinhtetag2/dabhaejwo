package com.dabhaejwo.domain.chat.controller;

import com.dabhaejwo.domain.chat.dto.request.AskRequest;
import com.dabhaejwo.domain.chat.dto.request.FaqAskRequest;
import com.dabhaejwo.domain.chat.dto.request.FeedbackRequest;
import com.dabhaejwo.domain.chat.dto.request.LeadRequest;
import com.dabhaejwo.domain.chat.dto.request.SessionStartRequest;
import com.dabhaejwo.domain.chat.dto.response.AnswerResponse;
import com.dabhaejwo.domain.chat.dto.response.WidgetSessionResponse;
import com.dabhaejwo.domain.chat.service.WidgetChatService;
import com.dabhaejwo.global.security.CurrentAuth;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * 방문자 위젯. 익명이며 공개 키 + Origin 으로 테넌트만 식별한다.
 *
 * <p><b>요청 본문의 어떤 값도 테넌트를 정하지 못한다.</b> 테넌트는 필터가 검증한 키에서만 온다 —
 * 본문에서 받으면 남의 업체를 사칭해 원가를 태울 수 있다.
 */
@RestController
@RequestMapping("/api/widget")
public class WidgetChatController {

    private final WidgetChatService chatService;

    public WidgetChatController(WidgetChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/session")
    public WidgetSessionResponse start(@Valid @RequestBody SessionStartRequest request,
                                       HttpServletRequest http) {
        return chatService.start(tenantId(), request.path(), visitorIpHash(http));
    }

    @PostMapping("/ask")
    public AnswerResponse ask(@Valid @RequestBody AskRequest request, HttpServletRequest http) {
        return chatService.ask(tenantId(), request, visitorIpHash(http));
    }

    @PostMapping("/faq/{id}")
    public AnswerResponse faq(@PathVariable UUID id,
                              @Valid @RequestBody FaqAskRequest request,
                              HttpServletRequest http) {
        return chatService.answerFaq(tenantId(), id, request.sessionId(), visitorIpHash(http));
    }

    @PostMapping("/feedback")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void feedback(@Valid @RequestBody FeedbackRequest request) {
        chatService.feedback(tenantId(), request);
    }

    @PostMapping("/lead")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, UUID> lead(@Valid @RequestBody LeadRequest request) {
        return Map.of("id", chatService.lead(tenantId(), request));
    }

    private UUID tenantId() {
        return CurrentAuth.visitor().tenantId();
    }

    /**
     * 방문자 IP 의 해시. 레이트 리밋 키이자 대화 기록의 방문자 구분자다.
     *
     * <p><b>원본 IP 를 저장하지 않는다.</b> 개인정보이고, 우리가 필요한 것은
     * "같은 사람인가"뿐이라 해시로 충분하다.
     *
     * <p>프록시 뒤라면 {@code X-Forwarded-For} 의 <b>맨 앞</b>이 실제 방문자다.
     * 이 값은 위조 가능하므로 신뢰 경계로 쓰지 않는다 — 레이트 리밋 키 이상의 용도로 쓰면 안 된다.
     */
    private String visitorIpHash(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = (forwarded == null || forwarded.isBlank())
                ? request.getRemoteAddr()
                : forwarded.split(",")[0].strip();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(ip.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 32);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 을 쓸 수 없습니다", e);
        }
    }
}
