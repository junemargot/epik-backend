package com.everyplaceinkorea.epik_boot3_api.anonymous.contents.concert.controller;

import com.everyplaceinkorea.epik_boot3_api.anonymous.contents.concert.dto.ConcertResponseDto;
import com.everyplaceinkorea.epik_boot3_api.anonymous.contents.concert.service.ConcertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("concert")
@RequiredArgsConstructor
public class ConcertController {

    private final ConcertService concertService;

    // 지역 필터링 조회
    // api/v1/concert?region=1&page=1
    @GetMapping()
    public ResponseEntity<List<ConcertResponseDto>> getConcertsByRegion(@RequestParam(name = "region", required = false) Long regionId,
                                                                        @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.status(OK)
                .body(concertService.getConcertsByRegion(regionId, page));
    }

    // 랜덤 이미지 조회
    @GetMapping("random")
    public ResponseEntity<List<ConcertResponseDto>> getConcertsByRandom() {
        return ResponseEntity.status(OK)
                .body(concertService.getConcertsByRandom());
    }

    // 장르별 조회
    @GetMapping("/genre")
    public ResponseEntity<List<ConcertResponseDto>> getConcertsByGenre(
            @RequestParam(name = "genreName", required = false) String genreName) {
        return ResponseEntity.status(OK).body(concertService.getConcertsByGenre(genreName));
    }
}
