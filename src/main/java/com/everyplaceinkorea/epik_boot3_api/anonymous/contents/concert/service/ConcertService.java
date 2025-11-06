package com.everyplaceinkorea.epik_boot3_api.anonymous.contents.concert.service;

import com.everyplaceinkorea.epik_boot3_api.anonymous.contents.concert.dto.ConcertResponseDto;

import java.util.List;

public interface ConcertService {
    List<ConcertResponseDto> getConcertsByRegion(Long regionId, Integer page);
    List<ConcertResponseDto> getConcertsByRandom();
    List<ConcertResponseDto> getConcertsByGenre(String genreName);
}

