package com.everyplaceinkorea.epik_boot3_api.member.musical.service;

import com.everyplaceinkorea.epik_boot3_api.member.musical.dto.MusicalResponseDto;

import java.util.List;

public interface MusicalService {
    // 북마크
    List<MusicalResponseDto> getBookmark(Long id);
    boolean isBookmarked(Long musicalId, Long memberId);
    boolean toggleBookmark(Long musicalId, Long memberId);
}
