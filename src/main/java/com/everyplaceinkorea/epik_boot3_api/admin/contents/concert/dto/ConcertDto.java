package com.everyplaceinkorea.epik_boot3_api.admin.contents.concert.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConcertDto {
  // 필요 데이터
  // 번호, 제목, 작성자, 등록일, 조회수, 총 게시물수
  private Long id;
  private String title;
  private String writer;
  private LocalDateTime writeDate;
  private Integer viewCount;
}
