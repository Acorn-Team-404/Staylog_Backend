package com.staylog.staylog.domain.payment.service;

import com.staylog.staylog.domain.booking.mapper.BookingMapper;
import com.staylog.staylog.domain.coupon.service.CouponService;
import com.staylog.staylog.domain.payment.entity.Payment;
import com.staylog.staylog.domain.payment.mapper.PaymentMapper;
import com.staylog.staylog.external.toss.client.TossPaymentClient;
import com.staylog.staylog.external.toss.dto.request.TossCancelRequest;
import com.staylog.staylog.global.constant.PaymentStatus;
import com.staylog.staylog.global.constant.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 보상 트랜잭션 서비스
 * - 결제 실패 시 예약과 결제 상태를 롤백하는 보상 처리
 * - REQUIRES_NEW: 외부 트랜잭션과 독립적으로 실행되어 실패 시에도 커밋됨
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCompensationService {

    private final PaymentMapper paymentMapper;
    private final BookingMapper bookingMapper;
    private final CouponService couponService;
    private final TossPaymentClient tossPaymentClient;

    /**
     * 결제 실패 시 보상 트랜잭션 실행
     * - PAYMENT -> PAY_FAILED
     * - RESERVATION -> RES_CANCELED
     *
     * @param bookingId 예약 ID
     * @param paymentId 결제 ID (nullable: 결제 생성 전 실패 시 null)
     * @param couponId 쿠폰 ID (nullable: 쿠폰 미사용 시 null)
     * @param failureReason 실패 사유
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensateFailedPayment(Long bookingId, Long paymentId, Long couponId, String failureReason) {
        log.warn("보상 트랜잭션 실행: bookingId={}, paymentId={}, reason={}", bookingId, paymentId, failureReason);

        try {
            // 1. PAYMENT 상태 업데이트 (락 없이 직접 UPDATE)
            if (paymentId != null) {
                paymentMapper.updatePaymentFailure(paymentId, PaymentStatus.PAY_FAILED.getCode(), failureReason);
                log.debug("결제 상태 변경: paymentId={}, status=PAY_FAILED", paymentId);
            } else {
                log.warn("결제 정보 없음: bookingId={} (결제 생성 전 실패)", bookingId);
            }

            // 2. RESERVATION 상태 -> CANCELED
            bookingMapper.updateBookingStatus(bookingId, ReservationStatus.RES_CANCELED.getCode());
            log.debug("예약 상태 변경: bookingId={}, status=RES_CANCELED", bookingId);

            // 3. COUPON 복구 (쿠폰이 사용된 경우)
            if (couponId != null) {
                try {
                    couponService.revertCouponUsage(couponId);
                    log.info("쿠폰 복구 완료: couponId={}", couponId);
                } catch (Exception e) {
                    log.error("쿠폰 복구 실패: couponId={}, error={}", couponId, e.getMessage(), e);
                    // 쿠폰 복구 실패는 로그만 남김 (보상 트랜잭션 전체를 실패시키지 않음)
                }
            }

            log.info("보상 트랜잭션 커밋: bookingId={}, paymentId={}", bookingId, paymentId);

        } catch (Exception e) {
            log.error("보상 트랜잭션 실패: bookingId={}, paymentId={}, error={}", bookingId, paymentId, e.getMessage(), e);
            throw e;  // 보상 트랜잭션 롤백 (외부 트랜잭션과 무관)
        }
    }

    /**
     * Toss 결제 취소 (보상)
     * Phase 3 실패 시 Toss API로 이미 승인된 결제를 취소
     *
     * @param paymentKey Toss 결제 키
     * @param reason 취소 사유
     */
    public void compensateTossPayment(String paymentKey, String reason) {
        log.warn("Toss 결제 취소 시도: paymentKey={}, reason={}", paymentKey, reason);

        try {
            TossCancelRequest cancelRequest = TossCancelRequest.builder()
                    .cancelReason(reason)
                    .build();

            tossPaymentClient.cancel(paymentKey, cancelRequest);
            log.info("Toss 결제 취소 완료: paymentKey={}", paymentKey);

        } catch (Exception e) {
            log.error("Toss 결제 취소 실패: paymentKey={}, error={}", paymentKey, e.getMessage(), e);
        }
    }

    /**
     * 결제 만료 시 보상 트랜잭션 실행 (스케줄러용)
     * - PAYMENT -> PAY_EXPIRED
     * - RESERVATION -> RES_CANCEL
     *
     * @param bookingId 예약 ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensateExpiredPayment(Long bookingId) {
        log.warn("결제 만료 보상 트랜잭션 실행: bookingId={}", bookingId);

        try {
            // 1.  비관적 락으로 PAYMENT 조회 (동시성 제어)
            Payment payment = paymentMapper.findPaymentByBookingIdWithLock(bookingId);
            if (payment != null) {
                Long paymentId = payment.getPaymentId();
                String paymentStatus = payment.getStatus();

                // READY 상태인 결제만 EXPIRED로 변경
                if (PaymentStatus.PAY_READY.getCode().equals(paymentStatus)) {
                    paymentMapper.updatePaymentFailure(paymentId, PaymentStatus.PAY_EXPIRED.getCode(), "결제 시간 만료");
                    log.debug("결제 상태 변경: paymentId={}, status=PAY_EXPIRED", paymentId);
                }
            }

            // 2. RESERVATION 상태 -> CANCELED
            bookingMapper.updateBookingStatus(bookingId, ReservationStatus.RES_CANCELED.getCode());
            log.debug("예약 상태 변경: bookingId={}, status=RES_CANCELED", bookingId);

            // 3. COUPON 복구 (쿠폰이 사용된 경우)
            if (payment != null) {
                Long couponId = payment.getCouponId();
                if (couponId != null) {
                    try {
                        couponService.revertCouponUsage(couponId);
                        log.info("만료로 인한 쿠폰 복구 완료: couponId={}", couponId);
                    } catch (Exception e) {
                        log.error("만료로 인한 쿠폰 복구 실패: couponId={}, error={}", couponId, e.getMessage(), e);
                    }
                }
            }

            log.info("만료 보상 트랜잭션 커밋: bookingId={}", bookingId);

        } catch (Exception e) {
            log.error("만료 보상 트랜잭션 실패: bookingId={}, error={}", bookingId, e.getMessage(), e);
            throw e;
        }
    }
}
