package com.everyplaceinkorea.epik_boot3_api.admin.inquiry.dto;

import java.time.LocalDateTime;

import com.everyplaceinkorea.epik_boot3_api.entity.inquiry.Inquiry;
import com.everyplaceinkorea.epik_boot3_api.entity.inquiry.InquiryStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InquiryListResponseDto {
  private Long id;

  private String parentCategory;

  private String categoryDescription;
  
  private String title;
  
  private String writer;
  
  @JsonFormat(pattern = "yyyy.MM.dd")
  private LocalDateTime createdAt;
  
  @JsonFormat(pattern = "yyyy.MM.dd")
  private LocalDateTime answeredAt;
  
  private InquiryStatus status;
  
  private String statusDescription;

  public static InquiryListResponseDto from(Inquiry inquiry) {
    return InquiryListResponseDto.builder()
              .id(inquiry.getId())
              .parentCategory(inquiry.getCategory().getParentCategory())
              .categoryDescription(inquiry.getCategory().getDescription())
              .title(inquiry.getTitle())
              .writer(inquiry.getWriter().getNickname())
              .createdAt(inquiry.getCreatedAt())
              .answeredAt(inquiry.getAnsweredAt())
              .status(inquiry.getStatus())
              .statusDescription(inquiry.getStatus().getDescription())
              .build();
  }
}
