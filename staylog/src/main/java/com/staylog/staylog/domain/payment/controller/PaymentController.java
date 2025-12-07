package com.staylog.staylog.domain.payment.controller;

import com.staylog.staylog.domain.payment.dto.request.ConfirmPaymentRequest;
import com.staylog.staylog.domain.payment.dto.request.PreparePaymentRequest;
import com.staylog.staylog.domain.payment.dto.response.PaymentResultResponse;
import com.staylog.staylog.domain.payment.dto.response.PreparePaymentResponse;
import com.staylog.staylog.domain.payment.service.PaymentService;
import com.staylog.staylog.global.common.code.ErrorCode;
import com.staylog.staylog.global.common.code.SuccessCode;
import com.staylog.staylog.global.common.response.SuccessResponse;
import com.staylog.staylog.global.common.util.MessageUtil;
import com.staylog.staylog.global.exception.custom.UnauthorizedException;
import com.staylog.staylog.global.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 결제 API 컨트롤러
 */
@Tag(name = "PaymentController", description = "결제 API")
@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final MessageUtil messageUtil;

    /**
     * 결제 준비
     * POST /v1/payments/prepare
     */
    @Operation(summary = "결제 준비", description = "예약에 대한 결제를 준비합니다. " +
            "프론트엔드에서 Toss SDK(결제 모듈)를 초기화하는 데 필요한 정보를 반환함.")
    @PostMapping("/prepare")
    public ResponseEntity<SuccessResponse<PreparePaymentResponse>> preparePayment(
            @Valid @RequestBody PreparePaymentRequest request,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        Long userId = extractUserId(securityUser);
        log.info("결제 준비 요청: userId={}, bookingId={}, amount={}", userId, request.getBookingId(), request.getAmount());

        PreparePaymentResponse response = paymentService.preparePayment(request, userId);

        String message = messageUtil.getMessage(SuccessCode.PAYMENT_PREPARED.getMessageKey());
        String code = SuccessCode.PAYMENT_PREPARED.name();

        return ResponseEntity.ok(SuccessResponse.of(code, message, response));
    }

    /**
     * 결제 승인
     * POST /v1/payments/confirm
     */
    @Operation(summary = "결제 승인", description = "Toss 결제를 승인합니다. " +
            "성공 시 예약이 확정되며, 실패 시 자동으로 취소됩니다.")
    @PostMapping("/confirm")
    public ResponseEntity<SuccessResponse<PaymentResultResponse>> confirmPayment(
            @Valid @RequestBody ConfirmPaymentRequest request,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        Long userId = extractUserId(securityUser);
        log.info("결제 승인 요청: userId={}, paymentKey={}, orderId={}", userId, request.getPaymentKey(), request.getOrderId());

        PaymentResultResponse response = paymentService.confirmPayment(request, userId);

        String message = messageUtil.getMessage(SuccessCode.PAYMENT_COMPLETED.getMessageKey());
        String code = SuccessCode.PAYMENT_COMPLETED.name();

        return ResponseEntity.ok(SuccessResponse.of(code, message, response));
    }

    /**
     * SecurityUser에서 userId 추출
     */
    private Long extractUserId(SecurityUser securityUser) {
        if (securityUser == null) {
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다");
        }
        return securityUser.getUserId();
    }
}
