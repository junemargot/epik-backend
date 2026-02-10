package com.everyplaceinkorea.epik_boot3_api.member.inquiry.controller;

import com.everyplaceinkorea.epik_boot3_api.member.inquiry.dto.InquiryCreateRequestDto;
import com.everyplaceinkorea.epik_boot3_api.member.inquiry.dto.InquiryDetailResponseDto;
import com.everyplaceinkorea.epik_boot3_api.member.inquiry.dto.InquiryListResponseDto;
import com.everyplaceinkorea.epik_boot3_api.member.inquiry.dto.InquiryUpdateRequestDto;
import com.everyplaceinkorea.epik_boot3_api.member.inquiry.service.InquiryService;
import com.everyplaceinkorea.epik_boot3_api.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("member/inquiry")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    /**
     * 1:1 문의 목록 조회 (페이징)
     * GET /member/inquiry?page=0&size=10
     */
    @GetMapping
    public ResponseEntity<Page<InquiryListResponseDto>> getMyInquiryList(
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = DEFAULT_SORT_FIELD, direction = Sort.Direction.DESC) Pageable pageable) {

        Long memberId = SecurityUtil.getCurrentMemberId();
        Page<InquiryListResponseDto> inquiries = inquiryService.getMyInquiries(memberId, pageable);

        return ResponseEntity.ok(inquiries);
    }

    /**
     * 1:1 문의 상세 조회 (아코디언 열기)
     * GET /member/inquiry/{inquiryId}
     */
    @GetMapping("/{inquiryId}")
    public ResponseEntity<InquiryDetailResponseDto> getInquiryDetail(@PathVariable Long inquiryId) {

        Long memberId = SecurityUtil.getCurrentMemberId();
        InquiryDetailResponseDto response = inquiryService.getInquiryDetail(memberId, inquiryId);

        return ResponseEntity.ok(response);
    }

    /**
     * 1:1 문의 등록
     * POST /member/inquiry
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Long> createInquiry(
            @Valid @RequestPart("request")InquiryCreateRequestDto request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        Long memberId = SecurityUtil.getCurrentMemberId();
        Long inquiryId = inquiryService.createInquiry(request, images, memberId);

        log.info("문의 등록 성공 - inquiryId: {}, memberId: {}", inquiryId, memberId);

        return ResponseEntity.ok(inquiryId);
    }

    /**
     * 1:1 문의 수정
     * PUT /member/inquiry/{inquiryId}
     */
    @PutMapping(value = "/{inquiryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateInquiry(
            @PathVariable Long inquiryId,
            @Valid @RequestPart("request") InquiryUpdateRequestDto request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        Long memberId = SecurityUtil.getCurrentMemberId();
        inquiryService.updateInquiry(inquiryId, request, images, memberId);

        log.info("문의 수정 성공 - inquiryId: {}, memberId: {}", inquiryId, memberId);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{inquiryId}")
    public ResponseEntity<Void> deleteInquiry(@PathVariable Long inquiryId) {

        Long memberId = SecurityUtil.getCurrentMemberId();
        inquiryService.deleteInquiry(memberId, inquiryId);

        log.info("문의 삭제 - inquiryId: {}, memberId: {}", inquiryId, memberId);

        return ResponseEntity.ok().build();
    }
}
