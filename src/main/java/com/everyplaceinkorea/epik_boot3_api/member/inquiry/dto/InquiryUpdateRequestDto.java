package com.everyplaceinkorea.epik_boot3_api.member.inquiry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InquiryUpdateRequestDto {

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 50, message = "제목은 50자를 초과할 수 없습니다.")
    private String title;

    @NotBlank(message = "문의 내용을 입력해주세요.")
    @Size(max = 5000, message = "문의 내용은 5000자를 초과할 수 없습니다.")
    private String content;

    // 이미지 수정 방식 선택 (선택사항)
    private boolean keepExistingImages = false; // true면 기존 이미지 유지
}
