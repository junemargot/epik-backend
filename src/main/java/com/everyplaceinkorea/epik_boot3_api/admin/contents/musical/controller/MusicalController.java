package com.everyplaceinkorea.epik_boot3_api.admin.contents.musical.controller;

import com.everyplaceinkorea.epik_boot3_api.admin.contents.musical.dto.MusicalListDto;
import com.everyplaceinkorea.epik_boot3_api.admin.contents.musical.dto.MusicalResponseDto;
import com.everyplaceinkorea.epik_boot3_api.admin.contents.musical.dto.MusicalUpdateDto;
import com.everyplaceinkorea.epik_boot3_api.admin.contents.musical.service.MusicalService;
import com.everyplaceinkorea.epik_boot3_api.admin.contents.musical.dto.MusicalCreateDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/admin/musical")
@RequiredArgsConstructor
@Slf4j
public class MusicalController {

    private final MusicalService musicalService;

    @PostMapping
    public ResponseEntity<MusicalResponseDto> create (
            MusicalCreateDto requestDto,
            MultipartFile files) throws IOException {

        log.info("requestDto = {} ", requestDto);
        return ResponseEntity.status(HttpStatus.OK)
                .body(musicalService.create(requestDto, files));
    }

    // 뮤지컬 전체 조회 및 검색
    @GetMapping
//    @GetMapping(value = "/admin/musical", params = "!status")
    public ResponseEntity<MusicalListDto> getList(
            @RequestParam(name = "p", defaultValue = "1") int page, // 페이지 번호
            @RequestParam(name = "k", required = false) String keyword, // 검색어
            @RequestParam(name = "s", required = false) String searchType) { // 검색 필터링
        return ResponseEntity.status(HttpStatus.OK)
                .body(musicalService.getList(page, keyword, searchType));
    }

    // 뮤지컬 전체 조회 및 검색
//    @GetMapping(value = "/admin/musical", params = "status")
//    public ResponseEntity<MusicalListDto> getFilteredList(
//            @RequestParam(name = "p", defaultValue = "1") int page, // 페이지 번호
//            @RequestParam(name = "k", required = false) String keyword, // 검색어
//            @RequestParam(name = "s", required = false) String searchType, // 검색 필터링
//            @RequestParam(name = "status", defaultValue = "ACTIVE") String status) throws IOException { // 상태 필터링
//
//        return ResponseEntity.status(HttpStatus.OK)
//                .body(musicalService.getFilteredList(page, keyword, searchType, status));
//
//    }

    // 뮤지컬 게시물 상세 조회
    @GetMapping("{id}")
    public ResponseEntity<MusicalResponseDto> getMusical(@PathVariable Long id) {
        log.info("상세조회 요청 호출완료");
        return ResponseEntity.status(HttpStatus.OK)
                .body(musicalService.getMusical(id));
    }

    // 뮤지컬 게시물 수정
    // api/v1/admin/musical/{id}
    @PatchMapping("{id}")
    public ResponseEntity<Long> update(
            @PathVariable Long id,
            @RequestPart(name = "request") MusicalUpdateDto musicalUpdateDto,
            @RequestPart MultipartFile file) {

        musicalService.update(id, musicalUpdateDto, file);
        return ResponseEntity.noContent().build();
    }

    // 뮤지컬 게시물 삭제
    // api/v1/admin/musical/{id}
    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete (@PathVariable Long id) {
        musicalService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // 뮤지컬 비공개/공개 상태 변경
    // api/v1/admin/musical/{id}/status
    @PatchMapping("{id}/status")
    public ResponseEntity<Void> changeMusicalStatus(@PathVariable Long id) {
        musicalService.updateMusicalStatus(id);
        return ResponseEntity.noContent().build();
    }




}
