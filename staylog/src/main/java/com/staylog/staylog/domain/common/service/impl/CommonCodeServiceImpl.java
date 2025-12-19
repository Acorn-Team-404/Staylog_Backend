package com.staylog.staylog.domain.common.service.impl;

import com.staylog.staylog.domain.common.dto.CommonCodeDto;
import com.staylog.staylog.domain.common.dto.response.CommonCodeGroupResponse;
import com.staylog.staylog.domain.common.mapper.CommonCodeMapper;
import com.staylog.staylog.domain.common.service.CommonCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommonCodeServiceImpl implements CommonCodeService {

    private final CommonCodeMapper commonCodeMapper;

    @Override
    public List<CommonCodeDto> getCodesByParentId(String parentId) {
        log.info("공통코드 조회 - parentId: {}", parentId);
        return commonCodeMapper.getCodesByParentId(parentId);
    }

    @Override
    @Cacheable(value = "commonCodes", key = "'all'")
    public CommonCodeGroupResponse getAllCommonCodesGrouped() {
        log.info(" 캐시 MISS - DB에서 공통코드 조회");
        log.info("모든 공통코드 그룹별 조회 시작");

        // 모든 공통코드 조회
        List<CommonCodeDto> allCodes = commonCodeMapper.getAllCommonCodes();

        // 부모 ID별로 그룹화
        Map<String, List<CommonCodeDto>> groupedCodes = allCodes.stream()
                .collect(Collectors.groupingBy(CommonCodeDto::getParentId));

        // 응답 객체 생성
        CommonCodeGroupResponse response = CommonCodeGroupResponse.builder()
                .regions(groupedCodes.getOrDefault("REGION_TYPE", List.of()))
                .accommodationTypes(groupedCodes.getOrDefault("ACCOMMODATION_TYPE", List.of()))
                .roomTypes(groupedCodes.getOrDefault("ROOM_TYPE", List.of()))
                .roomStatus(groupedCodes.getOrDefault("ROOM_STATUS", List.of()))
                .reservationStatus(groupedCodes.getOrDefault("RESERVATION_STATUS", List.of()))
                .paymentStatus(groupedCodes.getOrDefault("PAYMENT_STATUS", List.of()))
                .paymentMethods(groupedCodes.getOrDefault("PAYMENT_METHOD", List.of()))
                .refundStatus(groupedCodes.getOrDefault("REFUND_STATUS", List.of()))
                .refundTypes(groupedCodes.getOrDefault("REFUND_TYPE", List.of()))
                .notificationTypes(groupedCodes.getOrDefault("NOTIFICATION_TYPE", List.of()))
                .boardTypes(groupedCodes.getOrDefault("BOARD_TYPE", List.of()))
                .imageTypes(groupedCodes.getOrDefault("IMAGE_TYPE", List.of()))
                .userRoles(groupedCodes.getOrDefault("USER_ROLE", List.of()))
                .userStatus(groupedCodes.getOrDefault("USER_STATUS", List.of()))
                .order(groupedCodes.getOrDefault("SORT_ORDER",List.of()))
                .build();

        log.info("공통코드 그룹별 조회 완료 - 지역: {}개, 숙소타입: {}개, 객실타입: {}개",
                response.getRegions().size(),
                response.getAccommodationTypes().size(),
                response.getRoomTypes().size());

        return response;
    }
}
