package com.everyplaceinkorea.epik_boot3_api.member.musical.controller;

import com.everyplaceinkorea.epik_boot3_api.auth.entity.EpikUserDetails;
import com.everyplaceinkorea.epik_boot3_api.member.musical.dto.MusicalResponseDto;
import com.everyplaceinkorea.epik_boot3_api.member.musical.service.MusicalService;
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
@RequestMapping("member/musical")
@RequiredArgsConstructor
public class MusicalController {

    private final MusicalService musicalService;

    // 북마크 조회
    // 북마크
    @GetMapping("{id}/bookmark")
    public ResponseEntity<List<MusicalResponseDto>> getBookmark(@PathVariable Long id) {

        List<MusicalResponseDto> responseDtos = musicalService.getBookmark(id);
        return ResponseEntity.ok(responseDtos);
    }

    @GetMapping("{musicalId}/bookmark/status")
    public ResponseEntity<Map<String, Object>> getBookmarkStatus(
            @PathVariable Long musicalId,
            @AuthenticationPrincipal EpikUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.ok(Map.of(
                    "isBookmarked", false,
                    "authenticated", false
            ));
        }

        Long memberId = userDetails.getId();
        boolean isBookmarked = musicalService.isBookmarked(musicalId, memberId);

        return ResponseEntity.ok(Map.of(
                "isBookmarked", isBookmarked,
                "authenticated", true
        ));
    }

    @PostMapping("{musicalId}/bookmark/toggle")
    public ResponseEntity<Map<String, Object>> toggleBookmark(
            @PathVariable Long musicalId,
            @AuthenticationPrincipal EpikUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "로그인이 필요한 기능입니다."
            ));
        }

        try {
            Long memberId = userDetails.getId();
            boolean isBookmarked = musicalService.toggleBookmark(musicalId, memberId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "isBookmarked", isBookmarked,
                    "message", isBookmarked ? "북마크가 추가되었습니다." : "북마크가 삭제되었습니다."
            ));
        } catch (Exception e) {
            log.error("북마크 토글 중 오류 발생: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "북마크 처리 중 오류가 발생헀습니다."
            ));
        }
    }
}
