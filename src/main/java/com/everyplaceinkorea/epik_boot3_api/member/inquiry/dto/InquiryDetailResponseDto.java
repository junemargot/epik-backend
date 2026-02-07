package com.everyplaceinkorea.epik_boot3_api.member.inquiry.dto;

import com.everyplaceinkorea.epik_boot3_api.entity.inquiry.Inquiry;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class InquiryDetailResponseDto {

    // 카테고리 정보
    private String parentCategory;
    private String categoryDescription;

    // 문의 내용
    private String content;

    // 이미지
    private List<InquiryImageDto> images;

    // 답변 정보 (있으면 표시)
    private String answer;

    public static InquiryDetailResponseDto from(Inquiry inquiry) {
        return InquiryDetailResponseDto.builder()
                .parentCategory(inquiry.getCategory().getParentCategory())
                .categoryDescription(inquiry.getCategory().getDescription())
                .content(inquiry.getContent())
                .images(inquiry.getImages().stream()
                        .map(InquiryImageDto::from)
                        .collect(Collectors.toList()))
                .answer(inquiry.getAnswer())
                .build();

    }


}
