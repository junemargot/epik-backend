package com.everyplaceinkorea.epik_boot3_api.member.inquiry.service;

import com.everyplaceinkorea.epik_boot3_api.member.inquiry.dto.InquiryCreateRequestDto;
import com.everyplaceinkorea.epik_boot3_api.member.inquiry.dto.InquiryDetailResponseDto;
import com.everyplaceinkorea.epik_boot3_api.member.inquiry.dto.InquiryListResponseDto;
import com.everyplaceinkorea.epik_boot3_api.member.inquiry.dto.InquiryUpdateRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;


import java.util.List;

public interface InquiryService {

    // 1:1 문의 생성 (이미지 포함)
    Long createInquiry(InquiryCreateRequestDto request, List<MultipartFile> images, Long memberId);

    // 내 문의 목록 조회 (페이징)
    Page<InquiryListResponseDto> getMyInquiries(Long memberId, Pageable pageable);

    // 문의 상세 조회
    InquiryDetailResponseDto getInquiryDetail(Long inquireId, Long memberId);

    // 문의 수정
    void updateInquiry(Long inquireId, InquiryUpdateRequestDto request, List<MultipartFile> newImages, Long memberId);

    // 문의 삭제
    void deleteInquiry(Long inquireId, Long memberId);
}

