package com.dabhaejwo.domain.lead.entity;

import com.dabhaejwo.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.dabhaejwo.global.security.BotScope;

/**
 * 마스킹이 뚫리면 방문자 개인정보가 화면에 그대로 나간다.
 * 되돌릴 수 없는 종류의 사고라 테스트로 고정한다.
 */
class LeadTest {

    /** 시험용 범위. 두 값을 따로 넘기지 않는 것이 규약이다. */
    private static final BotScope SCOPE =
            new BotScope(UUID.randomUUID(), UUID.randomUUID());

    @Test
    @DisplayName("휴대폰 번호는 가운데를 가린다")
    void masksPhoneNumber() {
        assertEquals("010-****-3391", Lead.mask("010-2847-3391"));
        assertEquals("010-****-3391", Lead.mask("01028473391"));
    }

    @Test
    @DisplayName("이메일은 앞 두 글자와 도메인만 남긴다")
    void masksEmail() {
        // local 부분 kildong(7자) 중 앞 2자만 남기므로 별표는 5개다
        assertEquals("ki*****@example.com", Lead.mask("kildong@example.com"));
    }

    @Test
    @DisplayName("어떤 입력에도 원문 뒷부분이 통째로 남지 않는다")
    void neverLeaksFullValue() {
        // 빈 값은 제외한다 — 가릴 내용이 없으니 누출도 없다. contact 는 NOT NULL 이라 실제로 오지도 않는다.
        for (String raw : new String[] {"010-2847-3391", "01028473391", "kildong@example.com", "1234"}) {
            assertFalse(Lead.mask(raw).equals(raw), "마스킹되지 않았다: " + raw);
        }
    }

    @Test
    @DisplayName("짧아서 가릴 수 없는 값은 전부 가린다")
    void masksTooShortValueEntirely() {
        assertEquals("****", Lead.mask("1234"));
    }

    @Test
    @DisplayName("종료된 리드는 되돌릴 수 없다")
    void closedIsTerminal() {
        Lead lead = Lead.of(SCOPE, null, "김OO", "010-2847-3391", "문의");
        lead.changeStatus(LeadStatus.CONTACTED);
        lead.changeStatus(LeadStatus.CLOSED);

        assertThrows(BusinessException.class, () -> lead.changeStatus(LeadStatus.NEW));
    }

    @Test
    @DisplayName("연락함은 대기로 되돌릴 수 있다 — 잘못 눌렀을 때 손쓸 방법이 있어야 한다")
    void contactedIsReversible() {
        Lead lead = Lead.of(SCOPE, null, "김OO", "010-2847-3391", "문의");
        lead.changeStatus(LeadStatus.CONTACTED);
        lead.changeStatus(LeadStatus.NEW);

        assertEquals(LeadStatus.NEW, lead.getStatus());
    }
}
