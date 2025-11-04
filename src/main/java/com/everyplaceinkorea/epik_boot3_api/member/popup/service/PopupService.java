package com.everyplaceinkorea.epik_boot3_api.member.popup.service;

import com.everyplaceinkorea.epik_boot3_api.member.concert.dto.ConcertResponseDto;
import com.everyplaceinkorea.epik_boot3_api.member.popup.dto.PopupResponseDto;

import java.util.List;

public interface PopupService {
    // 북마크
    List<PopupResponseDto> getBookmark(Long id);
    boolean isBookmarked(Long popupId, Long memberId);
    boolean toggleBookmark(Long popupId, Long memberId);
}
