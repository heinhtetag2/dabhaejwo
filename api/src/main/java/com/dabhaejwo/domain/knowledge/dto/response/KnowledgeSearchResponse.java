package com.dabhaejwo.domain.knowledge.dto.response;

import com.dabhaejwo.domain.knowledge.repository.KnowledgeChunkRepository;

import java.util.List;
import java.util.UUID;

/**
 * 지식 검색 결과.
 *
 * <p>업체가 "이 질문에 챗봇이 뭘 보고 답하는지" 확인하는 용도다. 답변을 만들지 않으므로
 * 답변 모델을 부르지 않고, 질문 임베딩 비용만 든다.
 *
 * @param query    검색에 실제로 쓴 질문
 * @param matches  가까운 순. 비었으면 근거가 없다는 뜻이고, 그대로 보여준다
 */
public record KnowledgeSearchResponse(String query, List<Match> matches) {

    public static KnowledgeSearchResponse of(String query, List<KnowledgeChunkRepository.Match> matches) {
        return new KnowledgeSearchResponse(query, matches.stream().map(Match::from).toList());
    }

    /**
     * @param similarity 1에 가까울수록 비슷하다. 화면에서 반올림하되 서버는 원값을 준다 —
     *                   임계값을 조정할 때 반올림된 값만 있으면 근거가 없다
     */
    public record Match(UUID documentId, String documentTitle, String content, double similarity) {

        static Match from(KnowledgeChunkRepository.Match match) {
            return new Match(match.documentId(), match.documentTitle(),
                    match.content(), match.similarity());
        }
    }
}
