package com.staylog.staylog.global.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate 설정
 * 외부 API 호출용: Toss Payments, OAuth 로그인, 카카오맵
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))   // 연결 타임아웃 5초
                .setReadTimeout(Duration.ofSeconds(20))     // 읽기 타임아웃 20초 (Toss 결제 API용)
                .build();
    }
}
