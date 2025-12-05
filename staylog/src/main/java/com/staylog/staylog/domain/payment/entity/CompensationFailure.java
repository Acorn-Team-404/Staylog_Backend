package com.staylog.staylog.domain.payment.entity;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DLQ (Dead Letter Queue) 엔티티
 * 보상 트랜잭션 실패 이력
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensationFailure {

    private Long id;
    private String type;  // PAYMENT_FAIL, TOSS_CANCEL

    // 원본 데이터
    private Long bookingId;
    private Long paymentId;
    private String paymentKey;
    private Long couponId;

    // 실패 정보
    private String reason;
    private String errorMessage;
    private String stackTrace;

    // 재처리 정보
    private Integer retryCount;
    private String status;  // PENDING, PROCESSING, RESOLVED, IGNORED

    // 메타데이터
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private String resolvedBy;
    private String memo;
}
