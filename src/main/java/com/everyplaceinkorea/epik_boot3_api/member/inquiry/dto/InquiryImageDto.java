package com.everyplaceinkorea.epik_boot3_api.member.inquiry.dto;

import com.everyplaceinkorea.epik_boot3_api.entity.inquiry.InquiryImage;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InquiryImageDto {
    private Long id;
    private String imagePath; // 클라이언트에서 접근할 URL
    private String originalFilename;
    private Long fileSize;

    public static InquiryImageDto from(InquiryImage image) {
        return InquiryImageDto.builder()
                .id(image.getId())
                .imagePath(image.getImagePath())
                .originalFilename(image.getOriginalFilename())
                .fileSize(image.getFileSize())
                .build();
    }
}
