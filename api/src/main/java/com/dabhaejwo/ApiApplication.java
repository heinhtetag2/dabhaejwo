package com.dabhaejwo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * {@code @ConfigurationPropertiesScan} 이 없으면 {@code @ConfigurationProperties} 를 붙인
 * record 가 빈으로 등록되지 않는다. 컴파일은 통과하고 기동할 때 터지므로
 * 빌드만으로는 잡히지 않는다.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
// 학습 대기 문서를 집어가는 IndexingWorker 가 @Scheduled 로 돈다.
@EnableScheduling
public class ApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}

}
