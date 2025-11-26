package com.staylog.staylog.domain.search.mapper;

import com.staylog.staylog.domain.search.dto.request.AccomListRequest;
import com.staylog.staylog.domain.search.dto.response.AccomListResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SearchMapper {

    /**
     * 예약 가능한 숙소 ID 리스트 추출
     * 검색 조건 (people, checkIn, checkOut)
     * 예약 가능한 숙소 ID 리스트
     */
    List<Long> getAvailableAccomIds(AccomListRequest request);

    /**
     *숙소 기본 정보 + 객실 집계 조회 (예약 수 제외)
     * availableAccomIds 추출된 예약 가능 숙소 ID 리스트
     * 숙소 리스트 예약 수는 0으로 설정되고 인기순 2차정렬 선택시 다음 쿼리에서 예약 수 집계로 분리
     */
    List<AccomListResponse> getAccomListBasic(@Param("request") AccomListRequest request,
                                                @Param("availableAccomIds") List<Long> availableAccomIds);

    /**
     * 예약 수만 조회 (popular 인기순 정렬 시만 호출)
     * @accommodationIds getAccomListBasic에서 조회된 숙소 ID 리스트
    
     */
    List<Map<String, Object>> getReservationCounts(@Param("accommodationIds") List<Long> accommodationIds);


}
