package com.staylog.staylog.domain.search.controller;

import com.staylog.staylog.domain.search.dto.request.AccomListRequest;
import com.staylog.staylog.domain.search.dto.response.AccomListResponse;
import com.staylog.staylog.domain.search.service.SearchService;
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

@Tag(name = "SearchController", description = "숙소 검색 API")
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/v1")
public class SearchController {

    private final SearchService searchService;
    private final MessageUtil messageUtil;

    /**
     * 검색 조건 (인원, 체크인/아웃, 지역, 정렬)
     *  검색된 숙소 리스트
     */
    @Operation(summary = "숙소 검색", description = "조건에 따른 예약 가능한 숙소 리스트 조회 ")
    @GetMapping("/search/accommodations")
    public ResponseEntity<SuccessResponse<List<AccomListResponse>>> searchAccommodations(
            @ModelAttribute AccomListRequest request) {

        List<AccomListResponse> accomListResponse = searchService.searchAccommodations(request);

        log.info("조회된 숙소 개수: {}", accomListResponse.size());

        String code = SuccessCode.SUCCESS.name();
        String message = messageUtil.getMessage(SuccessCode.SUCCESS.getMessageKey());
        SuccessResponse<List<AccomListResponse>> success = SuccessResponse.of(code, message, accomListResponse);

        return ResponseEntity.ok(success);
    }

}
