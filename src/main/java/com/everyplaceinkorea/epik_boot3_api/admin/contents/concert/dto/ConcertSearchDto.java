package com.everyplaceinkorea.epik_boot3_api.admin.contents.concert.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConcertSearchDto {
  private Integer page;
  private String keyword;
  private String searchType; // 검색 타입
}
