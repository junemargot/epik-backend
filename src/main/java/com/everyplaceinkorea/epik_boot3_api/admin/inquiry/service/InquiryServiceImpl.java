package com.everyplaceinkorea.epik_boot3_api.admin.inquiry.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.everyplaceinkorea.epik_boot3_api.admin.inquiry.dto.InquiryDetailResponseDto;
import com.everyplaceinkorea.epik_boot3_api.admin.inquiry.dto.InquiryListResponseDto;
import com.everyplaceinkorea.epik_boot3_api.entity.inquiry.Inquiry;
import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import com.everyplaceinkorea.epik_boot3_api.repository.Member.MemberRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.inquiry.InquiryRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryServiceImpl implements InquiryService{
  
  private final InquiryRepository inquiryRepository;
  private final MemberRepository memberRepository;

  @Override
  public Page<InquiryListResponseDto> getAllInquiries(Pageable pageable) {
    Page<Inquiry> inquiries = inquiryRepository.findAll(pageable);
    return inquiries.map(InquiryListResponseDto::from);
  }

  @Override
  public InquiryDetailResponseDto getInquiryDetail(Long inquiryId) {
    Inquiry inquiry = inquiryRepository.findById(inquiryId)
            .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 문의입니다."));
    return InquiryDetailResponseDto.from(inquiry);
  }

  @Override
  @Transactional
  public void answerInquiry(Long inquiryId, String answer, Long adminId) {
    Inquiry inquiry = inquiryRepository.findById(inquiryId)
              .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 문의입니다."));
    
    Member admin = memberRepository.findById(adminId)
              .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
    
    inquiry.addAnswer(answer, admin);

    log.info("문의 답변 등록 - inquiryId: {}, adminId: {}", inquiryId, adminId);
  }
}
