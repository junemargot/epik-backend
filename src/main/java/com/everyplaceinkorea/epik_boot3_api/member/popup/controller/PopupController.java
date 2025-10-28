package com.everyplaceinkorea.epik_boot3_api.member.popup.controller;

import com.everyplaceinkorea.epik_boot3_api.auth.entity.EpikUserDetails;
import com.everyplaceinkorea.epik_boot3_api.member.popup.dto.PopupResponseDto;
import com.everyplaceinkorea.epik_boot3_api.member.popup.service.PopupService;
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
@RequestMapping("member/popup")
@RequiredArgsConstructor
public class PopupController {

    private final PopupService popupService;

    // 북마크 조회
    // 북마크
    @GetMapping("{id}/bookmark")
    public ResponseEntity<List<PopupResponseDto>> getBookmark(@PathVariable Long id) {

        List<PopupResponseDto> responseDtos = popupService.getBookmark(id);
        return ResponseEntity.ok(responseDtos);
    }

    @GetMapping("{popupId}/bookmark/status")
    public ResponseEntity<Map<String, Object>> getBookmarkStatus(
            @PathVariable Long popupId,
            @AuthenticationPrincipal EpikUserDetails userDetails) {

        if(userDetails == null) {
            return ResponseEntity.ok(Map.of(
                    "isBookmarked", false,
                    "authenticated", false
            ));
        }

        Long memberId = userDetails.getId();
        boolean isBookmarked = popupService.isBookmarked(popupId, memberId);

        return ResponseEntity.ok(Map.of(
                "isBookmarked", isBookmarked,
                "authenticated", true
        ));
    }

    @PostMapping("{popupId}/bookmark/toggle")
    public ResponseEntity<Map<String, Object>> toggleBookmark(
            @PathVariable Long popupId,
            @AuthenticationPrincipal EpikUserDetails userDetails) {

        if(userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "로그인이 필요한 기능입니다."
            ));
        }

        try {
            Long memberId = userDetails.getId();
            boolean isBookmarked = popupService.toggleBookmark(popupId, memberId);
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
