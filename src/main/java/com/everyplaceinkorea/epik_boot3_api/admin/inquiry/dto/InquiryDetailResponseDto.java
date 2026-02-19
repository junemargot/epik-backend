package com.everyplaceinkorea.epik_boot3_api.admin.inquiry.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.everyplaceinkorea.epik_boot3_api.entity.inquiry.Inquiry;
import com.everyplaceinkorea.epik_boot3_api.entity.inquiry.InquiryStatus;
import com.everyplaceinkorea.epik_boot3_api.member.inquiry.dto.InquiryImageDto;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InquiryDetailResponseDto {
  private Long id;
  
  private String parentCategory;

  private String categoryDescription;

  private String title;

  private String content;

  private String writer;

  @JsonFormat(pattern = "yyyy.MM.dd HH:mm")
  private LocalDateTime createdAt;

  private InquiryStatus status;

  private String statusDescription;

  private List<InquiryImageDto> images;

  private String answer;

  @JsonFormat(pattern = "yyyy.MM.dd HH:mm")
  private LocalDateTime answeredAt;

  public static InquiryDetailResponseDto from(Inquiry inquiry) {
    return InquiryDetailResponseDto.builder()
              .id(inquiry.getId())
              .parentCategory(inquiry.getCategory().getParentCategory())
              .categoryDescription(inquiry.getCategory().getDescription())
              .title(inquiry.getTitle())
              .content(inquiry.getContent())
              .writer(inquiry.getWriter().getNickname())
              .createdAt(inquiry.getCreatedAt())
              .status(inquiry.getStatus())
              .statusDescription(inquiry.getStatus().getDescription())
              .images(inquiry.getImages().stream()
                        .map(InquiryImageDto::from)
                        .collect(Collectors.toList()))
              .answer(inquiry.getAnswer())
              .answeredAt(inquiry.getAnsweredAt())
              .build();
  }
}
