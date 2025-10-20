package com.everyplaceinkorea.epik_boot3_api.member.concert.controller;

import com.everyplaceinkorea.epik_boot3_api.auth.entity.EpikUserDetails;
import com.everyplaceinkorea.epik_boot3_api.member.concert.dto.ConcertResponseDto;
import com.everyplaceinkorea.epik_boot3_api.member.concert.service.ConcertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("member/concert")
@RequiredArgsConstructor
public class ConcertController {

    private final ConcertService concertService;

    /**
     * 회원의 북마크 목록 조회
     */
    @GetMapping("{id}/bookmark")
    public ResponseEntity<List<ConcertResponseDto>> getBookmark(@PathVariable Long id) {
        List<ConcertResponseDto> responseDtos = concertService.getBookmark(id);
        return ResponseEntity.ok(responseDtos);
    }

    /**
     * 특정 콘서트 북마크 상태 조회
     */
    @GetMapping("{concertId}/bookmark/status")
    public ResponseEntity<Map<String, Object>> getBookmarkStatus(
            @PathVariable Long concertId,
            @AuthenticationPrincipal EpikUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.ok(Map.of(
                "isBookmarked", false,
                "authenticated", false
            ));
        }

        Long memberId = userDetails.getId();
        boolean isBookmarked = concertService.isBookmarked(concertId, memberId);

        return ResponseEntity.ok(Map.of(
            "isBookmarked", isBookmarked,
            "authenticated", true
        ));
    }

    /**
     * 북마크 추가/삭제 토글
     */
    @PostMapping("{concertId}/bookmark/toggle")
    public ResponseEntity<Map<String, Object>> toggleBookmark(
            @PathVariable Long concertId,
            @AuthenticationPrincipal EpikUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "success", false,
                "message", "로그인이 필요한 기능입니다."
            ));
        }

        try {
            Long memberId = userDetails.getId();
            boolean isBookmarked = concertService.toggleBookmark(concertId, memberId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "isBookmarked", isBookmarked,
                "message", isBookmarked ? "북마크가 추가되었습니다." : "북마크가 삭제되었습니다."
            ));
        } catch (Exception e) {
            log.error("북마크 토글 중 오류 발생: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "북마크 처리 중 오류가 발생했습니다."
            ));
        }
    }
}
