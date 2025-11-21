package com.staylog.staylog.domain.notification.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.staylog.staylog.domain.image.dto.ImageDto;
import com.staylog.staylog.domain.image.mapper.ImageMapper;
import com.staylog.staylog.domain.notification.dto.request.*;
import com.staylog.staylog.domain.notification.dto.response.*;
import com.staylog.staylog.domain.notification.mapper.NotificationMapper;
import com.staylog.staylog.domain.notification.service.NotificationService;
import com.staylog.staylog.global.common.code.ErrorCode;
import com.staylog.staylog.global.event.NotificationCreatedAllEvent;
import com.staylog.staylog.global.event.NotificationCreatedEvent;
import com.staylog.staylog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ImageMapper imageMapper;


    /**
     * 알림 데이터 저장
     *
     * @param notificationRequest 알림 데이터 + JSON 형태의 String Type 데이터
     * @param detailsResponse     반복적인 직렬화, 역직렬화를 막기 위한 온전한 Details 객체(프론트에 그대로 출력 가능)
     * @apiNote 알림 별로 필요한 상세데이터는 해당하는 이벤트리스너에서
     * JSON 형식으로 각 타입별 데이터에 맞게 notificationRequest.details 필드에 담아 가져온다.
     * notificationRequest를 사용해서 DB에 저장 후 details 필드의 값을 사용해,
     * NotificationResponse를 구성하여 SSE에 전달한다.
     * @author 이준혁
     */
    @Override
    @Transactional
    public void saveNotification(NotificationRequest notificationRequest, DetailsResponse detailsResponse) {

        // DB 저장
        int success = notificationMapper.notiSave(notificationRequest);
        if (success == 0) {
            log.error("알림 DB 저장 0건, 로직 확인 필요. userId: {}", notificationRequest.getUserId());
            throw new BusinessException(ErrorCode.NOTIFICATION_FAILED); // 재시도가 필요없는 코드 수준의 에러
        }

        log.info("알림 데이터 저장 완료. notiId: {}", notificationRequest.getNotiId());

        // 클라이언트에게 SSE로 푸시하기위한 객체 구성
        NotificationResponse notificationResponse = NotificationResponse.builder()
                .notiId(notificationRequest.getNotiId()) // selectKey로 채워진 알림 PK
                .targetId(notificationRequest.getTargetId()) // 페이지 이동을 위한 PK
                .details(detailsResponse)
                .isRead("N")
                .createdAt(LocalDateTime.now())
                .build();


        // =========== 알림 저장 이벤트 발행(SSE 푸시) ==============
        NotificationCreatedEvent event = new NotificationCreatedEvent(notificationRequest.getUserId(), notificationResponse);
        eventPublisher.publishEvent(event);
        // ======================================================

    }


    /**
     * 전체 사용자 일괄 알림 데이터 저장
     *
     * @param notificationRequest 알림 데이터 + JSON 형태의 String Type 데이터
     * @param detailsResponse     반복적인 직렬화, 역직렬화를 막기 위한 온전한 Details 객체(프론트에 그대로 출력 가능)
     * @apiNote 알림 별로 필요한 상세데이터는 해당하는 이벤트리스너에서
     * JSON 형식으로 각 타입별 데이터에 맞게 notificationRequest.details 필드에 담아 가져온다.
     * notificationRequest를 사용해서 DB에 저장 후 details 필드의 값을 사용해,
     * NotificationResponse를 구성하여 사용자에게 PUSH한다.
     * @author 이준혁
     */
    @Override
    @Transactional
    public void saveAllNotification(NotificationRequest notificationRequest, DetailsResponse detailsResponse) {

        // Users 테이블에서 userId를 매핑하여 모든 사용자에게 알림 저장
        int isSuccess = notificationMapper.notiSaveBatchFromUsers(notificationRequest);
        if (isSuccess == 0) {
            log.error("알림 DB 저장 0건, 로직 확인 필요. batchId: {}", notificationRequest.getBatchId());
            throw new BusinessException(ErrorCode.NOTIFICATION_FAILED); // 재시도가 필요없는 코드 수준의 에러
        }

        log.info("알림 데이터 일괄 저장 완료. batchId: {}, isSuccess: {}", notificationRequest.getBatchId(), isSuccess);

        // 클라이언트에게 SSE로 푸시하기위한 객체 구성
        NotificationResponse notificationResponse = NotificationResponse.builder()
                .notiId(null) // 단체 저장이므로 현재 null
                .targetId(null) // 단체 저장이므로 현재 null
                .details(detailsResponse)
                .isRead("N")
                .createdAt(LocalDateTime.now())
                .build();

        // =========== 단체 알림 저장 이벤트 발행(SSE 푸시) ==============
        NotificationCreatedAllEvent event = new NotificationCreatedAllEvent(notificationResponse, notificationRequest.getBatchId());
        eventPublisher.publishEvent(event);
        // ======================================================
    }


    /**
     * 유저 한명의 알림 리스트 조회
     *
     * @param notificationLimitRequest 부가정보 (userId, last 값, limit)
     * @return List<NotificationResponse>
     * @author 이준혁
     */
    @Override
    public List<NotificationResponse> getNotificationList(NotificationLimitRequest notificationLimitRequest) {

        // limit 설정을 안했을 시 방어 로직
        if(notificationLimitRequest.getLimit() == null) {
            // 기본값 limit 설정
            int defaultLimit = 15;
            notificationLimitRequest.setLimit(defaultLimit);
        }

        // 유저의 알림 리스트 조회
        List<NotificationSelectRequest> notiListFromDb = notificationMapper.findNotificationsByUserId(notificationLimitRequest);

        if (notiListFromDb == null || notiListFromDb.isEmpty()) {
            log.warn("알림 데이터 조회 실패: 알림이 없거나, 알림 정보를 찾을 수 없습니다. - userId={}", notificationLimitRequest.getUserId());
            return Collections.emptyList(); // 프론트의 catch를 실행시키지 않도록 throw 대신 빈 List를 반환
        }
        log.info("알림 데이터 조회 완료. userId: {}, length: {}", notificationLimitRequest.getUserId(), notiListFromDb.size());

        // map으로 순환하며 프론트에서 바로 사용할 수 있는 JSON으로 가공
        return notiListFromDb.stream().map((notiData) -> {

            // DetailsResponse 타입으로 역직렬화
            DetailsResponse detailsObject;
            try {
                detailsObject = objectMapper.readValue(
                        notiData.getDetails(),
                        DetailsResponse.class
                );

            } catch (Exception e) {
                log.error("목록 조회용 JSON 역직렬화 실패: {}", notiData.getDetails(), e);
                detailsObject = null; // (혹은 new DetailsResponse() 빈 객체)
            }

            // 새로운 JSON 객체 조합
            return NotificationResponse.builder()
                    .notiId(notiData.getNotiId()) // selectKey로 가져온 PK
                    .targetId(notiData.getTargetId())
                    .isRead(notiData.getIsRead())
                    .createdAt(notiData.getCreatedAt())
                    .details(detailsObject) // 변환된 객체
                    .build();
        }).toList();
    }


    /**
     * 알림 삭제
     *
     * @param notiId 알림 PK
     * @author 이준혁
     */
    @Override
    public void deleteNotification(long notiId) {

        int isSuccess = notificationMapper.deleteByNotiId(notiId);
        if (isSuccess == 0) {
            log.error("알림 데이터 삭제 실패: 알림 정보를 찾을 수 없습니다. - notiId={}", notiId);
            throw new BusinessException(ErrorCode.NOTIFICATION_FAILED); // 재시도가 필요없는 코드 수준의 에러
        }
        log.info("알림 데이터 삭제 완료. notiId: {}", notiId);
    }


    /**
     * 해당 유저의 알림 전체 삭제
     *
     * @param userId 유저 PK
     * @author 이준혁
     */
    public void deleteNotificationAll(long userId) {
        try {
            int isSuccess = notificationMapper.deleteAllByUserId(userId);
            log.info("알림 데이터 전체 삭제 완료. userId: {}, isSuccess={}", userId, isSuccess);
        } catch (BusinessException e) {
            log.error("알림 데이터 전체 삭제 실패: 알림 정보를 찾을 수 없습니다. - userId={}", userId);
            throw new BusinessException(ErrorCode.NOTIFICATION_FAILED); // 재시도가 필요없는 코드 수준의 에러
        }
    }


    /**
     * 단일 알림 읽음 처리
     *
     * @param readRequest 알림 PK
     * @author 이준혁
     */
    @Override
    public void readOne(ReadRequest readRequest) {
        int isSuccess = notificationMapper.readOne(readRequest);

        if (isSuccess == 0) {
            log.warn("알림 데이터 읽음 처리 실패: 알림 정보를 찾을 수 없습니다. - notiId={}", readRequest.getNotiId());
            throw new BusinessException(ErrorCode.NOTIFICATION_FAILED); // 재시도가 필요없는 코드 수준의 에러
        }
        log.info("알림 데이터 읽음 처리 완료. notiId={}", readRequest.getNotiId());
    }

    /**
     * 모든 알림 읽음 처리
     *
     * @param readAllRequest 유저 PK
     * @author 이준혁
     */
    @Override
    public void readAll(ReadAllRequest readAllRequest) {
        int isSuccess = notificationMapper.readAll(readAllRequest);

        if (isSuccess == 0) {
            log.error("알림 데이터 전체 읽음 처리 실패: 알림 정보를 찾을 수 없습니다. - userId={}", readAllRequest.getUserId());
            throw new BusinessException(ErrorCode.NOTIFICATION_FAILED);
        }
        log.info("전체 알림 데이터 읽음 처리 완료. userId={}, isSuccess={}", readAllRequest.getUserId(), isSuccess);
    }


    /**
     * 안읽은 알림 수 조회
     *
     * @param userId 사용자 PK
     * @return 안읽은 알림 수
     */
    @Override
    public int unreadCount(long userId) {
        int unreadCount;
        try {
            unreadCount = notificationMapper.unreadCount(userId);
        } catch (Exception e) {
            log.error("알림 데이터 unreadCount 조회 실패: 유저 정보를 찾을 수 없습니다. - userId={}", userId);
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        log.info("unreadCount 조회 완료. userId={}, unreadCount={}", userId, unreadCount);
        return unreadCount;
    }





    /**
     * 알림 데이터를 DB에 저장하기 위한 JSON 직렬화
     * @author 이준혁
     * @param detailsResponse 알림 데이터
     * @return detailsObject 직렬화된 문자열
     */
    @Override
    public String detailsToJsonString(DetailsResponse detailsResponse) {
        try { // 알림 데이터를 DB에 저장하기 위한 JSON 형태의 String 문자열 구성
            String detailsObject = objectMapper.writeValueAsString(detailsResponse);
            log.info("알림 데이터 직렬화 완료");
            return detailsObject;
        } catch (JsonProcessingException e) {
            log.error("알림 데이터 직렬화 중 오류 발생");
            throw new BusinessException(ErrorCode.NOTIFICATION_FAILED);
        }
    }


    /**
     * 대표 이미지 1장 가져오는 메서드
     * @author 이준혁
     * @param targetType 이미지의 타겟 타입
     * @param targetId 이미지의 타겟 PK
     * @return 이미지 URL
     */
    @Override
    public String getImageUrl(String targetType, long targetId) {
        ImageDto imageDto = ImageDto.builder()
                .targetType(targetType)
                .targetId(targetId)
                .build();
        String imageUrl = imageMapper.getMainImgByTargetTypeAndId(imageDto);

        if(imageUrl != null) {
            log.info("이미지 조회 완료. imageUrl: {}", imageUrl);
            return imageUrl;
        } else {
            // 이미지가 없을 경우 기본 이미지로 출력
            log.info("이미지 조회 실패 / 기본 이미지로 출력. targetType: {}, targetId: {}", targetType, targetId);
            ImageDto defaultImageDto = ImageDto.builder()
                    .targetType("IMG_FROM_ICON")
                    .targetId(3)
                    .build();
            return imageMapper.getMainImgByTargetTypeAndId(defaultImageDto);
        }

    }


}
