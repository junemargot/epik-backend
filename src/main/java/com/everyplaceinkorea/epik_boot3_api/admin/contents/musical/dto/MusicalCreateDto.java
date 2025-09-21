package com.everyplaceinkorea.epik_boot3_api.admin.contents.musical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class MusicalCreateDto { // 클라이언트의 뮤지컬 등록 요청 데이터를 담을 dto
    private String title; // 제목
    private String content; // 내용
    private Long writer; // 작성자
    private Long region; // 지역
    private String address; // 주소
    private String venue; // 장소
    private LocalDate startDate; // 시작일
    private LocalDate endDate; // 종료일
    private String runningTime; // 관람시간
    private String ageRestriction; // 관람 연령
    private String[] fileNames; // 업로드한 파일명
    private List<MusicalTicketPriceDto> ticketPrices; // 티켓 금액
    private List<MusicalTicketOfficeDto> ticketOffices; // 티켓 예매처
}