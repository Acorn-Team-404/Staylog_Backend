package com.staylog.staylog.global.config;

import com.staylog.staylog.global.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security 설정
 * - JWT 기반 인증/인가 설정
 * - CORS 설정
 * - CSRF 비활성화 (stateless API)
 * - 세션 관리: STATELESS
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // 항상 허용할 경로들 (메서드 상관없이 모두 허용)
    private static final String[] PERMIT_ALL_PATHS = {
            "/actuator/**",          // Spring Actuator
            "/swagger-ui/**",        // Swagger UI
            "/v3/api-docs/**",        // Swagger API Docs
            "/error"                // 에러 페이지
    };


    /**
     * Security Filter Chain 설정
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (JWT 사용으로 stateless)
                .csrf(AbstractHttpConfigurer::disable)

                // CORS 설정 활성화
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 세션 관리: STATELESS (JWT 사용)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 요청 인가 설정
                .authorizeHttpRequests(auth -> auth

                        // CORS Preflight: OPTIONS 메서드 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 공용 경로: 위에서 정의한 배열의 모든 경로 허용
                        .requestMatchers(PERMIT_ALL_PATHS).permitAll()

                        // Auth (로그인 필요 없음)
                        .requestMatchers(HttpMethod.POST, "/v1/user", "/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/user/loginId/*/duplicate").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/user/nickname/*/duplicate").permitAll()

                        // Email (로그인 필요 없음)
                        .requestMatchers(HttpMethod.POST, "/v1/mail-send", "/v1/mail-check").permitAll()

                        // Board (로그인 필요 없음 - GET/POST 일부)
                        .requestMatchers(HttpMethod.POST, "/v1/boardList").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/v1/boards/*",
                                "/journal",
                                "/journal/*",
                                "/review",
                                "/review/*"
                        ).permitAll()

                        // 정적 리소스 이미지 파일 허용
                        .requestMatchers(HttpMethod.GET, "/images/**").permitAll() // 👈 403 에러 해결!

                        // VIP 전용
                        .requestMatchers("/form/journal").hasAuthority("VIP")

                        // Admin 전용
                        .requestMatchers("/v1/admin/**").hasAuthority("ADMIN")

                        // 로그인 필요
                        // Board (수정/삭제/특정 조회)
                        .requestMatchers(HttpMethod.POST, "/v1/boards").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/v1/boards/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/v1/boards/*").authenticated()
                        .requestMatchers(HttpMethod.GET, "/v1/boards/booking/*").authenticated()
                        .requestMatchers("/form/review").authenticated()
                        // Mypage
                        .requestMatchers("/v1/mypage/**").authenticated()
                        // Profile
                        .requestMatchers(HttpMethod.POST, "/v1/profile").authenticated()
                        // Notification & SSE
                        .requestMatchers("/v1/notification/**").authenticated()
                        .requestMatchers("/api/v1/notification/subscribe").authenticated() // SSE 구독 경로
                        // Coupon
                        .requestMatchers("/v1/coupon/**").authenticated()
                        // Image (쓰기/수정/삭제)
                        .requestMatchers(HttpMethod.POST, "/v1/images").authenticated() // 이미지 업로드
                        .requestMatchers(HttpMethod.PUT, "/v1/images").authenticated() // 이미지 일괄 업데이트
                        .requestMatchers(HttpMethod.DELETE, "/v1/image/*").authenticated() // 단일 이미지 삭제
                        .requestMatchers(HttpMethod.DELETE, "/v1/images/*/*").authenticated() // 대상의 다중 이미지 삭제

                        // 나머지 모든 요청
                        .anyRequest().authenticated()
                )

                // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 이전에 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 Origin (개발 환경)
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",      // React 개발 서버
                "http://localhost:8080",      // 로컬 테스트
                "http://127.0.0.1:3000",
                "http://127.0.0.1:8080",
                "http://localhost:5173"
        ));

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 인증 정보 포함 허용 (쿠키, Authorization 헤더 등)
        configuration.setAllowCredentials(true);

        // preflight 요청 캐싱 시간 (초)
        configuration.setMaxAge(3600L);

        // 노출할 헤더 (클라이언트가 접근할 수 있는 응답 헤더)
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * 비밀번호 암호화를 위한 PasswordEncoder 빈 등록
     * BCrypt 알고리즘 사용
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


}