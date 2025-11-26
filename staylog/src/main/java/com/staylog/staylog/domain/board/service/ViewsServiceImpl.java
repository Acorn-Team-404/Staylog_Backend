package com.staylog.staylog.domain.board.service;

import com.staylog.staylog.domain.board.dto.ViewDto;
import com.staylog.staylog.domain.board.mapper.BoardMapper;
import com.staylog.staylog.domain.board.mapper.ViewsMapper;
import com.staylog.staylog.global.common.code.ErrorCode;
import com.staylog.staylog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViewsServiceImpl implements ViewsService {

    private final ViewsMapper viewsMapper;
    private final BoardMapper boardMapper;

    @Override
    public int CountViewsByBoardId(Long boardId) {
        return viewsMapper.countByBoardId(boardId);
    }

    @Override
    @Transactional
    public void insertView(ViewDto viewDto) {

        // viewDto 유효성 검증
        if (viewDto == null || viewDto.getBoardId() == null || viewDto.getBoardId() <= 0) {
            log.warn("유효하지 않은 ViewDto 또는 BoardId 요청.");
            throw new BusinessException(ErrorCode.BOARD_INVALID_INPUT);
        }

        // 최근 1시간 내 조회기록 확인
        boolean alreadyViewed = viewsMapper.hasRecentView(viewDto);

        if (!alreadyViewed) {
            // views 테이블에 insert
            viewsMapper.insertView(viewDto);

            // board 테이블에 조회수 증가
            boardMapper.increaseViewsCount(viewDto.getBoardId());

            log.info("새로운 조회 기록 및 조회수 증가. BoardId: {}", viewDto.getBoardId());
        } else {
            log.info("최근 1시간 내 중복 조회 기록. BoardId: {}", viewDto.getBoardId());
        }

    }

}
