package com.staylog.staylog.external.toss.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staylog.staylog.external.toss.config.TossPaymentsConfig;
import com.staylog.staylog.external.toss.dto.request.TossCancelRequest;
import com.staylog.staylog.external.toss.dto.request.TossConfirmRequest;
import com.staylog.staylog.external.toss.dto.request.TossVirtualAccountRequest;
import com.staylog.staylog.external.toss.dto.response.TossErrorResponse;
import com.staylog.staylog.external.toss.dto.response.TossPaymentResponse;
import com.staylog.staylog.external.toss.dto.response.TossVirtualAccountResponse;
import com.staylog.staylog.global.exception.custom.payment.TossApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * 토스 페이먼츠 API 클라이언트
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TossPaymentClient {

    private final RestTemplate restTemplate;
    private final TossPaymentsConfig tossConfig;
    private final ObjectMapper objectMapper;

    /**
     * 결제 승인 (재시도 로직 포함)
     * 일시적 오류 발생 시 최대 3회까지 재시도 (Exponential Backoff)
     */
    public TossPaymentResponse confirm(TossConfirmRequest request) {
        String url = tossConfig.getApiUrl() + "/confirm";
        int maxRetries = 3;
        int attempt = 0;

        HttpHeaders headers = createHeaders();
        headers.set("Idempotency-Key", generatePaymentIdempotencyKey(request.getPaymentKey()));
        HttpEntity<TossConfirmRequest> entity = new HttpEntity<>(request, headers);

        while (attempt < maxRetries) {
            attempt++;

            try {
                log.info("토스 결제 승인 요청 (시도 {}/{}): paymentKey={}, orderId={}, amount={}",
                        attempt, maxRetries, request.getPaymentKey(), request.getOrderId(), request.getAmount());

                ResponseEntity<TossPaymentResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    TossPaymentResponse.class
                );

                log.info("토스 결제 승인 성공: paymentKey={}", request.getPaymentKey());
                return response.getBody();

            } catch (HttpStatusCodeException e) {
                log.error("토스 결제 승인 실패 (시도 {}/{}): statusCode={}, body={}",
                          attempt, maxRetries, e.getStatusCode(), e.getResponseBodyAsString());

                boolean isRetryable = isRetryableError(e);

                if (isRetryable && attempt < maxRetries) {
                    long waitTimeMs = calculateBackoffMs(attempt);
                    log.warn("일시적 오류 감지, {}ms 후 재시도 (시도 {}/{}): statusCode={}",
                             waitTimeMs, attempt, maxRetries, e.getStatusCode());

                    try {
                        Thread.sleep(waitTimeMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("재시도 대기 중 인터럽트 발생");
                        throw parseTossError(e);
                    }
                    continue;
                }

                log.error("토스 결제 승인 최종 실패: 재시도 불가능하거나 최대 시도 횟수 초과");
                throw parseTossError(e);
            }
        }

        throw new TossApiException("RETRY_EXHAUSTED", "최대 재시도 횟수 초과");
    }

    /**
     * 결제 취소/환불
     */
    public TossPaymentResponse cancel(String paymentKey, TossCancelRequest request) {
        String url = tossConfig.getApiUrl() + "/" + paymentKey + "/cancel";

        HttpHeaders headers = createHeaders();

        // 멱등키 추가하기 (중복 환불 방지)
        headers.set("Idempotency-Key", generateCancelIdempotencyKey(paymentKey));

        HttpEntity<TossCancelRequest> entity = new HttpEntity<>(request, headers);

        try {
            log.info("토스 결제 취소 요청: paymentKey={}, cancelAmount={}",
                     paymentKey, request.getCancelAmount());

            ResponseEntity<TossPaymentResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                TossPaymentResponse.class
            );

            log.info("토스 결제 취소 성공: paymentKey={}", paymentKey);
            return response.getBody();

        } catch (HttpStatusCodeException e) {
            log.error("토스 결제 취소 실패: statusCode={}, body={}",
                      e.getStatusCode(), e.getResponseBodyAsString());
            throw parseTossError(e);
        }
    }

    /**
     * 결제 조회
     */
    public TossPaymentResponse getPayment(String paymentKey) {
        String url = tossConfig.getApiUrl() + "/" + paymentKey;

        HttpHeaders headers = createHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<TossPaymentResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                TossPaymentResponse.class
            );

            return response.getBody();

        } catch (HttpStatusCodeException e) {
            throw parseTossError(e);
        }
    }

    /**
     * 가상계좌 발급
     * API: POST /v1/virtual-accounts
     */
    public TossVirtualAccountResponse issueVirtualAccount(TossVirtualAccountRequest request) {
        String url = "https://api.tosspayments.com/v2/virtual-accounts";

        HttpHeaders headers = createHeaders();

        // 멱등키 추가 (가상계좌 중복 발급 방지)
        headers.set("Idempotency-Key", "VA_" + request.getOrderId());

        HttpEntity<TossVirtualAccountRequest> entity = new HttpEntity<>(request, headers);

        try {
            log.info("토스 가상계좌 발급 요청: orderId={}, amount={}, customerName={}, orderName={}, validHours={}",
                     request.getOrderId(), request.getAmount(), request.getCustomerName(),
                     request.getOrderName(), request.getValidHours());

            // 디버깅: 요청 본문 JSON 직렬화 확인
            try {
                String jsonBody = objectMapper.writeValueAsString(request);
                log.info("요청 JSON: {}", jsonBody);
            } catch (Exception e) {
                log.warn("JSON 직렬화 실패", e);
            }

            ResponseEntity<TossVirtualAccountResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                TossVirtualAccountResponse.class
            );

            TossVirtualAccountResponse result = response.getBody();
            log.info("토스 가상계좌 발급 성공: orderId={}, bank={}, accountNumber={}, dueDate={}",
                     result.getOrderId(), result.getBank(), result.getAccountNumber(), result.getDueDate());

            return result;

        } catch (HttpStatusCodeException e) {
            log.error("토스 가상계좌 발급 실패: statusCode={}, body={}",
                      e.getStatusCode(), e.getResponseBodyAsString());
            throw parseTossError(e);
        }
    }

    /**
     * HTTP 헤더 생성
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + tossConfig.getEncodedSecretKey());
        return headers;
    }

    /**
     * 결제 멱등키 생성
     */
    private String generatePaymentIdempotencyKey(String paymentKey) {
        return "CONFIRM_" + paymentKey;
    }

    /**
     * 환불(취소) 멱등키 생성
     */
    private String generateCancelIdempotencyKey(String refundId) {
        return "CANCEL_" + refundId;
    }

    /**
     * 토스 에러 파싱
     */
    private TossApiException parseTossError(HttpStatusCodeException e) {
        try {
            TossErrorResponse error = objectMapper.readValue(
                e.getResponseBodyAsString(),
                TossErrorResponse.class
            );
            return new TossApiException(error.getCode(), error.getMessage());
        } catch (Exception ex) {
            return new TossApiException("UNKNOWN", e.getMessage());
        }
    }

    /**
     * 재시도 가능한 오류인지 판단
     * HTTP 상태 코드 기반으로 일시적 오류 여부 확인
     */
    private boolean isRetryableError(HttpStatusCodeException e) {
        int statusCode = e.getStatusCode().value();

        // 재시도 가능한 HTTP 상태 코드
        // 503: Service Unavailable (서버 일시 과부하)
        // 504: Gateway Timeout (게이트웨이 타임아웃)
        // 408: Request Timeout (요청 타임아웃)
        // 429: Too Many Requests (요청 횟수 초과)
        return statusCode == 503
            || statusCode == 504
            || statusCode == 408
            || statusCode == 429;
    }

    /**
     * Exponential Backoff 대기 시간 계산
     * 1차 재시도: 1000ms (1초)
     * 2차 재시도: 2000ms (2초)
     * 3차 재시도: 4000ms (4초)
     */
    private long calculateBackoffMs(int attempt) {
        return (long) Math.pow(2, attempt - 1) * 1000;
    }
}
