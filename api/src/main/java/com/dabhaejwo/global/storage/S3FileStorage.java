package com.dabhaejwo.global.storage;

import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;

/**
 * S3 호환 저장소 구현. Cloudflare R2 를 쓴다.
 *
 * <p>R2 는 S3 API 를 그대로 흉내내므로 AWS SDK 를 쓸 수 있다. 다만 리전 개념이 없어
 * {@code auto} 를 넣고, 경로 스타일 접근을 켜야 한다 — 가상 호스트 스타일은 R2 계정
 * 엔드포인트에서 동작하지 않는다.
 */
public class S3FileStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(S3FileStorage.class);

    private final S3Client client;
    private final String bucket;

    public S3FileStorage(S3Client client, String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public void put(String key, InputStream content, long size, String contentType) {
        try {
            client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromInputStream(content, size));
        } catch (S3Exception e) {
            // 원문에는 버킷명·엔드포인트가 들어 있다. 로그에만 남기고 응답에는 싣지 않는다.
            log.error("파일 저장에 실패했습니다 (key={})", key, e);
            throw new BusinessException(ErrorCode.FEATURE_NOT_READY,
                    "파일을 저장하지 못했습니다. 잠시 후 다시 시도해 주세요");
        }
    }

    @Override
    public InputStream get(String key) {
        try {
            return client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception e) {
            log.error("파일을 읽지 못했습니다 (key={})", key, e);
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND,
                    "저장된 원본을 찾지 못했습니다");
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception e) {
            // 지우기 실패로 문서 삭제 자체를 막지 않는다. 남은 오브젝트는 비용일 뿐이고,
            // DB 에서 사라졌는데 화면에 계속 보이는 편이 사용자에게 훨씬 나쁘다.
            log.warn("저장소 오브젝트를 지우지 못했습니다 (key={}). 고아 객체가 남습니다", key, e);
        }
    }

    @Override
    public boolean available() {
        return true;
    }
}
