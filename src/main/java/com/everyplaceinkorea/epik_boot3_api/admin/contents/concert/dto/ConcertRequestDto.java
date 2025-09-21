package com.everyplaceinkorea.epik_boot3_api.admin.contents.concert.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConcertRequestDto {  // 등록할 때 필요한 데이터

  private String title;            // 제목
  private String content;          // 내용
  private String venue;            // 공연장
  private String address;          // 공연장 주소
  private LocalDate startDate;     // 시작일 -- YYYY.MM.DD
  private LocalDate endDate;       // 종료일
  private String runningTime;      // 공연시간
  private String ageRestriction;   // 관람연령
  private Long writer;             // 작성자 - 관리자페이지에서 필요
  private Long region;             // 지역
  private String youtubeUrl;       // 영상
  private String[] fileNames;      // 업로드한 파일명

  @JsonProperty("ticketPrices")
  private List<ConcertTicketPriceDto> concertTicketPrices;    // 티켓 가격
  @JsonProperty("ticketOffices")
  private List<ConcertTicketOfficeDto> concertTicketOffices;  // 티켓 판매처
}
