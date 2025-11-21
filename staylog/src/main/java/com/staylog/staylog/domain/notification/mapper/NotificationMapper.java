package com.staylog.staylog.domain.notification.mapper;

import com.staylog.staylog.domain.notification.dto.request.*;
import com.staylog.staylog.domain.notification.dto.response.NotificationUserMapping;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Set;

@Mapper
public interface NotificationMapper {

    /**
     * 알림 데이터 저장
     * @author 이준혁
     * @param notificationRequest 알림 정보
     * @return 성공 여부 1 또는 0
     */
    public int notiSave(NotificationRequest notificationRequest);

//    /**
//     * 전체 사용자 알림 일괄 데이터 저장(batch_id를 추가하면서 더이상 사용하지 않음)
//     * @author 이준혁
//     * @param notificationRequest 알림 정보
//     * @return 성공 여부 1 또는 0
//     */
//    public int notiSaveBroadcast(NotificationRequest notificationRequest);

    /**
     * 전체 사용자 알림 일괄 데이터 저장(batch 추가 삽입)
     * @author 이준혁
     * @param notificationRequest 알림 정보
     * @return 성공 여부 1 또는 0
     */
    public int notiSaveBatchFromUsers(NotificationRequest notificationRequest);


    /**
     * batchId로 저장된 알림 데이터 중 현재 접속 중인 유저의 noti_id와 user_id 조회
     * @author 이준혁
     * @param batchId 일괄 저장 uuid
     * @param userIds 현재 접속 중인 사용자의 PK List
     * @return NotificationUserMapping notiId와 userId로 이루어진 객체 List
     */
    public List<NotificationUserMapping> findByBatchIdAndUserIds(String batchId, Set<Long> userIds);


    /**
     * 유저 한명의 알림 리스트 조회
     * @author 이준혁
     * @param notificationLimitRequest 부가정보 (userId, last 값, limit)
     * @return NotificationResponse
     */
    public List<NotificationSelectRequest> findNotificationsByUserId(NotificationLimitRequest notificationLimitRequest);



    /**
     * 알림 삭제
     * @author 이준혁
     * @param notiId 알림 PK
     * @return 성공 여부 1 또는 0
     */
    public int deleteByNotiId(long notiId);

    /**
     * 해당 유저의 알림 전체 삭제
     * @author 이준혁
     * @param userId 유저 PK
     */
    public int deleteAllByUserId(long userId);


    /**
     * 단일 알림 읽음 처리
     * @author 이준혁
     * @param readRequest 알림 PK
     * @return 성공 여부 1 또는 0
     */
    public int readOne(ReadRequest readRequest);

    /**
     * 모든 알림 읽음 처리
     * @author 이준혁
     * @param readAllRequest 유저 PK
     * @return 성공 여부 1 또는 0
     */
    public int readAll(ReadAllRequest readAllRequest);

    /**
     * 안읽은 알림 수 조회
     * @author 이준혁
     * @param userId 사용자 PK
     * @return 안읽은 알림 수
     */
    public int unreadCount(long userId);
}
