package com.staylog.staylog.domain.search.service.impl;


import com.staylog.staylog.domain.image.assembler.ImageAssembler;
import com.staylog.staylog.domain.search.dto.request.AccomListRequest;
import com.staylog.staylog.domain.search.dto.response.AccomListResponse;
import com.staylog.staylog.domain.search.mapper.SearchMapper;
import com.staylog.staylog.domain.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {

    private final SearchMapper searchMapper;
    private final ImageAssembler imageAssembler;

    /**
     *
     * - 1단계: 예약 가능한 숙소 필터링 + 숙소 정보 조회 (통합)
     * - 2단계: 인기순 정렬 시만 예약 수 조회
     * @Author danjae
     */
    @Override
    @Cacheable(
        value = "searchResults",
        key = "T(String).join('_', " +
              "#request.regionCodes != null ? #request.regionCodes : {}) + '_' + " +
              "(#request.checkIn != null ? #request.checkIn.toString() : 'null') + '_' + " +
              "(#request.checkOut != null ? #request.checkOut.toString() : 'null') + '_' + " +
              "(#request.people != null ? #request.people : 0) + '_' + " +
              "(#request.order != null ? #request.order : 'default') + '_' + " +
              "(#request.lastAccomId != null ? #request.lastAccomId : 0)"
    )
    public List<AccomListResponse> searchAccommodations(AccomListRequest request) {
        long startTime = System.currentTimeMillis();

        log.info("🔴 캐시 MISS - DB에서 검색 수행 (쿼리 최적화 버전)");

        log.info("검색 조건 - 인원: {}, 체크인: {}, 체크아웃: {}, 지역: {}, 정렬: {}",
                request.getPeople(), request.getCheckIn(), request.getCheckOut(),
                request.getRegionCodes(), request.getOrder());

        // ========== 1단계: 예약 가능 여부 체크 + 숙소 정보 조회 (통합) ==========
        long step1Start = System.currentTimeMillis();
        List<AccomListResponse> accommodationList = searchMapper.getAccomListOptimized(request);
        long step1Time = System.currentTimeMillis() - step1Start;

        log.info("[1단계 완료] 숙소 조회 (예약가능+기본정보 통합) - 개수: {}, 소요시간: {}ms",
                accommodationList.size(), step1Time);

        // 조회된 숙소가 없으면 빈 리스트 반환
        if (accommodationList.isEmpty()) {
            log.warn("조회된 숙소가 없습니다.");
            return Collections.emptyList();
        }

        // ========== 2단계: 예약 수 조회 (인기순 정렬 시만 실행) ==========
        boolean isPopularSort = "popular".equals(request.getOrder());
        long step2Time = 0;

        if (isPopularSort) {
            long step2Start = System.currentTimeMillis();

            //  조회된 숙소 ID 리스트
            List<Long> accommodationIds = accommodationList.stream()
                    .map(AccomListResponse::getAccommodationId)
                    .collect(Collectors.toList());

            // 예약 수 조회
            List<Map<String, Object>> reservationCounts = searchMapper.getReservationCounts(accommodationIds);

            log.debug("[2단계 디버그] 예약 수 조회 결과 개수: {}", reservationCounts.size());
            if (!reservationCounts.isEmpty()) {
                log.debug("[2단계 디버그] 첫 번째 Map 키: {}", reservationCounts.get(0).keySet());
            }

            // Map으로 변환 (accommodationId -> reservationCount)
            // Oracle은 alias를 대문자로 변환하므로 대문자 키 사용
            Map<Long, Integer> reservationCountMap = reservationCounts.stream()
                    .collect(Collectors.toMap(
                            map -> {
                                Object accomIdObj = map.get("ACCOMMODATIONID"); // Oracle 대문자 키
                                if (accomIdObj == null) {
                                    log.error("ACCOMMODATIONID is null in map: {}", map);
                                    throw new IllegalStateException("ACCOMMODATIONID not found in result map");
                                }
                                return ((Number) accomIdObj).longValue();
                            },
                            map -> {
                                Object countObj = map.get("RESERVATIONCOUNT"); // Oracle 대문자 키
                                if (countObj == null) {
                                    log.warn("RESERVATIONCOUNT is null for accommodation, defaulting to 0");
                                    return 0;
                                }
                                return ((Number) countObj).intValue();
                            }
                    ));

            // 예약 수를 각 숙소 객체에 설정
            accommodationList.forEach(accom -> {
                Integer resCount = reservationCountMap.getOrDefault(accom.getAccommodationId(), 0);
                accom.setReservationCount(resCount);
            });

            // 예약 수 기준으로 재정렬 (내림차순)
            accommodationList.sort((a, b) -> {
                int compareRes = Integer.compare(b.getReservationCount(), a.getReservationCount());
                if (compareRes != 0) return compareRes;
                return Long.compare(a.getAccommodationId(), b.getAccommodationId());
            });

            step2Time = System.currentTimeMillis() - step2Start;
            log.info("[2단계 완료] 예약 수 조회 및 재정렬 - 소요시간: {}ms", step2Time);
        } else {
            log.info("[2단계 스킵] 인기순 정렬이 아니므로 예약 수 조회 생략");
        }

        // ========== 결과 로그 출력 ==========
        long totalTime = System.currentTimeMillis() - startTime;
        log.info("========== 숙소 검색 완료  ==========");
        log.info("총 소요시간: {}ms (1단계: {}ms, 2단계: {}ms)",
                totalTime, step1Time, step2Time);
        log.info("최종 결과 개수: {}", accommodationList.size());

        // 개별 숙소 정보 로그
        for (AccomListResponse accom : accommodationList) {
            log.info("숙소 - ID: {}, 이름: {}, 지역: {}, 최대인원: {}, 최저가: {}, 예약수: {}",
                    accom.getAccommodationId(),
                    accom.getAccommodationName(),
                    accom.getRegionName(),
                    accom.getMaxCapacity(),
                    accom.getBasePrice(),
                    accom.getReservationCount());
        }
        imageAssembler.assembleMainImageUrl(accommodationList, AccomListResponse::getAccommodationId, AccomListResponse::setImageUrl, "ACCOMMODATION");

        return accommodationList;
    }
}
