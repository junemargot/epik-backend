package com.everyplaceinkorea.epik_boot3_api.external.kopis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "dbs")
public class KopisXmlResponseDto {

  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "db")
  private List<KopisPerformanceDto> performances;

  @JacksonXmlProperty(localName = "totalCount")
  private int totalCount;
}
