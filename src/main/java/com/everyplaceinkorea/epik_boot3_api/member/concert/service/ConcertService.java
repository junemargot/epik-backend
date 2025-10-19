package com.everyplaceinkorea.epik_boot3_api.member.concert.service;

import com.everyplaceinkorea.epik_boot3_api.member.concert.dto.ConcertResponseDto;

import java.util.List;

public interface ConcertService {
    // 북마크
    List<ConcertResponseDto> getBookmark(Long id);

    boolean isBookmarked(Long concertId, Long memberId);
    boolean toggleBookmark(Long concertId, Long memberId);
}
