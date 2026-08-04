package com.dabhaejwo.global.storage;

import java.io.InputStream;

/**
 * 오브젝트 저장소.
 *
 * <p>구현은 S3 호환(Cloudflare R2)이지만 도메인은 그것을 모른다 — 저장소를 바꿔도
 * 업로드 서비스가 그대로여야 한다.
 *
 * <p>키는 <b>우리가 정한다.</b> 사용자가 올린 파일명을 키로 쓰면 경로 조작(`../`)과
 * 테넌트 간 충돌이 동시에 생긴다.
 */
public interface FileStorage {

    /**
     * @param key         저장소 안의 경로. {@code tenants/{tenantId}/documents/{documentId}}
     * @param contentType 서버가 판정한 MIME. 클라이언트가 보낸 값을 그대로 쓰지 않는다
     */
    void put(String key, InputStream content, long size, String contentType);

    /** 없으면 조용히 성공으로 친다 — 지우려던 것이 이미 없는 상태는 원하는 결과다. */
    void delete(String key);

    /** 설정이 없으면 false. 호출부는 업로드를 명시적으로 거절한다. */
    boolean available();
}
