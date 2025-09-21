package com.everyplaceinkorea.epik_boot3_api.external.kopis;

import com.everyplaceinkorea.epik_boot3_api.config.KopisApiConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
public class KopisApiService {
    
    private final WebClient webClient;
    private final KopisApiConfig kopisApiConfig;
    
    public KopisApiService(KopisApiConfig kopisApiConfig) {
        this.kopisApiConfig = kopisApiConfig;
        this.webClient = WebClient.builder()
                .baseUrl(kopisApiConfig.getApi().getBaseUrl())
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .defaultHeader("Accept", "application/xml, text/xml, */*")
                .defaultHeader("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.8")
                .build();
    }

    /**
     * 공연 목록 조회
     * @param stDate
     * @param edDate
     * @param cPage
     * @param rows
     * @return
     */
    public String getPerformanceList(String stDate, String edDate, int cPage, int rows) {
        try {
            log.info("KOPIS API 호출 시작");
            log.info("파라미터 - stDate: {}, edDate: {}, cPage: {}, rows: {}", stDate, edDate, cPage, rows);
            log.info("API KEY: {}", kopisApiConfig.getApi().getKey());
            log.info("Base URL: {}", kopisApiConfig.getApi().getBaseUrl());

            String finalUrl = webClient.get()
                    .uri(uriBuilder -> {
                        String builtUri = uriBuilder
                                .path("/pblprfr")
                                .queryParam("service", kopisApiConfig.getApi().getKey())
                                .queryParam("stdate", stDate)
                                .queryParam("eddate", edDate)
                                .queryParam("cpage", cPage)
                                .queryParam("rows", rows)
                                .build().toString();
                                log.info("최종 요청 URL: {}", builtUri);
                                return uriBuilder
                                    .path("/pblprfr")
                                    .queryParam("service", kopisApiConfig.getApi().getKey())
                                    .queryParam("stdate", stDate)
                                    .queryParam("eddate", edDate)
                                    .queryParam("cpage", cPage)
                                    .queryParam("rows", rows)
                                    .build();
                    })
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(kopisApiConfig.getApi().getTimeout()))
                    .block();

                    log.info("=== KOPIS API 응답 받음 ===");
                    log.info("응답 null 여부: {}", finalUrl == null);
                    log.info("응답 길이: {}", finalUrl != null ? finalUrl.length() : 0);

                    if (finalUrl != null && finalUrl.length() > 0) {
                        log.info("응답 시작 부분 (200자): {}",
                                finalUrl.length() > 200 ? finalUrl.substring(0, 200) : finalUrl);
                    }

                    return finalUrl;
        } catch(Exception e) {
            log.error("KOPIS API 호출 실패 상세: {}", e.getMessage(), e);
            throw new RuntimeException("KOPIS API 호출 실패:", e);
        }
    }

    /**
     * 장르별 조회
     * @param mt20id
     * @return
     */
    public String getPerformanceListByGenre(String stDate, String edDate, int cPage, int rows, String shcate) {
        try {
            log.info("KOPIS API 장르별 호출 - 장르코드: {}, 페이지: {}", shcate, cPage);

            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/pblprfr")
                            .queryParam("service", kopisApiConfig.getApi().getKey())
                            .queryParam("stdate", stDate)
                            .queryParam("eddate", edDate)
                            .queryParam("cpage", cPage)
                            .queryParam("rows", rows)
                            .queryParam("shcate", shcate)  // 장르 코드 파라미터 추가
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(kopisApiConfig.getApi().getTimeout()))
                    .block();

            log.info("장르 {} 응답 길이: {}", shcate, response != null ? response.length() : 0);
            return response;
        } catch(Exception e) {
            log.error("KOPIS API 장르별 호출 실패 - 장르: {}, 에러: {}", shcate, e.getMessage());
            throw new RuntimeException("KOPIS API 장르별 호출 실패:", e);
        }
    }
    
    public String getPerformanceDetail(String mt20id) {
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/pblprfr/{mt20id}")
                            .queryParam("service", kopisApiConfig.getApi().getKey())
                            .build(mt20id))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(kopisApiConfig.getApi().getTimeout()))
                    .block();
        } catch(Exception e) {
            log.error("KOPIS 상세정보 조회 실패: {}", e.getMessage());
            throw new RuntimeException("KOPIS 상세정보 조회 실패", e);
        }
    }

  /**
   * KOPIS 공연시설 API 연동
   * 1. 공연시설목록 조회로 시설명 -> 시설 ID 매핑
   * 2. 공연시설상세 조회로 시설 ID -> 상세주소 조회
   */
  public String getFacilityList(int cPage, int rows) {
    try {
      log.info("KOPIS 공연시설목록 조회 - 페이지: {}", cPage);

      String response = webClient.get()
              .uri(uriBuilder -> uriBuilder
                      .path("/prfplc")
                      .queryParam("service", kopisApiConfig.getApi().getKey())
                      .queryParam("cpage", cPage)
                      .queryParam("rows", rows)
                      .build())
              .retrieve()
              .bodyToMono(String.class)
              .timeout(Duration.ofMillis(kopisApiConfig.getApi().getTimeout()))
              .block();

      log.debug("공연 시설 목록 응답 길이: {}", response != null ? response.length() : 0);
      return response;
    } catch(Exception e) {
      log.error("KOPIS 공연시설목록 조회 실패: {}", e.getMessage());
      throw new RuntimeException("KOPIS 공연시설목록조회 실패", e);
    }
  }

  /**
   * 공연시설상세조회
   * URL: http://www.kopis.or.kr/openApi/restful/prfplc/{시설ID}
   */
  public String getFacilityDetail(String facilityId) {
    try {
      log.info("KOPIS 공연시설상세조회 - 시설ID: {}", facilityId);

      String response = webClient.get()
              .uri(uriBuilder -> uriBuilder
                      .path("/prfplc/{facilityId}")  // 공연시설상세조회 API
                      .queryParam("service", kopisApiConfig.getApi().getKey())
                      .build(facilityId))
              .retrieve()
              .bodyToMono(String.class)
              .timeout(Duration.ofMillis(kopisApiConfig.getApi().getTimeout()))
              .block();

      log.debug("공연시설상세 응답 길이: {}", response != null ? response.length() : 0);
      return response;
    } catch(Exception e) {
      log.error("KOPIS 공연시설상세조회 실패 - 시설ID: {}, 에러: {}", facilityId, e.getMessage());
      return null; // 실패해도 계속 진행
    }
  }
}