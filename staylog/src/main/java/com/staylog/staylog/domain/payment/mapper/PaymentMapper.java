package com.staylog.staylog.domain.payment.mapper;

import com.staylog.staylog.domain.payment.dto.response.PreparePaymentResponse;
import com.staylog.staylog.domain.payment.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;

/**
 * 결제 Mapper 인터페이스
 */
@Mapper
public interface PaymentMapper {

    /**
     * 결제 생성
     * @param params 결제 정보 (status, amount, method, bookingId, paymentKey)
     */
    void insertPayment(Payment params);

    /**
     * 결제 조회 (paymentId) - Payment Entity 반환
     * @param paymentId 결제 ID
     * @return 결제 정보
     */
    Payment findPaymentById(@Param("paymentId") Long paymentId);

    /**
     * 결제 조회 (bookingId) - Payment Entity 반환
     * @param bookingId 예약 ID
     * @return 결제 정보
     */
    Payment findPaymentByBookingId(@Param("bookingId") Long bookingId);

    /**
     * 결제 조회 (bookingId) + 비관적 락 (FOR UPDATE)
     * 동시성 제어를 위해 행 락을 획득하여 조회
     * @param bookingId 예약 ID
     * @return 결제 정보
     */
    Payment findPaymentByBookingIdWithLock(@Param("bookingId") Long bookingId);

    /**
     * @param orderId -> Reservation의 BookingNum
     * @return Payment엔티티 bookingId 포함
     */
    Payment findByOrderId(@Param("orderId") String orderId);

    /**
     * 결제 조회 (paymentKey) - Payment Entity 반환
     * @param paymentKey Toss 결제 키
     * @return 결제 정보
     */
    Payment findPaymentByPaymentKey(@Param("paymentKey") String paymentKey);


    /**
     * Payment 상태 업데이트
     */
    int updatePaymentStatus(@Param("paymentId") Long paymentId,
                            @Param("paymentKey") String paymentKey,
                            @Param("status") String status,
                            @Param("approvedAt") OffsetDateTime approvedAt);
    /**
     * 결제 상태 업데이트 (승인 성공 시)
     * @param paymentId 결제 ID
     * @param status 변경할 상태 (PAY_PAID, PAY_FAILED, etc.)
     * @param paymentKey Toss 결제 키
     * @param lastTransactionKey 마지막 트랜잭션 키
     */
    void updatePaymentApproved(@Param("paymentId") Long paymentId,
                             @Param("status") String status,
                             @Param("paymentKey") String paymentKey,
                             @Param("lastTransactionKey") String lastTransactionKey);

    /**
     * 결제 실패 정보 업데이트
     * @param paymentId 결제 ID
     * @param status 변경할 상태 (PAY_FAILED)
     * @param failureReason 실패 사유
     */
    void updatePaymentFailure(@Param("paymentId") Long paymentId,
                              @Param("status") String status,
                              @Param("failureReason") String failureReason);


    /**
     * 쿠폰 PK 조회
     * @param paymentId 결제 PK
     * @return couponId
     */
    Long findCouponIdByPaymentId(@Param("paymentId") Long paymentId);

    /**
     * 가상계좌 정보 업데이트 (Toss 응답에서 받은 정보 저장)
     * @param paymentId 결제 ID
     * @param bank 은행명
     * @param accountNumber 계좌번호
     * @param customerName 예금주
     * @param dueDate 입금 기한
     */
    void updateVirtualAccountInfo(@Param("paymentId") Long paymentId,
                                  @Param("bank") String bank,
                                  @Param("accountNumber") String accountNumber,
                                  @Param("customerName") String customerName,
                                  @Param("dueDate") OffsetDateTime dueDate);


    void updateVirtualAccountDeposit(@Param("paymentId")Long paymentId,
                                     @Param("code") String code,
                                     @Param("approvedAt") OffsetDateTime approvedAt);
}
