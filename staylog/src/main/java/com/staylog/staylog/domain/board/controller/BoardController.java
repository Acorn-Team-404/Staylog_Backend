package com.staylog.staylog.domain.board.controller;

import com.staylog.staylog.domain.board.dto.BoardDto;
import com.staylog.staylog.domain.board.dto.BookingDto;
import com.staylog.staylog.domain.board.dto.ViewDto;
import com.staylog.staylog.domain.board.dto.request.BoardListRequest;
import com.staylog.staylog.domain.board.dto.response.BoardListResponse;

import com.staylog.staylog.domain.board.service.BoardService;
import com.staylog.staylog.domain.board.service.ViewsService;
import com.staylog.staylog.global.common.code.SuccessCode;
import com.staylog.staylog.global.common.dto.PageRequest;
import com.staylog.staylog.global.common.response.SuccessResponse;
import com.staylog.staylog.global.common.util.MessageUtil;
import com.staylog.staylog.global.exception.BusinessException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 게시판 컨트롤러
 * @author 김지은
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
public class BoardController {

    private final BoardService boardService;
    private final ViewsService viewsService;
    private final MessageUtil messageUtil;

    // 게시판 카테고리별 목록 조회
    @PostMapping("/boardList")
    public ResponseEntity<SuccessResponse<BoardListResponse>> boardList(@RequestBody BoardListRequest boardListRequest) {

        BoardListResponse response = boardService.getByBoardType(boardListRequest);
        String message = messageUtil.getMessage(SuccessCode.BOARD_LIST_FETCHED.getMessageKey());
        String code = SuccessCode.BOARD_LIST_FETCHED.name();

        SuccessResponse<BoardListResponse> success = SuccessResponse.of(code, message, response);

        return ResponseEntity.ok(success);

    }

    // 게시판 등록
    @PostMapping("/boards")
    public ResponseEntity<SuccessResponse<BoardDto>> boardCreate(@RequestBody BoardDto boardDto) {

        BoardDto newBoard = boardService.insert(boardDto);

        String code = SuccessCode.BOARD_CREATED.name();
        String message = messageUtil.getMessage(SuccessCode.BOARD_CREATED.getMessageKey());

        SuccessResponse<BoardDto> success = SuccessResponse.of(code, message, newBoard);

        return ResponseEntity.ok(success);

    }


    // 게시판 수정
    @PutMapping("/boards/{boardId}")
    public ResponseEntity<SuccessResponse<BoardDto>> boardUpdate(@RequestBody BoardDto boardDto) {

        boardService.update(boardDto);

        String code = SuccessCode.BOARD_UPDATED.name();
        String message = messageUtil.getMessage(SuccessCode.BOARD_UPDATED.getMessageKey());

        SuccessResponse<BoardDto> success = SuccessResponse.of(code, message, boardDto);

        return ResponseEntity.ok(success);
    }

    // 게시판 삭제
    @DeleteMapping("/boards/{boardId}")
    public ResponseEntity<SuccessResponse<Void>> boardDelete(long boardId) {

        boardService.delete(boardId);

        String code = SuccessCode.BOARD_DELETED.name();
        String message = messageUtil.getMessage(SuccessCode.BOARD_DELETED.getMessageKey());

        SuccessResponse<Void> success = SuccessResponse.of(code, message, null);

        return ResponseEntity.ok(success);
    }

    // 예약내역 불러오기
    @GetMapping("/boards/bookings/{userId}")
    public ResponseEntity<SuccessResponse<List<BookingDto>>> bookingList(@PathVariable Long userId) {

        List<BookingDto> bookings = boardService.bookingList(userId);

        String code = SuccessCode.BOARD_LIST_FETCHED.name();
        String message = messageUtil.getMessage(SuccessCode.BOARD_LIST_FETCHED.getMessageKey());

        SuccessResponse<List<BookingDto>> success = SuccessResponse.of(code, message, bookings);
        return ResponseEntity.ok(success);

    }

    // 게시글 상세정보 불러오기
    @GetMapping("/boards/{boardId}")
    public ResponseEntity<SuccessResponse<BoardDto>> getByBoardId(
            @PathVariable Long boardId,
            @RequestParam(value = "userId", required = false) Long userId,
            HttpServletRequest request,
            HttpServletResponse response) {

        // 조회 기록 처리
        String viewToken;

        // 로그인 사용자
        if(userId != null) {
            viewToken = "USER_" + userId;
        }
        // 비로그인 사용자
        else {
            Cookie[] cookies = request.getCookies();
            Cookie tokenCookie = Arrays.stream(Optional.ofNullable(cookies).orElse(new Cookie[0]))
                    .filter(c -> "view_token".equals(c.getName()))
                    .findFirst()
                    .orElse(null);

            if(tokenCookie == null){
                //랜덤 UUID 쿠키 생성 (비로그인 사용자용)
                viewToken = UUID.randomUUID().toString();
                Cookie newCookie = new Cookie("view_token", viewToken);
                newCookie.setMaxAge(60*60); // 1시간 유지
                newCookie.setPath("/");
                response.addCookie(newCookie);
            } else {
                viewToken = tokenCookie.getValue();
            }
        }

        // 조회수 처리 (1시간 내 중복 방지)
        viewsService.insertView(new ViewDto());

        BoardDto board = boardService.getByBoardId(boardId);

        String code = SuccessCode.BOARD_DETAIL_FETCHED.name();
        String message = messageUtil.getMessage(SuccessCode.BOARD_DETAIL_FETCHED.getMessageKey());

        SuccessResponse<BoardDto> success = SuccessResponse.of(code, message, board);
        return ResponseEntity.ok(success);
    }


}