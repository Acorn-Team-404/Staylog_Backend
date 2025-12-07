package com.staylog.staylog.domain.payment.service.impl;

import com.staylog.staylog.domain.booking.dto.response.BookingDetailResponse;
import com.staylog.staylog.domain.booking.entity.Booking;
import com.staylog.staylog.domain.booking.mapper.BookingMapper;
import com.staylog.staylog.domain.booking.service.BookingService;
import com.staylog.staylog.domain.coupon.dto.response.CouponResponse;
import com.staylog.staylog.domain.coupon.service.CouponService;
import com.staylog.staylog.domain.payment.dto.request.ConfirmPaymentRequest;
import com.staylog.staylog.domain.payment.dto.request.PreparePaymentRequest;
import com.staylog.staylog.domain.payment.dto.response.PaymentResultResponse;
import com.staylog.staylog.domain.payment.dto.response.PreparePaymentResponse;
import com.staylog.staylog.domain.payment.entity.Payment;
import com.staylog.staylog.domain.payment.mapper.PaymentMapper;
import com.staylog.staylog.domain.payment.service.PaymentCompensationService;
import com.staylog.staylog.domain.payment.service.PaymentService;
import com.staylog.staylog.external.toss.client.TossPaymentClient;
import com.staylog.staylog.external.toss.config.TossPaymentsConfig;
import com.staylog.staylog.external.toss.dto.request.TossCancelRequest;
import com.staylog.staylog.external.toss.dto.request.TossConfirmRequest;
import com.staylog.staylog.external.toss.dto.request.TossVirtualAccountRequest;
import com.staylog.staylog.external.toss.dto.response.TossPaymentResponse;
import com.staylog.staylog.external.toss.dto.response.TossVirtualAccountResponse;
import com.staylog.staylog.external.toss.dto.response.VirtualAccount;
import com.staylog.staylog.global.common.code.ErrorCode;
import com.staylog.staylog.global.constant.PaymentStatus;
import com.staylog.staylog.global.constant.ReservationStatus;
import com.staylog.staylog.global.event.PaymentConfirmEvent;
import com.staylog.staylog.global.exception.custom.ForbiddenException;
import com.staylog.staylog.global.exception.custom.booking.BookingNotFoundException;
import com.staylog.staylog.global.exception.custom.payment.PaymentAmountMismatchException;
import com.staylog.staylog.global.exception.custom.payment.PaymentFailedException;
import com.staylog.staylog.global.exception.custom.payment.TossApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * 결제 서비스 구현
 * - 결제 준비 (READY 상태)
 * - 결제 승인 (Toss API 호출)
 * - 보상 트랜잭션 (실패 시 롤백)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final BookingMapper bookingMapper;
    private final BookingService bookingService;
    private final TossPaymentClient tossPaymentClient;
    private final TossPaymentsConfig tossConfig;
    private final PaymentCompensationService compensationService;
    private final CouponService couponService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 결제 준비
     * - 예약 소유자 확인 (인가)
     * - 예약 상태 검증 (PENDING & 5분 이내)
     * - 결제 생성 (READY 상태)
     */
    @Override
    @Transactional
    public PreparePaymentResponse preparePayment(PreparePaymentRequest request, Long userId) {
        log.info("결제 준비 시작: userId={}, bookingId={}, amount={}", userId, request.getBookingId(), request.getAmount());

        // 1. 예약 소유자 확인 (인가)
        validateBookingOwnership(request.getBookingId(), userId);

        // 2. 예약 상태 검증 (PENDING & 5분 이내)
        bookingService.validateBookingPending(request.getBookingId());

        // 3. 예약 정보 조회
        BookingDetailResponse booking = bookingMapper.findBookingById(request.getBookingId());
        if (booking == null) {
            throw new BookingNotFoundException(request.getBookingId());
        }

        Long bookingAmount = booking.getAmount();
        String bookingNum = booking.getBookingNum();
        String guestName = booking.getGuestName();

        // 4. 금액 검증
        if (!bookingAmount.equals(request.getAmount())) {
            log.error("결제 금액 불일치: 예약금액={}, 요청금액={}", bookingAmount, request.getAmount());
            throw new PaymentAmountMismatchException(bookingAmount, request.getAmount());
        }

        // 5. 쿠폰 할인 계산 (couponId가 있는 경우)
        Long originalAmount = request.getAmount();  // 할인 전 금액
        Long discountAmount = 0L;
        Long finalAmount = originalAmount;
        Long couponId = request.getCouponId();

        if (couponId != null) {
            // 쿠폰 검증 메서드 호출
            CouponResponse availableCoupon = couponService.validateCoupon(userId, couponId);
            
            // 할인금액 계산 및 최종금액 계산
            discountAmount = couponService.calculateCouponDiscount(originalAmount, availableCoupon.getDiscount());
            finalAmount = originalAmount - discountAmount;

            // 최종 금액은 0원 이상이어야 함
            if (finalAmount < 0) {
                finalAmount = 0L;
            }

            //  RESERVATION.FINAL_AMOUNT 업데이트
            bookingMapper.updateFinalAmount(request.getBookingId(), finalAmount);

            log.info("쿠폰 할인 적용: 쿠폰ID={}, 원래금액={}, 할인액={}, 최종금액={}",
                    couponId, originalAmount, discountAmount, finalAmount);
        } else {
            // 쿠폰 미사용 시에도 FINAL_AMOUNT 업데이트 (AMOUNT와 동일)
            bookingMapper.updateFinalAmount(request.getBookingId(), finalAmount);
        }

        // 6. 계좌이체인 경우 만료 시간 연장 (5분 → 7일)
        if ("TRANSFER".equals(request.getMethod())) {
            LocalDateTime newExpiresAt = LocalDateTime.now().plusDays(7);
            bookingMapper.updateExpiresAt(request.getBookingId(), newExpiresAt);
            log.info("계좌이체 예약 만료 시간 연장: bookingId={}, expiresAt={}", request.getBookingId(), newExpiresAt);
        }

        // 7. 결제 생성 (READY 상태, 쿠폰 정보 포함)
        Payment payment = Payment.builder()
                .status(PaymentStatus.PAY_READY.getCode())
                .amount(finalAmount)
                .method(request.getMethod())
                .bookingId(request.getBookingId())
                .paymentKey(null)
                .couponId(couponId)
                .originalAmount(originalAmount)
                .discountAmount(discountAmount)
                .requestedAt(OffsetDateTime.now())
                .build();

        paymentMapper.insertPayment(payment);

        // 8. 생성된 결제 조회
        Long paymentId = payment.getPaymentId();

        log.info("결제 준비 완료: paymentId={}, orderId={}, method={}", paymentId, bookingNum, request.getMethod());

        // 9. PreparePaymentResponse 생성 (가상계좌 정보는 Toss SDK가 처리)
        return PreparePaymentResponse.builder()
                .paymentId(paymentId)
                .orderId(bookingNum)  // Toss에 전달할 주문번호
                .amount(finalAmount)  // 할인 후 최종 금액 반환
                .method(request.getMethod())
                .clientKey(tossConfig.getClientKey())  // 프론트엔드용
                .customerName(guestName)
                .build();
    }

    /**
     * 결제 승인 (트랜잭션 분리)
     * Phase 1: 검증 (트랜잭션 없음)
     * Phase 2: Toss API 호출 (트랜잭션 없음)
     * Phase 3: DB 업데이트 (짧은 트랜잭션 + 비관적 락)
     */
    @Override
    public PaymentResultResponse confirmPayment(ConfirmPaymentRequest request, Long userId) {
        log.info("결제 승인 시작: userId={}, paymentKey={}, orderId={}, amount={}",
                userId, request.getPaymentKey(), request.getOrderId(), request.getAmount());

        // Phase 1: 검증 (트랜잭션 없음, 빠른 실패)
        PaymentValidationResult validation = validatePaymentRequest(request, userId);

        // Phase 2: Toss API 호출 (트랜잭션 없음)
        TossPaymentResponse tossResponse = callTossApiWithCompensation(
                request,
                validation.getBookingId(),
                validation.getPaymentId(),
                validation.getCouponId()
        );

        // Phase 3: DB 업데이트 (짧은 트랜잭션 + 비관적 락)
        return completePaymentWithLock(
                validation.getBookingId(),
                validation.getPaymentId(),
                validation.getCouponId(),
                request,
                tossResponse
        );
    }

    /**
     * Phase 1: 결제 검증 (트랜잭션 없음)
     * 락 없이 빠른 검증 수행
     */
    private PaymentValidationResult validatePaymentRequest(ConfirmPaymentRequest request, Long userId) {
        // 1. 예약 조회
        Booking booking = bookingMapper.findBookingByBookingNum(request.getOrderId());
        if (booking == null) {
            throw new PaymentFailedException("예약을 찾을 수 없습니다: " + request.getOrderId());
        }

        Long bookingId = booking.getBookingId();

        // 2. 예약 소유자 확인 (인가)
        validateBookingOwnership(bookingId, userId);

        // 3. 결제 조회 (락 없이)
        Payment payment = paymentMapper.findPaymentByBookingId(bookingId);
        if (payment == null) {
            throw new PaymentFailedException("결제 정보를 찾을 수 없습니다");
        }

        // 4. 이미 완료된 결제 체크 (빠른 실패)
        if ("PAY_PAID".equals(payment.getStatus())) {
            log.info("이미 완료된 결제: paymentId={}, bookingId={}", payment.getPaymentId(), bookingId);
            throw new PaymentFailedException("이미 완료된 결제입니다");
        }

        // 5. 금액 검증
        if (!payment.getAmount().equals(request.getAmount())) {
            log.error("결제 금액 불일치: 결제금액={}, 요청금액={}",
                    payment.getAmount(), request.getAmount());

            compensationService.compensateFailedPayment(
                    bookingId,
                    payment.getPaymentId(),
                    payment.getCouponId(),
                    "결제 금액 불일치"
            );

            throw new PaymentAmountMismatchException(payment.getAmount(), request.getAmount());
        }

        return new PaymentValidationResult(
                bookingId,
                payment.getPaymentId(),
                payment.getCouponId()
        );
    }

    /**
     * Phase 2: Toss API 호출 (트랜잭션 없음)
     * 실패 시 보상 트랜잭션 실행
     */
    private TossPaymentResponse callTossApiWithCompensation(
            ConfirmPaymentRequest request,
            Long bookingId,
            Long paymentId,
            Long couponId
    ) {
        try {
            TossConfirmRequest tossRequest = TossConfirmRequest.builder()
                    .paymentKey(request.getPaymentKey())
                    .orderId(request.getOrderId())
                    .amount(request.getAmount())
                    .build();

            return tossPaymentClient.confirm(tossRequest);

        } catch (TossApiException e) {
            log.error("Toss 결제 승인 실패: {}", e.getMessage(), e);

            compensationService.compensateFailedPayment(
                    bookingId,
                    paymentId,
                    couponId,
                    e.getTossErrorMessage()
            );

            throw e;

        } catch (Exception e) {
            log.error("결제 승인 중 예외 발생: {}", e.getMessage(), e);

            compensationService.compensateFailedPayment(
                    bookingId,
                    paymentId,
                    couponId,
                    "결제 처리 중 오류 발생"
            );

            throw new PaymentFailedException("결제 처리 중 오류가 발생했습니다");
        }
    }

    /**
     * Phase 3: DB 업데이트 (짧은 트랜잭션 + 비관적 락)
     * 동시성 제어 및 상태 재확인
     */
    @Transactional
    private PaymentResultResponse completePaymentWithLock(
            Long bookingId,
            Long paymentId,
            Long couponId,
            ConfirmPaymentRequest request,
            TossPaymentResponse tossResponse
    ) {
        // 비관적 락으로 결제 상태 재확인
        Payment payment = paymentMapper.findPaymentByBookingIdWithLock(bookingId);
        if (payment == null) {
            log.error("결제 정보 없음: bookingId={}", bookingId);
            compensationService.compensateTossPayment(tossResponse.getPaymentKey(), "결제 정보 없음");
            throw new PaymentFailedException("결제 정보를 찾을 수 없습니다");
        }

        // Double-check: 이미 다른 요청이 처리했는지 확인
        if (!"PAY_READY".equals(payment.getStatus())) {
            log.warn("동시 요청 감지: 이미 처리된 결제 paymentId={}, status={}",
                    payment.getPaymentId(), payment.getStatus());

            compensationService.compensateTossPayment(tossResponse.getPaymentKey(), "중복 요청 감지");
            throw new PaymentFailedException("이미 처리된 결제입니다");
        }

        // 토스 method 매핑
        String internalMethod = "가상계좌".equals(tossResponse.getMethod())
                ? "VIRTUAL_ACCOUNT"
                : tossResponse.getMethod();

        // 가상계좌 만료 시간 연장
        if ("가상계좌".equals(tossResponse.getMethod())) {
            LocalDateTime newExpiresAt = LocalDateTime.now().plusHours(24);
            bookingMapper.updateExpiresAt(bookingId, newExpiresAt);
            log.info("가상계좌 예약 만료 시간 연장: bookingId={}, expiresAt={}", bookingId, newExpiresAt);
        }

        // 가상계좌 정보 저장
        if (tossResponse.getVirtualAccount() != null) {
            VirtualAccount va = tossResponse.getVirtualAccount();
            paymentMapper.updateVirtualAccountInfo(
                    paymentId,
                    va.getBank(),
                    va.getAccountNumber(),
                    va.getCustomerName(),
                    va.getDueDate()
            );
            log.info("가상계좌 정보 저장: bank={}, accountNumber={}", va.getBank(), va.getAccountNumber());
        }

        // 결제 상태 업데이트
        String paymentStatus = "VIRTUAL_ACCOUNT".equals(internalMethod)
                ? PaymentStatus.PAY_READY.getCode()
                : PaymentStatus.PAY_PAID.getCode();

        paymentMapper.updatePaymentApproved(
                paymentId,
                paymentStatus,
                tossResponse.getPaymentKey(),
                tossResponse.getLastTransactionKey()
        );

        // 예약 상태 업데이트
        String bookingStatus = "VIRTUAL_ACCOUNT".equals(internalMethod)
                ? ReservationStatus.RES_PENDING.getCode()
                : ReservationStatus.RES_CONFIRMED.getCode();

        bookingMapper.updateBookingStatus(bookingId, bookingStatus);

        log.info("결제 승인 완료: paymentId={}, bookingId={}, status={}, method={}",
                paymentId, bookingId, paymentStatus, tossResponse.getMethod());

        // 이벤트 발행
        PaymentConfirmEvent event = new PaymentConfirmEvent(
                paymentId,
                bookingId,
                tossResponse.getTotalAmount(),
                couponId
        );
        eventPublisher.publishEvent(event);

        // 응답 생성
        return PaymentResultResponse.builder()
                .paymentId(paymentId)
                .paymentKey(tossResponse.getPaymentKey())
                .orderId(tossResponse.getOrderId())
                .bookingId(bookingId)
                .amount(tossResponse.getTotalAmount())
                .method(tossResponse.getMethod())
                .paymentStatus(paymentStatus)
                .reservationStatus(bookingStatus)
                .requestedAt(payment.getRequestedAt())
                .approvedAt(tossResponse.getApprovedAt())
                .virtualAccount(tossResponse.getVirtualAccount())
                .build();
    }

    /**
     * 예약 소유자 확인 (인가)
     * 본인의 예약만 결제 가능
     */
    private void validateBookingOwnership(Long bookingId, Long userId) {
        Booking booking = bookingMapper.findById(bookingId);

        if (booking == null) {
            throw new BookingNotFoundException(bookingId);
        }

        if (!booking.getUserId().equals(userId)) {
            log.warn("예약 소유자 불일치: bookingId={}, requestUserId={}, bookingOwnerId={}",
                    bookingId, userId, booking.getUserId());
            throw new ForbiddenException(ErrorCode.FORBIDDEN, "본인의 예약만 결제 가능합니다");
        }
    }

    /**
     * 결제 검증 결과 DTO
     */
    private static class PaymentValidationResult {
        private final Long bookingId;
        private final Long paymentId;
        private final Long couponId;

        public PaymentValidationResult(Long bookingId, Long paymentId, Long couponId) {
            this.bookingId = bookingId;
            this.paymentId = paymentId;
            this.couponId = couponId;
        }

        public Long getBookingId() { return bookingId; }
        public Long getPaymentId() { return paymentId; }
        public Long getCouponId() { return couponId; }
    }

}
