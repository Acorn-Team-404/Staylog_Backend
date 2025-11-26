package com.staylog.staylog.domain.board.service;


import com.staylog.staylog.domain.board.dto.LikesDto;
import com.staylog.staylog.domain.board.mapper.BoardMapper;
import com.staylog.staylog.domain.board.mapper.LikesMapper;
import com.staylog.staylog.global.common.code.ErrorCode;
import com.staylog.staylog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikesServiceImpl implements LikesService {

    private final LikesMapper likesMapper;
    private final BoardMapper boardMapper;

//    @Override
//    public List<LikesDto> getByBoardId(long boardId) {
//
//        return likesMapper.getByBoardId(boardId);
//    }

    @Transactional
    public void toggleLike(LikesDto likesDto) {

        // likesDto 유효성검사
        if (likesDto == null || likesDto.getBoardId() <= 0 || likesDto.getUserId() <= 0) {
            log.warn("유효하지 않은 좋아요 요청 DTO: {}", likesDto);
            throw new BusinessException(ErrorCode.BOARD_INVALID_INPUT);
        }
        // 현재 좋아요 상태 확인
        boolean liked = likesMapper.liked(likesDto);

        if (!liked) {
            // 좋아요 추가
            likesMapper.addLike(likesDto);
            boardMapper.increaseLikeCount(likesDto.getBoardId());
            // 좋아요 로깅
            log.info("좋아요 추가. BoardId: {}, UserId: {}", likesDto.getBoardId(), likesDto.getUserId());
        } else {
            // 좋아요 삭제 (deleteLike 반환값 int)
            int deletedRows = likesMapper.deleteLike(likesDto);
            // 삭제된 행이 있으면
            if (deletedRows > 0) {
                boardMapper.decreaseLikeCount(likesDto.getBoardId());
                log.info("좋아요 삭제. BoardId: {}, UserId: {}", likesDto.getBoardId(), likesDto.getUserId());
            } else {
                // 이미 삭제되었거나 기록이 없는 경우
                log.warn("삭제 요청했으나 좋아요 기록을 찾을 수 없음. BoardId: {}, UserId: {}", likesDto.getBoardId(), likesDto.getUserId());
                // 여기서 예외를 던지는 대신, 멱등성을 위해 경고만 남기고 정상 종료하는 것이 일반적입니다.
            }


        }
    }


    @Override
    public int countByBoardId(long boardId) {

        // boardId 유효성 검사
        if (boardId <= 0) {
            throw new BusinessException(ErrorCode.BOARD_INVALID_INPUT);
        }

        return likesMapper.countByBoardId(boardId);
    }

    @Override
    public boolean liked(LikesDto likesDto) {

        // likesDto 유효성검사
        if (likesDto == null || likesDto.getBoardId() <= 0 || likesDto.getUserId() <= 0) {
            throw new BusinessException(ErrorCode.BOARD_INVALID_INPUT);
        }

        return likesMapper.liked(likesDto);
    }
}
