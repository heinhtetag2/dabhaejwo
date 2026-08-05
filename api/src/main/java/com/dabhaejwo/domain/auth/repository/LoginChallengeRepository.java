package com.dabhaejwo.domain.auth.repository;

import com.dabhaejwo.domain.auth.entity.LoginChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface LoginChallengeRepository extends JpaRepository<LoginChallenge, UUID> {

    /** 재발송 횟수. 메일 폭탄을 막는 판단 근거다. */
    long countBySubjectIdAndCreatedAtAfter(UUID subjectId, OffsetDateTime after);

    /**
     * 같은 사람이 새로 로그인하면 이전 챌린지를 닫는다.
     *
     * <p>안 닫으면 <b>메일함에 살아 있는 옛 코드가 전부 유효하다.</b> 코드를 다섯 번 받으면
     * 맞힐 기회가 다섯 배가 되는 셈이다.
     */
    @Modifying
    @Query("""
            UPDATE LoginChallenge c SET c.consumedAt = :now
            WHERE c.subjectId = :subjectId AND c.consumedAt IS NULL
            """)
    void closeOpenChallenges(@Param("subjectId") UUID subjectId, @Param("now") OffsetDateTime now);
}
