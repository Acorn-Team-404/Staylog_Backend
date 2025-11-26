package com.staylog.staylog.domain.board.controller;

import com.staylog.staylog.domain.board.dto.LikesDto;
import com.staylog.staylog.domain.board.service.LikesService;
import com.staylog.staylog.global.common.code.SuccessCode;
import com.staylog.staylog.global.common.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.staylog.staylog.global.common.util.MessageUtil;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class LikesController {

    private final LikesService likesService;
    private final MessageUtil messageUtil;

    // 게시글별 좋아요 수
    @GetMapping(path = "/likes/{boardId}", params = "!userId")
    public ResponseEntity<SuccessResponse<Integer>> countByBoardId(@PathVariable long boardId) {

        int likesCount = likesService.countByBoardId(boardId);

        String code = SuccessCode.BOARD_DETAIL_FETCHED.name();
        String message = messageUtil.getMessage(SuccessCode.BOARD_DETAIL_FETCHED.getMessageKey());

        SuccessResponse<Integer> success = SuccessResponse.of(code, message, likesCount);

        return ResponseEntity.ok(success);
    }

    // 사용자의 좋아요 여부
    @GetMapping(path = "/likes/{boardId}", params = "userId")
    public ResponseEntity<SuccessResponse<Boolean>> liked(@PathVariable long boardId, @PathVariable long userId) {

        // @PathVariable 로 받은 두개로 LikesDto 생성
        LikesDto likesDto = LikesDto.builder()
                .boardId(boardId)
                .userId(userId)
                .build();

        // likeService 호출하여 좋아요 여부 확인
        boolean liked = likesService.liked(likesDto);

        String code = SuccessCode.BOARD_DETAIL_FETCHED.name();
        String message = messageUtil.getMessage(SuccessCode.BOARD_LIKES_FETCHED.getMessageKey());
        SuccessResponse<Boolean> success = SuccessResponse.of(code, message, liked);

        return ResponseEntity.ok(success);
    }



    // 좋아요 등록/삭제 토글
    @PostMapping("/likes")
    public ResponseEntity<SuccessResponse<Void>> toggleLike(@RequestBody LikesDto likesDto) {

        // likesService 호출하여 좋아요 등록/삭제 토글
        likesService.toggleLike(likesDto);

        String code = SuccessCode.BOARD_CREATED.name();
        String message = messageUtil.getMessage(SuccessCode.BOARD_CREATED.getMessageKey());
        SuccessResponse<Void> success = SuccessResponse.of(code, message, null);

        return ResponseEntity.ok(success);

    }




}
