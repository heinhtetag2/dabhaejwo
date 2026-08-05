package com.dabhaejwo.domain.knowledge.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 임베딩 조각 저장·검색.
 *
 * <p><b>여기가 pgvector 를 아는 유일한 곳이다.</b> Hibernate 는 {@code vector} 타입을
 * 매핑하지 못하고, {@code <=>} 연산자가 서비스 레이어로 새어 나가면 저장소를 바꿀 때
 * 도메인까지 흔들린다 (.claude/rules/backend-spring-boot.md).
 */
@Repository
public class KnowledgeChunkRepository {

    private final JdbcTemplate jdbc;

    public KnowledgeChunkRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 문서의 조각을 통째로 갈아끼운다.
     *
     * <p>다시 학습할 때 이전 조각이 남아 있으면 옛 내용이 계속 검색된다 —
     * 업체는 문서를 고쳤는데 챗봇이 옛말을 하는 상황이 된다.
     */
    public void replaceForDocument(UUID tenantId, UUID documentId, List<String> contents,
                                   List<float[]> embeddings) {
        if (contents.size() != embeddings.size()) {
            throw new IllegalArgumentException("조각 수와 벡터 수가 다릅니다");
        }
        deleteByDocument(tenantId, documentId);

        for (int i = 0; i < contents.size(); i++) {
            jdbc.update("""
                    INSERT INTO knowledge_chunks (tenant_id, document_id, ordinal, content, embedding, created_at)
                    VALUES (?, ?, ?, ?, ?::vector, now())
                    """, tenantId, documentId, i, contents.get(i), toVectorLiteral(embeddings.get(i)));
        }
    }

    public void deleteByDocument(UUID tenantId, UUID documentId) {
        jdbc.update("DELETE FROM knowledge_chunks WHERE tenant_id = ? AND document_id = ?",
                tenantId, documentId);
    }

    public long countByTenant(UUID tenantId) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM knowledge_chunks WHERE tenant_id = ?", Long.class, tenantId);
        return count == null ? 0 : count;
    }

    /**
     * 질문 벡터와 가까운 조각을 찾는다.
     *
     * <p><b>{@code tenant_id} 조건은 절대 빠지지 않는다.</b> 타 업체 자료가 답변 근거로
     * 섞이면 그 순간 서비스가 끝난다 (tenant-plan.md §7.1).
     *
     * <p>이 조건 때문에 HNSW 인덱스를 못 탈 수 있다 — 벡터 인덱스는 필터 없는
     * {@code ORDER BY ... LIMIT} 에서 가장 잘 동작한다. <b>그래도 필터를 뺄 수는 없다.</b>
     * 느리면 테넌트별 파티셔닝을 검토한다 (docs/IMPROVEMENTS.md).
     *
     * <p>{@code <=>} 는 코사인 거리다 — 0에 가까울수록 비슷하다. 유사도로 뒤집어 돌려준다.
     */
    public List<Match> search(UUID tenantId, float[] queryEmbedding, int limit) {
        return jdbc.query("""
                SELECT c.document_id, c.content, d.title, d.path,
                       1 - (c.embedding <=> ?::vector) AS similarity
                FROM knowledge_chunks c
                JOIN knowledge_documents d ON d.id = c.document_id
                WHERE c.tenant_id = ?
                  AND d.status = 'INDEXED'
                ORDER BY c.embedding <=> ?::vector
                LIMIT ?
                """,
                (rs, rowNum) -> new Match(
                        UUID.fromString(rs.getString("document_id")),
                        rs.getString("title"),
                        rs.getString("path"),
                        rs.getString("content"),
                        rs.getDouble("similarity")),
                toVectorLiteral(queryEmbedding), tenantId, toVectorLiteral(queryEmbedding), limit);
    }

    /**
     * pgvector 리터럴. {@code [0.1,0.2,...]} 형태다.
     *
     * <p>{@code Locale.ROOT} 로 찍는다 — 한국어 로케일에서 소수점이 쉼표로 나오면
     * 벡터 전체가 깨진다.
     */
    private String toVectorLiteral(float[] vector) {
        StringBuilder literal = new StringBuilder(vector.length * 8 + 2);
        literal.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                literal.append(',');
            }
            literal.append(String.format(java.util.Locale.ROOT, "%.6f", vector[i]));
        }
        return literal.append(']').toString();
    }

    /**
     * @param documentPath 웹페이지 문서의 경로. 업로드 파일은 {@code null} 이다 —
     *                     방문자가 열 수 없는 것을 링크로 주지 않는다
     * @param similarity 1에 가까울수록 비슷하다. 답변 실패 판정 임계값과 비교한다
     *                   ({@code cost_guards.answer_fail_similarity})
     */
    public record Match(UUID documentId, String documentTitle, String documentPath,
                        String content, double similarity) {
    }
}
