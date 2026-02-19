package com.everyplaceinkorea.epik_boot3_api.admin.inquiry.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.everyplaceinkorea.epik_boot3_api.admin.inquiry.dto.InquiryAnswerRequestDto;
import com.everyplaceinkorea.epik_boot3_api.admin.inquiry.dto.InquiryDetailResponseDto;
import com.everyplaceinkorea.epik_boot3_api.admin.inquiry.dto.InquiryListResponseDto;
import com.everyplaceinkorea.epik_boot3_api.admin.inquiry.service.InquiryService;
import com.everyplaceinkorea.epik_boot3_api.util.SecurityUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@Slf4j
@RestController
@RequestMapping("admin/inquiry")
@RequiredArgsConstructor
public class InquiryController {
  
  private final InquiryService inquiryService;

  @GetMapping
  public ResponseEntity<Page<InquiryListResponseDto>> getInquiryList(@PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

    Page<InquiryListResponseDto> inquiries = inquiryService.getAllInquiries(pageable);
    return ResponseEntity.ok(inquiries);
  }

  @GetMapping("/{inquiryId}")
  public ResponseEntity<InquiryDetailResponseDto> getInquiryDetail(@PathVariable Long inquiryId) {
    InquiryDetailResponseDto response = inquiryService.getInquiryDetail(inquiryId);
    return ResponseEntity.ok(response);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping("/{inquiryId}/answer")
  public ResponseEntity<Void> answerInquiry(@PathVariable Long inquiryId, @Valid @RequestBody InquiryAnswerRequestDto request) {
    Long adminId = SecurityUtil.getCurrentMemberId();
    inquiryService.answerInquiry(inquiryId, request.getAnswer(), adminId);
    return ResponseEntity.ok().build();
  }
}
