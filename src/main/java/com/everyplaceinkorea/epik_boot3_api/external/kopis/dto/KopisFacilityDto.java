package com.everyplaceinkorea.epik_boot3_api.external.kopis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KopisFacilityDto {
  private String fcltynm;   // 시설명
  private String mt10id;    // 시설 ID
  private String adres;     // 주소
  private String telno;     // 전화번호
  private String relateurl; // 홈페이지
  private String la;        // 위도
  private String lo;        // 경도
  private String seatscale; // 좌석 규모
}
