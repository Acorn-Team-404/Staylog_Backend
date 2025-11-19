package com.staylog.staylog.domain.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staylog.staylog.domain.booking.entity.Booking;
import com.staylog.staylog.domain.booking.mapper.BookingMapper;
import com.staylog.staylog.domain.payment.entity.Payment;
import com.staylog.staylog.domain.payment.mapper.PaymentMapper;
import com.staylog.staylog.external.toss.config.TossPaymentsConfig;
import com.staylog.staylog.external.toss.dto.request.TossVirtualAccountWebhookRequest;
import com.staylog.staylog.global.constant.PaymentStatus;
import com.staylog.staylog.global.constant.ReservationStatus;
import com.staylog.staylog.global.event.PaymentConfirmEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/v2/payments/webhook")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final TossPaymentsConfig tossConfig;
    private final PaymentMapper paymentMapper;
    private final BookingMapper bookingMapper;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * v2 가상계좌 웹훅 수신
     */
    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody String payload) {
        log.info("========== v2 가상계좌 웹훅 수신 ==========");
        log.info("Payload: {}", payload);

        try {
            // payload JSON -> DTO
            TossVirtualAccountWebhookRequest webhookRequest = objectMapper.readValue(
                    payload, TossVirtualAccountWebhookRequest.class);

            String status = webhookRequest.getStatus();
            log.info("입금 상태: {}", status);

            if (!"DONE".equalsIgnoreCase(status)) {
                log.info("처리 불필요한 이벤트: {}", status);
                return ResponseEntity.ok("OK");
            }

            String orderId = webhookRequest.getOrderId();
            String transactionKey = webhookRequest.getTransactionKey();
            log.info("orderId={}, transactionKey={}", orderId, transactionKey);

            // 예약 조회 하기
            Booking booking = bookingMapper.findBookingByBookingNum(orderId);
            if (booking == null) {
                log.warn("예약을 찾을 수 없음: orderId={}", orderId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Booking not found");
            }
            Long bookingId = booking.getBookingId();

            // 결제 조회
            Payment payment = paymentMapper.findPaymentByBookingId(bookingId);
            if (payment == null) {
                log.warn("결제를 찾을 수 없음: bookingId={}", bookingId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Payment not found");
            }
            Long paymentId = payment.getPaymentId();
            String currentStatus = payment.getStatus();

            // 이미 처리된 결제인지 확인 (멱등성)
            if (PaymentStatus.PAY_PAID.getCode().equals(currentStatus)) {
                log.info("이미 처리된 가상계좌 입금: paymentId={}", paymentId);
                return ResponseEntity.ok("Already processed");
            }

            //  결제 승인: PAYMENT(PAID) + RESERVATION(CONFIRMED)
            paymentMapper.updateVirtualAccountDeposit(paymentId,
                    PaymentStatus.PAY_PAID.getCode(),
                    OffsetDateTime.now());

            bookingMapper.updateBookingStatus(bookingId, ReservationStatus.RES_CONFIRMED.getCode());
            log.info("v2 가상계좌 웹훅 처리 완료: paymentId={}, bookingId={}", paymentId, bookingId);

            // 결제 완료 이벤트 발행
            PaymentConfirmEvent event = new PaymentConfirmEvent(
                    paymentId,
                    bookingId,
                    payment.getAmount(),
                    payment.getCouponId()
            );
            eventPublisher.publishEvent(event);

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("v2 가상계좌 웹훅 처리 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }
}
