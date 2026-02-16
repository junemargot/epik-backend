package com.everyplaceinkorea.epik_boot3_api.member.inquiry.dto;

import com.everyplaceinkorea.epik_boot3_api.entity.inquiry.InquiryCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InquiryCreateRequestDto {

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 50, message = "제목은 50자를 초과할 수 없습니다.")
    private String title;

    @NotBlank(message = "문의 내용을 입력해주세요.")
    private String content;

    @NotNull(message = "문의 유형을 선택해주세요.")
    private InquiryCategory category;

    private boolean receiveEmailAnswer;
}
