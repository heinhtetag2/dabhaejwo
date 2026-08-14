package com.dabhaejwo.domain.faq.entity;

import com.dabhaejwo.global.security.BotScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 공통 질문 = 저장 답변.
 *
 * <p>이 답변으로 나가는 응답은 <b>모델을 거치지 않는다.</b> 그래서 {@code ai_usage} 에
 * 기록되지 않고 대화 사용량에도 잡히지 않는다 — 업체 입장에서 원가가 0인 답변이다.
 * 업체 상세의 "공통 질문 0개 강조"가 원가 급증의 선행 지표인 이유가 이것이다.
 *
 * <p>{@code shown} 은 <b>버튼 노출 여부일 뿐이다.</b> false 여도 방문자가 비슷한 내용을
 * 직접 입력하면 이 답변이 쓰인다. 끄는 것과 지우는 것은 다르다.
 */
@Entity
@Table(name = "faqs")
public class Faq {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** 어느 서비스의 것인가. 조회는 전부 이 값으로 좁힌다. */
    @Column(name = "bot_id", nullable = false)
    private UUID botId;

    @Column(nullable = false)
    private String question;

    @Column(nullable = false)
    private String answer;

    /** 답변 아래에 링크로 붙는 문서 제목들. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<String> links = new ArrayList<>();

    /** 답변을 보여준 뒤 이어서 제안할 질문들. 표시용으로만 읽으므로 jsonb 다. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "follow_up_faq_ids", columnDefinition = "jsonb", nullable = false)
    private List<UUID> followUpFaqIds = new ArrayList<>();

    @Column(nullable = false)
    private boolean shown;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "hit_count", nullable = false)
    private int hitCount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Faq() {
    }

    public static Faq of(BotScope scope, String question, String answer, List<String> links,
                         boolean shown, int sortOrder) {
        return of(scope, question, answer, links, null, shown, sortOrder);
    }

    public static Faq of(BotScope scope, String question, String answer, List<String> links,
                         List<UUID> followUpFaqIds, boolean shown, int sortOrder) {
        Faq faq = new Faq();
        faq.tenantId = scope.tenantId();
        faq.botId = scope.botId();
        faq.sortOrder = sortOrder;
        faq.createdAt = OffsetDateTime.now();
        faq.edit(question, answer, links, followUpFaqIds, shown);
        return faq;
    }

    public void edit(String question, String answer, List<String> links,
                     List<UUID> followUpFaqIds, boolean shown) {
        this.question = question;
        this.answer = answer;
        this.links = links == null ? new ArrayList<>() : new ArrayList<>(links);
        this.followUpFaqIds = followUpFaqIds == null ? new ArrayList<>() : new ArrayList<>(followUpFaqIds);
        this.shown = shown;
        touch();
    }

    public void moveTo(int order) {
        this.sortOrder = order;
        touch();
    }

    /** 방문자가 버튼을 눌렀을 때. 원가가 발생하지 않는 응답이라 사용량과는 별개로 센다. */
    public void hit() {
        this.hitCount++;
    }

    private void touch() {
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getBotId() {
        return botId;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public List<String> getLinks() {
        return links;
    }

    public List<UUID> getFollowUpFaqIds() {
        return followUpFaqIds;
    }

    public boolean isShown() {
        return shown;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public int getHitCount() {
        return hitCount;
    }
}
