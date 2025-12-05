package com.staylog.staylog.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Spring Retry 설정
 * - 보상 트랜잭션 자동 재시도를 위한 설정
 */
@Configuration
@EnableRetry
public class RetryConfig {
}
