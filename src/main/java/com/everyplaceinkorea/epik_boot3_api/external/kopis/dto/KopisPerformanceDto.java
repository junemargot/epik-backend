package com.everyplaceinkorea.epik_boot3_api.external.kopis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KopisPerformanceDto {
  private String mt20id;         // 공연 ID
  private String mt10id;         // 공연시설 ID
  private String prfnm;          // 공연명
  private String prfpdfrom;      // 공연시작일
  private String prfpdto;        // 공연종료일
  private String fcltynm;        // 공연시설명
  private String poster;         // 포스터이미지경로
  private String area;           // 지역
  private String genrenm;        // 장르명
  private String prfstate;       // 공연상태
  private String prftime;        // 공연시간
  private String pcseguidance;   // 티켓가격
  private String dtguidance;     // 공연시간 상세
  private String styurls;        // 소개이미지목록
  private String prfruntime;     // 런타임
  private String prfage;         // 관람연령
  private String child;          // 아동 공연 여부 (Y/N)
  private String visit;          // 내한 공연 여부 (Y/N)
}
