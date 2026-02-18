package com.everyplaceinkorea.epik_boot3_api.admin.inquiry.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.everyplaceinkorea.epik_boot3_api.admin.inquiry.dto.InquiryDetailResponseDto;
import com.everyplaceinkorea.epik_boot3_api.admin.inquiry.dto.InquiryListResponseDto;

public interface InquiryService {
  Page<InquiryListResponseDto> getAllInquiries(Pageable pageable);
  InquiryDetailResponseDto getInquiryDetail(Long inquiryId);
  void answerInquiry(Long inquiryId, String answer, Long adminId);
} 
