package com.staylog.staylog.domain.booking.service.impl;

import com.staylog.staylog.domain.booking.dto.request.CreateBookingRequest;
import com.staylog.staylog.domain.booking.dto.response.BookingDetailResponse;
import com.staylog.staylog.domain.booking.entity.Booking;
import com.staylog.staylog.domain.booking.mapper.BookingMapper;
import com.staylog.staylog.domain.booking.service.BookingService;
import com.staylog.staylog.global.constant.ReservationStatus;
import com.staylog.staylog.global.exception.custom.booking.BookingExpiredException;
import com.staylog.staylog.global.exception.custom.booking.BookingNotFoundException;
import com.staylog.staylog.global.exception.custom.booking.BookingNotPendingException;
import com.staylog.staylog.global.exception.custom.booking.RoomNotAvailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 예약 서비스 구현
 * - 예약 생성 (PENDING 상태)
 * - 객실 가용성 검증
 * - 5분 타임아웃 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingMapper bookingMapper;

    /**
     * 예약 생성
     * - 객실 가용성 검증
     * - 예약 생성 (PENDING 상태)
     * - 5분 타임아웃 설정
     */
    @Override
    @Transactional
    @CacheEvict(value = "searchResults", allEntries = true)
    public BookingDetailResponse createBooking(Long userId, String guestName, CreateBookingRequest request) {
        log.info("예약 생성 시작: userId={}, guestName={}, roomId={}, checkIn={}, checkOut={}",
                userId, guestName, request.getRoomId(), request.getCheckIn(), request.getCheckOut());

        // 1. 객실 가용성 체크 (중복 예약 확인)
        int conflictCount = bookingMapper.checkRoomAvailability(
                request.getRoomId(),
                request.getCheckIn(),
                request.getCheckOut()
        );

        if (conflictCount > 0) {
            log.warn("객실 예약 불가: roomId={}, 중복 예약 {}건", request.getRoomId(), conflictCount);
            throw new RoomNotAvailableException();
        }

        // 2. 주문번호 생성
        String bookingNum = generateBookingNum();

        // 3. 인원 계산
        int adults = Optional.ofNullable(request.getAdults()).orElse(1);
        int children = Optional.ofNullable(request.getChildren()).orElse(0);
        int infants = Optional.ofNullable(request.getInfants()).orElse(0);
        int total = adults + children + infants;

        // 4. 만료 시간 계산
        LocalDateTime expiresAt = calculateExpiresAt(request.getPaymentMethod());

        // 5. 예약 엔티티 생성
        Booking booking = Booking.builder()
                .userId(userId)
                .roomId(request.getRoomId())
                .bookingNum(bookingNum)
                .amount(request.getAmount())
                .finalAmount(request.getAmount())
                .checkIn(request.getCheckIn())
                .checkOut(request.getCheckOut())
                .status(ReservationStatus.RES_PENDING.getCode())
                .guestName(guestName)
                .adults(adults)
                .children(children)
                .infants(infants)
                .totalGuestCount(total)
                .expiresAt(expiresAt)
                .isWrited("N")
                .build();

        // DB insert → bookingId 자동 채워짐
        bookingMapper.insertBooking(booking);

        // 6. bookingId로 상세 조회
        BookingDetailResponse result = bookingMapper.findBookingById(booking.getBookingId());

        if (result == null) {
            throw new RuntimeException("예약 생성 실패");
        }

        return result;
    }

    /**
     * 예약 조회
     */
    @Override
    @Transactional(readOnly = true)
    public BookingDetailResponse getBooking(Long bookingId) {
        BookingDetailResponse booking = bookingMapper.findBookingById(bookingId);

        if (booking == null) {
            throw new BookingNotFoundException(bookingId);
        }

        return booking;
    }

    /**
     * 예약 상태가 PENDING인지 검증 (결제 준비 전)
     * - 만료 체크 (EXPIRES_AT 기준)
     * - PENDING 상태 체크
     */
    @Override
    @Transactional(readOnly = true)
    public void validateBookingPending(Long bookingId) {
        BookingDetailResponse booking = bookingMapper.findBookingById(bookingId);

        if (booking == null) {
            throw new BookingNotFoundException(bookingId);
        }

        // DB에서 조회한 상태 문자열을 ReservationStatus enum으로 변환함
        ReservationStatus status = ReservationStatus.fromCode(booking.getStatus());

        // 만료 체크 (EXPIRES_AT 기준)
        LocalDateTime expiresAt = booking.getExpiresAt();
        if (expiresAt.isBefore(LocalDateTime.now())) {
            log.warn("예약 만료: bookingId={}, expiresAt={}", bookingId, expiresAt);
            throw new BookingExpiredException(bookingId);
        }

        // 예약 상태가 PENDING 상태 체크
        if (status != ReservationStatus.RES_PENDING) {
            log.warn("예약이 PENDING 상태가 아님: bookingId={}, status={}", bookingId, status);
            throw new BookingNotPendingException(status);
        }
    }

    /**
     * 예약 상태 업데이트
     */
    @Override
    @Transactional
    @CacheEvict(value = "searchResults", allEntries = true)
    public void updateBookingStatus(Long bookingId, String status) {
        bookingMapper.updateBookingStatus(bookingId, status);
        log.info("예약 상태 업데이트: bookingId={}, status={}", bookingId, status);
    }

    /**
     * 만료된 예약 자동 취소
     * - EXPIRES_AT이 현재 시간보다 이전인 PENDING 예약을 CANCELED로 변경
     * - 결제 수단에 따라 만료 시간이 다름 (카드 5분, 가상계좌 7일 등)
     */
    @Override
    @Transactional
    @CacheEvict(value = "searchResults", allEntries = true)
    public int cancelExpiredBookings() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> expiredBookings = bookingMapper.findExpiredBookings(now);

        if (expiredBookings.isEmpty()) {
            return 0;
        }

        int canceledCount = 0;
        for (Booking booking : expiredBookings) {
            Long bookingId = booking.getBookingId();
            String bookingNum = booking.getBookingNum();

            try {
                bookingMapper.updateBookingStatus(bookingId, ReservationStatus.RES_CANCELED.getCode());
                log.info("만료된 예약 취소: bookingId={}, bookingNum={}", bookingId, bookingNum);
                canceledCount++;
            } catch (Exception e) {
                log.error("예약 취소 실패: bookingId={}, error={}", bookingId, e.getMessage(), e);
            }
        }

        return canceledCount;
    }

    /**
     * 주문번호 생성
     * 형식: ORDER_{timestamp}_{UUID 앞 8자리}
     */
    private String generateBookingNum() {
        return "ORDER_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * 만료 시간 계산
     *
     * 예약 생성 시점에는 결제 수단을 알 수 없으므로 기본값 5분으로 설정
     * 결제 준비 시점에 결제 수단에 따라 만료 시간을 연장함
     *
     * @param paymentMethod 결제 수단 (사용 안 함, 하위 호환성 유지용)
     * @return 만료 시간 (5분 후)
     */
    private LocalDateTime calculateExpiresAt(String paymentMethod) {
        // 모든 예약 5분으로 생성
        // 계좌이체는 결제 준비 시점에 7일로 연장
        return LocalDateTime.now().plusMinutes(5);
    }
}
