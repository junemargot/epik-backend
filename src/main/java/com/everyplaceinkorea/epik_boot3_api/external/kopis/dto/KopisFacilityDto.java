package com.everyplaceinkorea.epik_boot3_api.external.kopis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KopisFacilityDto {
  private String fcltynm;   // 시설명
  private String mt10id;    // 시설 ID
  private String adres;     // 주소 (상세주소)
  private String sidonm;    // 시도명
  private String gugunm;    // 구군명
  private String telno;     // 전화번호
  private String relateurl; // 홈페이지
  private String la;        // 위도
  private String lo;        // 경도
  private List<KopisHallDto> halls;  // 산하 공연장 목록
}
