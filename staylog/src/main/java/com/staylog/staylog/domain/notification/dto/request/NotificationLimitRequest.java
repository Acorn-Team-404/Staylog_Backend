package com.staylog.staylog.domain.notification.dto.request;

import lombok.*;
import org.apache.ibatis.type.Alias;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;


/**
 * 알림 목록 조회 시 Limit를 걸어 페이징할 용도의 DTO
 * author 이준혁
 */
@Alias("notificationLimitRequest")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationLimitRequest {
    private long userId;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime lastCreatedAt; // 마지막으로 받은 알림의 생성일
    private Long lastNotiId; // 마지막으로 받은 알림의 PK
    private Integer limit; // 응답할 데이터의 개수
}
