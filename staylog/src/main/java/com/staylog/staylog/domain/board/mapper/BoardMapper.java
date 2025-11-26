package com.staylog.staylog.domain.board.mapper;

import com.staylog.staylog.domain.board.dto.BoardDto;
import com.staylog.staylog.domain.board.dto.BookingDto;
import com.staylog.staylog.domain.board.dto.request.BoardListRequest;
import com.staylog.staylog.domain.board.dto.request.BoardRequest;
import com.staylog.staylog.domain.board.dto.response.BoardListResponse;
import com.staylog.staylog.domain.booking.entity.AccommodationIdAndName;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BoardMapper {

    List<BoardDto> getByBoardType(BoardListRequest boardListRequest);
    BoardDto getByBoardId(long boardId);

    int insert(BoardDto boardDto);
    int update(BoardDto boardDto);
    int delete(long boardId);

    int countByBoardType(String boardType);
    int countBoardList(BoardDto boardDto);

    List<BookingDto> bookingList(Long userId);


    // 게시글 PK로 작성자 PK 조회(알림에서 사용할 목적)
    long getUserIdByBoardId(long boardId);


    void increaseLikeCount(Long boardId);
    void decreaseLikeCount(Long boardId);
    void updateLikeCount(Long boardId);

    void increaseViewsCount(Long boardId);
    void updateViewsCount(Long boardId);

    // 게시글 PK로 숙소명, PK 조회 (알림에서 사용)
    AccommodationIdAndName getAccommodationNameAndIdByBoardId(long boardId);
}








