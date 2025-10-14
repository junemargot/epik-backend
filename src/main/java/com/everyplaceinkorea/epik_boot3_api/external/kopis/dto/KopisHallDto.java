package com.everyplaceinkorea.epik_boot3_api.external.kopis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KopisHallDto {
  private String mt13id;    // 공연장 ID
  private String prfplcnm;  // 공연장명
  private String seatscale; // 좌석 규모
  private String mt10id;    // 공연시설 ID
}
