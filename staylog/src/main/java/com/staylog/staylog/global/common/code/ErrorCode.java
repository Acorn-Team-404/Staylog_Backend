package com.staylog.staylog.global.common.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 정의
 * 모든 비즈니스 에러 코드를 중앙 집중식으로 관리
 *
 * @author 임채호
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ==================== 공통 에러 (0xxx) ====================
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E0001", "error.internal"),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "E0002", "error.invalid.input"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "E0003", "error.method.not.allowed"),
    TEMPORARY_SERVER_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "E0004", "error.temporary.server"), // 503 (재시도 O)

    // ==================== 인증/인가 에러 (1xxx) ====================
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "E1001", "error.unauthorized"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "E1002", "error.invalid.credentials"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "E1003", "error.token.expired"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "E1004", "error.token.invalid"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "E1005", "error.forbidden"),

    // ==================== 회원 관련 에러 (2xxx) ====================
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "E2001", "error.user.not.found"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "E2002", "error.user.duplicate.email"),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "E2003", "error.user.duplicate.nickname"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "E2004", "error.user.disabled"),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "E2005", "error.user.password.mismatch"),
    DUPLICATE_LOGINID(HttpStatus.CONFLICT, "E2006", "error.user.duplicate.loginId"),


    // ==================== 이메일 인증 에러 (3xxx) ====================
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "E3001", "error.mail.send.failed"),
    VERIFICATION_CODE_INVALID(HttpStatus.BAD_REQUEST, "E3002", "error.mail.verification.invalid"),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "E3003", "error.mail.verification.expired"),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "E3004", "error.mail.not.verified"),
    EMAIL_ALREADY_VERIFIED(HttpStatus.BAD_REQUEST, "E3005", "error.mail.already.verified"),
    EMAIL_VERIFIED_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "E3006", "error.mail.verified.failed"),
    EMAIL_DELETED_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "E3007", "error.mail.deleted.failed"),

    // ==================== 숙소 관련 에러 (4xxx) ====================
    ACCOMMODATION_NOT_FOUND(HttpStatus.NOT_FOUND, "E4001", "error.accommodation.not.found"),
    ACCOMMODATION_LIST_NOT_FOUND(HttpStatus.NOT_FOUND,"E4002", "error.accommodation.list.not.found"),
    ACCOMMODATION_REVIEW_LIST_NOT_FOUND(HttpStatus.NOT_FOUND, "E4003", "error.accommodation.review.list.not.found"),
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "E4004", "error.room.not.found"),
    ROOM_LIST_NOT_FOUND(HttpStatus.NOT_FOUND, "E4005", "error.room.list.not.found"),
    ROOM_NOT_AVAILABLE(HttpStatus.CONFLICT, "E4006", "error.room.not.available"),
    DATE_NOT_FOUND(HttpStatus.BAD_REQUEST,"E4007","error.room.bookingDate.not.found"),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "E4008", "error.room.invalid.date.range"),
    ADDR_NOT_FOUND(HttpStatus.BAD_REQUEST, "E4009", "error.accommodation.addr.not.found"),

    // ==================== 예약 관련 에러 (5xxx) ====================
    BOOKING_NOT_FOUND(HttpStatus.NOT_FOUND, "E5001", "error.booking.not.found"),
    BOOKING_ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, "E5002", "error.booking.already.cancelled"),
    BOOKING_CANNOT_CANCEL(HttpStatus.BAD_REQUEST, "E5003", "error.booking.cannot.cancel"),
    INVALID_BOOKING_DATE(HttpStatus.BAD_REQUEST, "E5004", "error.booking.invalid.date"),
    BOOKING_EXPIRED(HttpStatus.BAD_REQUEST, "E5005", "error.booking.expired"),
    BOOKING_NOT_PENDING(HttpStatus.BAD_REQUEST, "E5006", "error.booking.not.pending"),

    // ==================== 결제 관련 에러 (6xxx) ====================
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "E6001", "error.payment.not.found"),
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "E6002", "error.payment.failed"),
    PAYMENT_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "E6003", "error.payment.already.completed"),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "E6004", "error.payment.amount.mismatch"),
    REFUND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "E6005", "error.payment.refund.failed"),
    PAYMENT_EXPIRED(HttpStatus.BAD_REQUEST, "E6006", "error.payment.expired"),
    PAYMENT_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "E6007", "error.payment.timeout"),
    TOSS_API_ERROR(HttpStatus.BAD_GATEWAY, "E6008", "error.payment.toss.api"),
    PAYMENT_CANCELLED_BY_USER(HttpStatus.BAD_REQUEST, "E6009", "error.payment.cancelled.by.user"),
    PAYMENT_ALREADY_REFUNDED(HttpStatus.BAD_REQUEST, "E6010", "error.payment.already.refunded"),
    WEBHOOK_SIGNATURE_INVALID(HttpStatus.UNAUTHORIZED, "E6011", "error.payment.webhook.signature.invalid"),

    // ==================== 게시판 관련 에러 (7xxx) ====================
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "E7001", "error.board.not.found"),
    BOARD_ALREADY_EXISTS(HttpStatus.CONFLICT, "E7002", "error.board.already.exists"),
    BOARD_NOT_AUTHORIZED(HttpStatus.FORBIDDEN, "E7003", "error.board.not.authorized"),
    BOARD_CREATE_FAILED(HttpStatus.BAD_REQUEST, "E7004", "error.board.not.created"),
    BOARD_INVALID_INPUT(HttpStatus.EXPECTATION_FAILED, "E7005", "error.board.invalid.input"),

    // xxxxxxxxxxxxxxxxxxxx 저널 관련 에러 (8xxx) 사용안함 xxxxxxxxxxxxxx
    JOURNAL_NOT_FOUND(HttpStatus.NOT_FOUND, "E8001", "error.journal.not.found"),
    JOURNAL_NOT_AUTHORIZED(HttpStatus.FORBIDDEN, "E8002", "error.journal.not.authorized"),

    // ==================== 파일 관련 에러 (9xxx) ====================
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "E9001", "error.file.upload.failed"),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "E9002", "error.file.size.exceeded"),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "E9003", "error.file.invalid.type"),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "E9004", "error.file.not.found"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "E9005", "error.resource.not.found"),

    // ==================== 알림 관련 에러 (10xxx) ====================
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "E10001", "error.notification.not.found"),
    NOTIFICATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "E10002", "error.notification.failed"),
    NOTIFICATION_SUBSCRIBE_FAILED(HttpStatus.BAD_REQUEST, "E10003", "error.notification.subscribe.failed"),
    NOTIFICATION_EMITTER_NOT_FOUND(HttpStatus.NOT_FOUND, "E10004", "error.notification.emitter.not.found"),


	// ==================== 댓글 관련 에러 (11xxx) ====================
    COMMENTS_NOT_FOUND(HttpStatus.NOT_FOUND, "E11001", "error.comments.not.found"),
    COMMENTS_FAILED_CREATED(HttpStatus.BAD_REQUEST, "E11002", "error.comments.failed.create"),
    COMMENTS_FAILED_UPDATED(HttpStatus.BAD_REQUEST, "E11003", "error.comments.failed.update"),
    COMMENTS_FAILED_DELETED(HttpStatus.BAD_REQUEST, "E11004", "error.comments.failed.delete"),
    COMMENTS_NOT_AUTHORIZED(HttpStatus.FORBIDDEN, "E11005", "error.comments.not.authorized"),
    BOARD_NOT_FOUND_FOR_COMMENT(HttpStatus.NOT_FOUND, "E11006", "error.board.not.found.for.comment"),

    // ==================== 환불 관련 에러 (12xxx) ====================
    REFUND_NOT_FOUND(HttpStatus.NOT_FOUND, "E12001", "error.refund.not.found"),
    REFUND_POLICY_VIOLATION(HttpStatus.BAD_REQUEST, "E12002", "error.refund.policy.violation"),
    REFUND_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "E12003", "error.refund.already.processed"),
    REFUND_AMOUNT_INVALID(HttpStatus.BAD_REQUEST, "E12004", "error.refund.amount.invalid"),
    TOSS_REFUND_API_ERROR(HttpStatus.BAD_GATEWAY, "E12005", "error.refund.toss.api"),


    // ==================== 쿠폰 관련 에러 (13xxx) ====================
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "E13001", "error.coupon.not.found"),
    COUPON_FAILED_USED(HttpStatus.BAD_REQUEST, "E13002", "error.coupon.failed.used"),
    COUPON_FAILED_UNUSED(HttpStatus.BAD_REQUEST, "E13003", "error.coupon.failed.unused"),
    COUPON_FAILED_CREATED(HttpStatus.BAD_REQUEST, "E13004", "error.coupon.failed.created"),
    COUPON_FAILED_DELETED(HttpStatus.BAD_REQUEST, "E13005", "error.coupon.failed.deleted");

    private final HttpStatus status;
    private final String code;
    private final String messageKey;
}
