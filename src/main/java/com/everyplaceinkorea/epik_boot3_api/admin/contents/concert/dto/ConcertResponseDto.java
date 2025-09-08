package com.everyplaceinkorea.epik_boot3_api.admin.contents.concert.dto;

import com.everyplaceinkorea.epik_boot3_api.entity.common.DataSource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConcertResponseDto {
  // 응답받을 때 필요한 데이터
  private Long id;                 // 콘서트 식별 번호
  private String title;            // 공연 이름
  private String content;          // 내용
  private String writer;           // 작성자
  private String venue;            // 공연장
  private String address;          // 공연장 상세주소
  private LocalDate startDate;     // 시작일 -- YYYY.MM.DD
  private LocalDate endDate;       // 종료일
  private String runningTime;      // 공연시간
  private String ageRestriction;   // 관람연령
  private String saveImageName;    // 이미지
  private String imageUrl;         // 실제 이미지 URL (추가)
  private LocalDateTime writeDate; // 작성일
  private List<ConcertTicketPriceDto> ticketPrices;    // 티켓가격
  private List<ConcertTicketOfficeDto> ticketOffices;  // 티켓판매
  private String youtubeUrl;       // 유튜브 url
  private DataSource dataSource;   // 데이터 출처

  // 이미지 URL을 동적으로 생성하는 메서드
  public String getImageUrl() {
    if(dataSource == DataSource.KOPIS_API) {
      // KOPIS 데이터인 경우 외부 URL 그대로 사용
      return this.imageUrl;
    } else {
      // 입력 데이터인 경우 로컬 서버 경로 사용
      if(saveImageName != null && !saveImageName.trim().isEmpty()) {
        return "http://localhost:8081/api/v1/uploads/images/concert/" + saveImageName;
      }
    }
    return null; // 이미지가 없는 경우
  }
}
