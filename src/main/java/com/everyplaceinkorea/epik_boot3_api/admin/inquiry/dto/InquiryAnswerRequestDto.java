package com.everyplaceinkorea.epik_boot3_api.admin.inquiry.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InquiryAnswerRequestDto {
  
  @NotBlank(message = "답변 내용을 입력해주세요.")
  private String answer;
}
