package com.staylog.staylog.domain.notification.controller;

import com.staylog.staylog.domain.notification.dto.request.NotificationLimitRequest;
import com.staylog.staylog.domain.notification.dto.request.ReadAllRequest;
import com.staylog.staylog.domain.notification.dto.request.ReadRequest;
import com.staylog.staylog.domain.notification.dto.response.NotificationResponse;
import com.staylog.staylog.domain.notification.service.NotificationService;
import com.staylog.staylog.global.common.code.SuccessCode;
import com.staylog.staylog.global.common.response.SuccessResponse;
import com.staylog.staylog.global.common.util.MessageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "NotificationController", description = "알림 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("v1")
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final MessageUtil messageUtil;


    /**
     * 유저 한명의 알림 리스트 조회
     * @author 이준혁
     * @param userId 유저 PK
     * @param notificationLimitRequest 부가정보 (last 값, limit)
     * @return List<NotificationResponse>
     */
    @Operation(summary = "알림 목록 조회", description = "로그인한 사용자의 모든 알림 리스트를 조회합니다.")
    @GetMapping("/notification/{userId}")
    public ResponseEntity<SuccessResponse<List<NotificationResponse>>> getNotificationList(@PathVariable long userId, @ModelAttribute NotificationLimitRequest notificationLimitRequest) {

        // userId 담아서 전달
        notificationLimitRequest.setUserId(userId);

        List<NotificationResponse> notiList = notificationService.getNotificationList(notificationLimitRequest);
        log.info("목록 조회용 DetailsResponse 역직렬화 완료. userId: {}", userId);

        String message = messageUtil.getMessage(SuccessCode.NOTIFICATION_LIST_FIND.getMessageKey());
        String code = SuccessCode.NOTIFICATION_LIST_FIND.name();
        return ResponseEntity.ok(SuccessResponse.of(code, message, notiList));
    }

    
    /**
     * 알림 삭제
     * @author 이준혁
     * @param notiId 알림 PK
     */
    @Operation(summary = "알림 삭제", description = "알림을 삭제합니다.")
    @DeleteMapping("/notification/{notiId}/delete")
    public ResponseEntity<SuccessResponse<Void>> deleteNotification(@PathVariable long notiId) {
        notificationService.deleteNotification(notiId);

        String message = messageUtil.getMessage(SuccessCode.NOTIFICATION_DELETE.getMessageKey());
        String code = SuccessCode.NOTIFICATION_DELETE.name();
        return ResponseEntity.ok(SuccessResponse.of(code, message, null));
    }


    /**
     * 해당 유저의 알림 전체 삭제
     * @author 이준혁
     * @param userId 유저 PK
     */
    @Operation(summary = "특정 유저의 알림 전체 삭제", description = "알림을 일괄 삭제합니다.")
    @DeleteMapping("/notification/{userId}/delete-all")
    public ResponseEntity<SuccessResponse<Void>> deleteNotificationAll(@PathVariable long userId) {

        notificationService.deleteNotificationAll(userId);

        String message = messageUtil.getMessage(SuccessCode.NOTIFICATION_DELETE.getMessageKey());
        String code = SuccessCode.NOTIFICATION_DELETE.name();
        return ResponseEntity.ok(SuccessResponse.of(code, message, null));
    }


    /**
     * 단일 알림 읽음 처리
     * @author 이준혁
     * @param readRequest 알림 PK
     */
    @Operation(summary = "단일 알림 읽음 처리", description = "단일 알림의 읽음 상태를 Y로 변경합니다.")
    @PatchMapping("/notification/read-one")
    public ResponseEntity<SuccessResponse<Void>> readOne(@RequestBody ReadRequest readRequest) {
        notificationService.readOne(readRequest);

        String message = messageUtil.getMessage(SuccessCode.NOTIFICATION_READ.getMessageKey());
        String code = SuccessCode.NOTIFICATION_READ.name();
        return ResponseEntity.ok(SuccessResponse.of(code, message, null));
    }

    /**
     * 모든 알림 읽음 처리
     * @author 이준혁
     * @param readAllRequest 유저 PK
     */
    @Operation(summary = "모든 알림 읽음 처리", description = "모든 알림의 읽음 상태를 Y로 변경합니다.")
    @PatchMapping("/notification/read-all")
    public ResponseEntity<SuccessResponse<Void>> readAll(@RequestBody ReadAllRequest readAllRequest) {
        notificationService.readAll(readAllRequest);

        String message = messageUtil.getMessage(SuccessCode.NOTIFICATION_READ.getMessageKey());
        String code = SuccessCode.NOTIFICATION_READ.name();
        return ResponseEntity.ok(SuccessResponse.of(code, message, null));
    }




    /**
     * 안읽은 알림 수 조회
     * @author 이준혁
     * @param userId 사용자 PK
     * @return 안읽은 알림 수
     */
    @Operation(summary = "안읽은 알림 수 조회", description = "유저의 알림 중 읽지 않은 알림의 개수를 조회합니다. \n * 알림 아이콘에 출력할 용도")
    @GetMapping("/notification/{userId}/unread-count")
    public ResponseEntity<SuccessResponse<Integer>> unreadCount(@PathVariable long userId) {
        Integer unreadCount = notificationService.unreadCount(userId);

        String message = messageUtil.getMessage(SuccessCode.NOTIFICATION_UNREAD_COUNT.getMessageKey());
        String code = SuccessCode.NOTIFICATION_UNREAD_COUNT.name();
        return ResponseEntity.ok(SuccessResponse.of(code, message, unreadCount));
    }



}
