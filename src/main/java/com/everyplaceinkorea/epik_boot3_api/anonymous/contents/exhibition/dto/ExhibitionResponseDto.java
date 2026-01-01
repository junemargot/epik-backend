package com.everyplaceinkorea.epik_boot3_api.anonymous.contents.exhibition.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ExhibitionResponseDto {
    // 번호
    private Long id;
    // 대표이미지
    private String fileSavedName;
    // 제목
    private String title;
    // 장소
    private String venue;
    // 시작일
    private LocalDate startDate;
    // 종료일
    private LocalDate endDate;

    private String performanceStatus;
}
