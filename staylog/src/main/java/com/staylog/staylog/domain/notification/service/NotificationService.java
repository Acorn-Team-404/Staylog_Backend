package com.staylog.staylog.domain.notification.service;

import com.staylog.staylog.domain.notification.dto.request.NotificationLimitRequest;
import com.staylog.staylog.domain.notification.dto.request.NotificationRequest;
import com.staylog.staylog.domain.notification.dto.request.ReadAllRequest;
import com.staylog.staylog.domain.notification.dto.request.ReadRequest;
import com.staylog.staylog.domain.notification.dto.response.DetailsResponse;
import com.staylog.staylog.domain.notification.dto.response.NotificationResponse;

import java.util.List;

public interface NotificationService {


    /**
     * 알림 데이터 저장 및 푸시
     *
     * @param notificationRequest 알림 데이터 + JSON 형태의 String Type 데이터
     * @param detailsResponse     반복적인 직렬화, 역직렬화를 막기 위한 온전한 Details 객체(프론트에 그대로 출력 가능)
     * @apiNote 알림 별로 필요한 상세데이터는 해당하는 이벤트리스너에서
     * JSON 형식으로 각 타입별 데이터에 맞게 notificationRequest.details 필드에 담아 가져온다.
     * notificationRequest를 사용해서 DB에 저장 후 details 필드의 값을 사용해,
     * NotificationResponse를 구성하여 사용자에게 PUSH한다.
     * @author 이준혁
     */
    public void saveNotification(NotificationRequest notificationRequest, DetailsResponse detailsResponse);


    /**
     * 알림 데이터 저장 및 푸시
     *
     * @param notificationRequest 알림 데이터 + JSON 형태의 String Type 데이터
     * @param detailsResponse     반복적인 직렬화, 역직렬화를 막기 위한 온전한 Details 객체(프론트에 그대로 출력 가능)
     * @apiNote 알림 별로 필요한 상세데이터는 해당하는 이벤트리스너에서
     * JSON 형식으로 각 타입별 데이터에 맞게 notificationRequest.details 필드에 담아 가져온다.
     * notificationRequest를 사용해서 DB에 저장 후 details 필드의 값을 사용해,
     * NotificationResponse를 구성하여 사용자에게 PUSH한다.
     * @author 이준혁
     */
    public void saveAllNotification(NotificationRequest notificationRequest, DetailsResponse detailsResponse);


    /**
     * 유저 한명의 알림 리스트 조회
     * @author 이준혁
     * @param notificationLimitRequest 부가정보 (userId, last 값, limit)
     * @return List<NotificationResponse>
     */
    public List<NotificationResponse> getNotificationList(NotificationLimitRequest notificationLimitRequest);


    /**
     * 알림 삭제
     * @author 이준혁
     * @param notiId 알림 PK
     */
    public void deleteNotification(long notiId);

    /**
     * 해당 유저의 알림 전체 삭제
     * @author 이준혁
     * @param userId 유저 PK
     */
    public void deleteNotificationAll(long userId);

    /**
     * 단일 알림 읽음 처리
     * @author 이준혁
     * @param readRequest 알림 PK
     */
    public void readOne(ReadRequest readRequest);

    /**
     * 모든 알림 읽음 처리
     * @author 이준혁
     * @param readAllRequest 유저 PK
     */
    public void readAll(ReadAllRequest readAllRequest);

    /**
     * 안읽은 알림 수 조회
     * @param userId 사용자 PK
     * @return 안읽은 알림 수
     */
    public int unreadCount(long userId);

    /**
     * 알림 데이터를 DB에 저장하기 위한 JSON 직렬화
     * @author 이준혁
     * @param detailsResponse 알림 데이터
     * @return detailsObject 직렬화된 문자열
     */
    public String detailsToJsonString(DetailsResponse detailsResponse);

    /**
     * 대표 이미지 1장 가져오는 메서드
     * @author 이준혁
     * @param targetType 이미지의 타겟 타입
     * @param targetId 이미지의 타겟 PK
     * @return 이미지 URL
     */
    public String getImageUrl(String targetType, long targetId);
}
