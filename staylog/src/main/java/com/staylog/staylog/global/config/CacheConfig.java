package com.staylog.staylog.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 로컬 캐시 설정 (Caffeine)
 * - 검색 결과, 공통 코드 캐싱
 * - TTL 기반 자동 만료
 * - 캐시 적중률 통계 기록
 *
 * @author Team404
 */
@Configuration
@EnableCaching  // Spring Cache 활성화
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // Caffeine 캐시 설정
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10_000)                         // 최대 10,000개 엔트리
            .expireAfterWrite(10, TimeUnit.MINUTES)      // TTL 10분 (검색 결과)
            .recordStats());                              // 통계 기록 (Hit Rate 측정용)

        // 캐시 이름 미리 등록
        cacheManager.setCacheNames(List.of("searchResults", "commonCodes"));

        return cacheManager;
    }
}
