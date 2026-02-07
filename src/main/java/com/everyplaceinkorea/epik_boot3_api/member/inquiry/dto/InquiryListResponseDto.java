package com.everyplaceinkorea.epik_boot3_api.member.inquiry.dto;

import com.everyplaceinkorea.epik_boot3_api.entity.inquiry.Inquiry;
import com.everyplaceinkorea.epik_boot3_api.entity.inquiry.InquiryStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InquiryListResponseDto {
    private Long id;
    private String title;

    @JsonFormat(pattern = "yyyy.MM.dd")
    private LocalDateTime createdAt;

    private InquiryStatus status;

    private String statusDescription; // "답변대기" or "답변완료"

    public static InquiryListResponseDto from(Inquiry inquiry) {
        return InquiryListResponseDto.builder()
                .id(inquiry.getId())
                .title(inquiry.getTitle())
                .createdAt(inquiry.getCreatedAt())
                .status(inquiry.getStatus())
                .statusDescription(inquiry.getStatus().getDescription())
                .build();
    }
}
