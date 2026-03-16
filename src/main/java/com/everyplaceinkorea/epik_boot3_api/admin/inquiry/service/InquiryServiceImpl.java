package com.everyplaceinkorea.epik_boot3_api.admin.inquiry.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
  private final JavaMailSender mailSender;

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
              .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 회원입니다."));
    
    inquiry.addAnswer(answer, admin);

    if(inquiry.isReceiveEmailAnswer()) {
      sendAnswerEmail(inquiry, answer);
    }

    log.info("문의 답변 등록 - inquiryId: {}, adminId: {}", inquiryId, adminId);
  }

  private void sendAnswerEmail(Inquiry inquiry, String answer) {
    try {
      String memberEmail = inquiry.getWriter().getEmail();

      SimpleMailMessage message = new SimpleMailMessage();
      message.setTo(memberEmail);
      message.setSubject("[EPIK] 1:1 문의 답변 메일입니다.");
      message.setText(
          inquiry.getWriter().getNickname() + "님, 안녕하세요.\n\n"
        + "고객님의 문의에 대한 답변이 등록되었습니다.\n"
        + "아래의 답변은 마이페이지 -> 1:1 문의내역 페이지에서도 확인하실 수 있습니다.\n\n\n"
        + "──────────────────────────────────────────────────────────────\n"
        + "문의 제목: " + inquiry.getTitle() + "\n"
        + "──────────────────────────────────────────────────────────────\n"
        + "답변 내용: " + answer + "\n\n"
        + "──────────────────────────────────────────────────────────────\n"
        + "더 궁금하신 점이 있으시면 다시 문의해주세요.\n"
        + "감사합니다.\n\n"
        + "EPIK 고객지원팀"
      );

      mailSender.send(message);
      log.info("문의 답변 이메일 발송 완료 - to: {}", memberEmail);
    } catch(Exception e) {
      log.error("문의 답변 이메일 발송 실패 - inquiryId: {}", inquiry.getId(), e);
    }
  }
}