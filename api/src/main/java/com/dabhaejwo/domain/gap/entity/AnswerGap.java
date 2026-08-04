package com.dabhaejwo.domain.gap.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * 답변 개선 대상 — 챗봇이 답하지 못했거나 방문자가 👎 를 누른 질문.
 *
 * <p>같은 질문을 표현만 바꿔 물어도 하나로 묶는다. 안 그러면 "제주도 배송되나요"와
 * "제주도까지 배송 되나요?"가 따로 세어져, 업체가 실제로 몇 번 놓쳤는지 알 수 없다.
 */
@Entity
@Table(name = "answer_gaps")
public class AnswerGap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** 마지막으로 들어온 원문. 업체에게는 이걸 보여준다. */
    @Column(nullable = false)
    private String question;

    /** 누적 키. 화면에 나오지 않는다. */
    @Column(name = "question_norm", nullable = false)
    private String questionNorm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GapReason reason;

    @Column(name = "occurrence_count", nullable = false)
    private int occurrenceCount;

    @Column(name = "last_asked_at", nullable = false)
    private OffsetDateTime lastAskedAt;

    @Column(name = "last_path")
    private String lastPath;

    /** 그때 챗봇이 실제로 한 말. 업체가 "왜 이게 문제인지" 판단하는 근거다. */
    @Column(name = "bot_answer")
    private String botAnswer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GapStatus status;

    @Column(name = "resolved_faq_id")
    private UUID resolvedFaqId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected AnswerGap() {
    }

    public static AnswerGap of(UUID tenantId, String question, GapReason reason,
                               String lastPath, String botAnswer) {
        AnswerGap gap = new AnswerGap();
        gap.tenantId = tenantId;
        gap.question = question;
        gap.questionNorm = normalize(question);
        gap.reason = reason;
        gap.occurrenceCount = 1;
        gap.lastAskedAt = OffsetDateTime.now();
        gap.lastPath = lastPath;
        gap.botAnswer = botAnswer;
        gap.status = GapStatus.OPEN;
        gap.createdAt = gap.lastAskedAt;
        return gap;
    }

    /**
     * 공백·문장부호를 걷어낸 소문자 키. 한글은 NFKC 로 정규화한다 —
     * 자모가 분리된 입력(macOS 복사 등)이 다른 질문으로 세어지는 것을 막는다.
     */
    public static String normalize(String raw) {
        return Normalizer.normalize(raw, Normalizer.Form.NFKC)
                .toLowerCase(Locale.KOREAN)
                .replaceAll("[\\p{Punct}\\s]+", "");
    }

    /** 같은 질문이 또 들어왔을 때. */
    public void recur(String question, String lastPath, String botAnswer) {
        this.question = question;
        this.occurrenceCount++;
        this.lastAskedAt = OffsetDateTime.now();
        this.lastPath = lastPath;
        this.botAnswer = botAnswer;
        // 업체가 넘어갔던 질문이라도 다시 들어오면 목록에 되살린다.
        if (this.status == GapStatus.DISMISSED) {
            this.status = GapStatus.OPEN;
        }
    }

    public void resolveWith(UUID faqId) {
        this.status = GapStatus.RESOLVED;
        this.resolvedFaqId = faqId;
    }

    public void dismiss() {
        this.status = GapStatus.DISMISSED;
    }

    public Long getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getQuestion() {
        return question;
    }

    public GapReason getReason() {
        return reason;
    }

    public int getOccurrenceCount() {
        return occurrenceCount;
    }

    public OffsetDateTime getLastAskedAt() {
        return lastAskedAt;
    }

    public String getLastPath() {
        return lastPath;
    }

    public String getBotAnswer() {
        return botAnswer;
    }

    public GapStatus getStatus() {
        return status;
    }

    public UUID getResolvedFaqId() {
        return resolvedFaqId;
    }
}
