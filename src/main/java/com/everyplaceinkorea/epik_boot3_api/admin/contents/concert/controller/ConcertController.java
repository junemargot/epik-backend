package com.everyplaceinkorea.epik_boot3_api.admin.contents.concert.controller;

import com.everyplaceinkorea.epik_boot3_api.admin.contents.concert.dto.ConcertListDto;
import com.everyplaceinkorea.epik_boot3_api.admin.contents.concert.dto.ConcertRequestDto;
import com.everyplaceinkorea.epik_boot3_api.admin.contents.concert.dto.ConcertResponseDto;
import com.everyplaceinkorea.epik_boot3_api.admin.contents.concert.service.ConcertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("admin/concert")
@RequiredArgsConstructor
public class ConcertController {

  private final ConcertService concertService;

  @GetMapping
  public ResponseEntity<ConcertListDto> getAllConcerts(
          @RequestParam(name = "p", defaultValue = "1") Integer page,
          @RequestParam(name = "k", required = false) String keyword,
          @RequestParam(name = "s", required = false) String searchType) {

    // ConcertService에서 검색 및 페이징 데이터를 가져옴
    ConcertListDto concerts = concertService.getList(page, keyword, searchType);
    System.out.println("콘서트 목록: " + concerts);
    System.out.println("검색 파라미터 - searchType: " + searchType + ", keyword: " + keyword);
    // 가져온 데이터를 ResponseEntity로 반환
    return ResponseEntity.ok().body(concerts);
  }

  @GetMapping("/{id}")
  public ResponseEntity<ConcertResponseDto> getConcertById(@PathVariable Long id) {

    ConcertResponseDto concert = concertService.getById(id);
    return ResponseEntity.ok(concert);
  }

  @PostMapping
  public ResponseEntity<ConcertResponseDto> createConcert(@ModelAttribute ConcertRequestDto concertRequestDto,
                                                          MultipartFile files) throws IOException {
    log.info("Concert request: {}", concertRequestDto.toString());
    return ResponseEntity.status(HttpStatus.OK)
            .body(concertService.create(concertRequestDto, files));
  }

  @DeleteMapping("{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {

    concertService.delete(id);

    return ResponseEntity.noContent().build();
  }
}
