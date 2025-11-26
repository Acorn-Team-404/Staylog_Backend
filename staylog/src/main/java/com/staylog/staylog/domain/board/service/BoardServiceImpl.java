package com.staylog.staylog.domain.board.service;

import com.staylog.staylog.domain.board.dto.BoardDto;
import com.staylog.staylog.domain.board.dto.BookingDto;
import com.staylog.staylog.domain.board.dto.request.BoardListRequest;
import com.staylog.staylog.domain.board.dto.response.BoardListResponse;
import com.staylog.staylog.domain.board.mapper.BoardMapper;
import com.staylog.staylog.domain.image.assembler.ImageAssembler;
import com.staylog.staylog.global.common.code.ErrorCode;
import com.staylog.staylog.global.common.response.PageResponse;
import com.staylog.staylog.global.event.ReviewCreatedEvent;
import com.staylog.staylog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    private final BoardMapper boardMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ImageAssembler imageAssembler;


    @Override
    public BoardListResponse getByBoardType(BoardListRequest boardListRequest) {

        if (boardListRequest == null) {
            throw new BusinessException(ErrorCode.BOARD_NOT_FOUND);
        }

        log.info("게시글 목록 조회 시작. BoardType: {}, Page: {}, Size: {}",
                boardListRequest.getBoardType(), boardListRequest.getPageNum(), boardListRequest.getPageSize());

        // 전체 게시글 수
        int totalCount = boardMapper.countByBoardType(boardListRequest.getBoardType());

        // 페이지 계산 결과
        PageResponse pageResponse = new PageResponse();
        pageResponse.calculate(boardListRequest, totalCount);


        // 게시글 목록
        List<BoardDto> boardList = boardMapper.getByBoardType(boardListRequest);

        if (boardList.isEmpty()) {
            log.warn("조회된 게시글이 없습니다. BoardType: {}", boardListRequest.getBoardType());
            throw new BusinessException(ErrorCode.BOARD_NOT_FOUND);
        }

        imageAssembler.assembleMainImageUrl(boardList, BoardDto::getBoardId, BoardDto::setImageUrl, BoardDto::getBoardType);

        log.info("게시글 목록 조회 완료. BoardType: {}, 조회 건수: {}", boardListRequest.getBoardType(), boardList.size());


        // BoardListResponse로 묶어서 반환
        BoardListResponse boardListResponse = new BoardListResponse();
        boardListResponse.setBoardList(boardList);
        boardListResponse.setPageResponse(pageResponse);


        return boardListResponse;

    }

    // 게시글 상세보기
    @Override
    @Transactional
    public BoardDto getByBoardId(long boardId) {

        log.info("게시글 상세 조회 요청 시작. BoardId: {}", boardId);

        // boardId 유효성 검사
        if(boardId <= 0) {
            log.warn("유효하지 않은 BoardId: {}", boardId);
            throw new BusinessException(ErrorCode.BOARD_INVALID_INPUT);
        }

        // 게시글 조회
        BoardDto board = boardMapper.getByBoardId(boardId);

        // 게시글 존재 X - exception 처리
        if (board == null) {
            log.warn("요청된 게시글이 존재하지 않음. BoardId: {}", boardId);
            throw new BusinessException(ErrorCode.BOARD_NOT_FOUND);
        }

        // 조회수 업데이트 + 조회수 수동 증가
        // 조회수 증가는 비동기 별도 처리
        boardMapper.updateViewsCount(boardId);
        board.setViewsCount(board.getViewsCount() + 1);

        // 이미지 추가 조립이 필요하면 여기서 수행
        // imageAssembler.assembleDetail(board);

        log.info("게시글 상세 조회 완료. BoardId: {}", boardId);
        return board;
    }

    // 게시글 등록
    @Override
    @Transactional
    public BoardDto insert(BoardDto boardDto) {

        boardMapper.insert(boardDto);

        log.info("게시글 등록완료. BoardId: {}", boardDto.getBoardId());

        // =============== 리뷰 게시글 작성 이벤트 발행(알림 발송) ==================
        if(boardDto.getBoardType().equals("BOARD_REVIEW")) { // 리뷰 게시글만 알림 전송
            ReviewCreatedEvent event = new ReviewCreatedEvent(boardDto.getBoardId(), boardDto.getAccommodationId(), boardDto.getBookingId(), boardDto.getUserId());
            eventPublisher.publishEvent(event);
        }

        return boardDto;
    }

    @Override
    @Transactional
    public void update(BoardDto boardDto) {

        log.info("게시글 수정 요청. BoardId: {}", boardDto.getBoardId());

        // 수정된 행 개수
        int updatedRows = boardMapper.update(boardDto);

        // 수정된 행 개수 = 0 일 경우 exception 처리
        if (updatedRows == 0) {
            log.warn("수정 대상 게시글을 찾을 수 없습니다. BoardId: {}", boardDto.getBoardId());
            throw new BusinessException(ErrorCode.BOARD_NOT_FOUND);
        }

        log.info("게시글 수정 완료. BoardId: {}, 수정된 게시글 수 : {}", boardDto.getBoardId(), updatedRows);
    }

    @Override
    @Transactional
    public void delete(long boardId) {

        log.info("게시글 삭제 요청. BoardId: {}", boardId);

        // boardId 유효성 검사 - exception 처리
        if(boardId <= 0) {
            throw new BusinessException(ErrorCode.BOARD_INVALID_INPUT);
        }

        int deletedRows = boardMapper.delete(boardId);

        if (deletedRows == 0) {
            log.warn("삭제 대상 게시글을 찾을 수 없습니다. BoardId: {}", boardId);
            throw new BusinessException(ErrorCode.BOARD_NOT_FOUND);
        }

        log.info("게시글 삭제 완료. BoardId: {}", boardId);
    }

    @Override
    public List<BookingDto> bookingList(long userId) {

        log.info("사용자 예약 목록 조회 요청. UserId: {}", userId);

        if(userId <= 0) {
            throw new BusinessException(ErrorCode.BOARD_INVALID_INPUT);
        }

        List<BookingDto> bookingList = boardMapper.bookingList(userId);

        log.info("사용자 예약 목록 조회 완료. UserId: {}, 조회 건수: {}", userId, bookingList.size());
        return  bookingList;
    }




}
