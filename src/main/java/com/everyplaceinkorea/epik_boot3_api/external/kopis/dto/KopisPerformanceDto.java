package com.everyplaceinkorea.epik_boot3_api.external.kopis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KopisPerformanceDto {
  private String mt20id;      // 공연ID
  private String prfnm;       // 공연명
  private String prfpdfrom;   // 공연시작일
  private String prfpdto;     // 공연종료일
  private String fcltynm;     // 공연시설명
  private String poster;      // 포스터이미지경로
  private String area;        // 지역
  private String genrenm;     // 장르명
  private String openrun;     // 오픈런
  private String prfstate;    // 공연상태
}
