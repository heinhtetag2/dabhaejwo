package com.dabhaejwo.global.config;

import com.dabhaejwo.global.storage.FileStorage;
import com.dabhaejwo.global.storage.S3FileStorage;
import com.dabhaejwo.global.storage.UnavailableFileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * 파일 저장소 빈.
 *
 * <p>Cloudflare R2 는 S3 API 를 흉내내므로 AWS SDK 를 그대로 쓴다. 다만 두 가지가 다르다.
 * <ul>
 *   <li><b>리전이 없다.</b> SDK 가 리전을 요구하므로 {@code auto} 를 넣는다</li>
 *   <li><b>경로 스타일이어야 한다.</b> 가상 호스트 스타일({@code bucket.endpoint})은
 *       R2 계정 엔드포인트에서 동작하지 않는다</li>
 * </ul>
 */
@Configuration
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    @Bean
    FileStorage fileStorage(AppProperties properties) {
        AppProperties.Storage config = properties.storage();

        if (!config.configured()) {
            // 조용히 넘어가면 업로드 버튼을 누른 사람이 이유를 모른 채 실패를 본다.
            log.warn("파일 저장소가 설정되지 않았습니다 (R2_BUCKET·R2_ENDPOINT·키). "
                    + "파일 업로드는 FEATURE_NOT_READY 로 거절됩니다.");
            return new UnavailableFileStorage();
        }

        S3Client client = S3Client.builder()
                .endpointOverride(URI.create(config.endpoint()))
                // R2 에는 리전 개념이 없지만 SDK 가 요구한다.
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.accessKeyId(), config.secretAccessKey())))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();

        log.info("파일 저장소 연결 — bucket={}", config.bucket());
        return new S3FileStorage(client, config.bucket());
    }
}
