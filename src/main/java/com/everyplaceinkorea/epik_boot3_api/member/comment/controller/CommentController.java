package com.everyplaceinkorea.epik_boot3_api.member.comment.controller;
/*
* 회원 댓글 요청 api
* */

import com.everyplaceinkorea.epik_boot3_api.member.comment.dto.CommentCreateDto;
import com.everyplaceinkorea.epik_boot3_api.member.comment.dto.CommentResponseDto;
import com.everyplaceinkorea.epik_boot3_api.member.comment.dto.CommentUpdateDto;
import com.everyplaceinkorea.epik_boot3_api.member.comment.service.CommentService;
import com.everyplaceinkorea.epik_boot3_api.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("feed")
@RequiredArgsConstructor
@Slf4j
public class CommentController {

    private final CommentService commentService;

    // 댓글 작성
    @PostMapping("{feedId}/comment")
    public ResponseEntity<Long> create(@PathVariable Long feedId,
                                    @RequestBody CommentCreateDto commentCreateDto) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        log.info("피드 댓글 작성 - feedId: {}, memberId: {}, content: {}",
                feedId, memberId, commentCreateDto.getContent());

        return ResponseEntity.status(HttpStatus.OK)
                .body(commentService.create(feedId, memberId, commentCreateDto));
    }

    // 댓글 조회
    @GetMapping("{feedId}/comment")
    public ResponseEntity<List<CommentResponseDto>> getList(@PathVariable Long feedId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(commentService.getList(feedId));
    }

    // 댓글 수정
    @PatchMapping("{feedId}/comment/{commentId}")
    public ResponseEntity<Void> update(
            @PathVariable Long feedId,
            @PathVariable Long commentId,
            @RequestBody CommentUpdateDto updateDto)
    {
        commentService.update(commentId, updateDto);
        return ResponseEntity.noContent().build();
    }

    // 댓글 삭제
    @DeleteMapping("{feedId}/comment/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long feedId,
            @PathVariable Long commentId)
    {
        commentService.delete(commentId);
        return ResponseEntity.noContent().build();
    }
}
