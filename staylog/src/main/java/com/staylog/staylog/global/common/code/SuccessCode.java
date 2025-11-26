package com.staylog.staylog.global.common.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 성공 코드 정의
 * 모든 성공 메시지 코드를 중앙 집중식으로 관리
 *
 * @author 임채호
 */
@Getter
@RequiredArgsConstructor
public enum SuccessCode {

    // ==================== 공통 성공 (0xxx) ====================
    SUCCESS(200, "S0001", "success.common"),
    CREATED(201, "S0002", "success.created"),
    UPDATED(200, "S0003", "success.updated"),
    DELETED(200, "S0004", "success.deleted"),

    // ==================== 인증/인가 성공 (1xxx) ====================
    LOGIN_SUCCESS(200, "S1001", "success.auth.login"),
    LOGOUT_SUCCESS(200, "S1002", "success.auth.logout"),
    TOKEN_REFRESHED(200, "S1003", "success.auth.token.refreshed"),
    SIGNUP_SUCCESS(201, "S1004", "success.auth.signup"),

    // ==================== 이메일 인증 성공 (3xxx) ====================
    MAIL_SENT(200, "S3001", "success.mail.sent"),
    MAIL_VERIFIED(200, "S3002", "success.mail.verified"),

    // ==================== 회원 관련 성공 (2xxx) ====================
    USER_PROFILE_UPDATED(200, "S2001", "success.user.profile.updated"),
    PASSWORD_CHANGED(200, "S2002", "success.user.password.changed"),
    USER_NICKNAME_CHECKED(200, "S2003", "success.user.nickname.checked"),
    USER_LOGINID_CHECKED(200, "S2004", "success.user.loginId.checked"),
    USER_EMAIL_CHECKED(200, "S2005", "success.user.email.checked"),

    // ==================== 숙소 관련 성공 (4xxx) ====================
    ACCOMMODATION_CREATED(201, "S4001", "success.accommodation.created"),
    ACCOMMODATION_UPDATED(200, "S4002", "success.accommodation.updated"),
    ACCOMMODATION_FOUND(200, "S4003", "success.accommodation.found"),
    ACCOMMODATION_LIST_FOUND(200, "S4004", "success.accommodation.list.found"),
    ACCOMMODATION_REVIEW_LIST_FOUND(200,"S4005", "success.accommodation.review.list.found"),

    // ==================== 예약 관련 성공 (5xxx) ====================
    BOOKING_CREATED(201, "S5001", "success.booking.created"),
    BOOKING_CANCELLED(200, "S5002", "success.booking.cancelled"),
    BOOKING_STATUS_UPDATED(200, "S5003", "success.booking.status.updated"),

    // ==================== 결제 관련 성공 (6xxx) ====================
    PAYMENT_PREPARED(200, "S6001", "success.payment.prepared"),
    PAYMENT_COMPLETED(200, "S6002", "success.payment.completed"),
    WEBHOOK_PROCESSED(200, "S6003", "success.payment.webhook.processed"),

    // ==================== 게시판 관련 성공 (7xxx) ====================
    BOARD_LIST_FETCHED(200, "S7004", "success.board.list"),
    BOARD_DETAIL_FETCHED(200, "S7005", "success.board.detail"),
    BOARD_CREATED(201, "S7001", "success.board.created"),
    BOARD_UPDATED(200, "S7002", "success.board.updated"),
    BOARD_DELETED(200, "S7003", "success.board.deleted"),
    BOARD_LIKES_FETCHED(200, "S7006", "success.board.likes"),

    // ==================== 이미지 관련 성공 (8xxx) ====================
    IMAGE_UPLOAD_SUCCESS(201, "S8001", "success.image.uploaded"),
  
    // ==================== 알림 관련 성공 (9xxx) ====================
    NOTIFICATION_CREATE(201, "S9001", "success.notification.created"),
    NOTIFICATION_LIST_FIND(200, "S9002", "success.notification.list.find"),
    NOTIFICATION_DELETE(200, "S9003", "success.notification.delete"),
    NOTIFICATION_READ(200, "S9004", "success.notification.read"),
    NOTIFICATION_UNREAD_COUNT(200, "S9005", "success.notification.unread-count"),

	//===================== 객실 관련 성공 (10xxx)=====================
	ROOM_SUCCESS(200, "S10001", "success.room"),
	
	//===================== 댓글 관련 성공 (11xxx)=====================
	COMMENTS_LIST_FETCHED(200, "S11001", "success.comments.list"),
	COMMENTS_CREATED(201, "S11002", "success.comments.created"),
	COMMENTS_UPDATED(200, "S11003", "success.comments.updated"),
	COMMENTS_DELETED(200, "S11004", "success.comments.deleted"),

	//===================== 환불 관련 성공 (12xxx)=====================
	REFUND_REQUEST_SUCCESS(201, "S12001", "success.refund.request"),
	REFUND_PROCESS_SUCCESS(200, "S12002", "success.refund.process"),
  
  // ==================== 쿠폰 관련 성공 (13xxx) ====================
  COUPON_LIST_FIND(200, "S13001", "success.coupon.list.find"),
  COUPON_CREATE(201, "S13002", "success.coupon.created"),
  COUPON_USED(201, "S13003", "success.coupon.used"),
  COUPON_DELETED(201, "S13004", "success.coupon.deleted");



    private final int httpStatus;
    private final String code;
    private final String messageKey;
}
