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
                .build();
    }

    /**
     * 공연 목록 조회 - Blocking 방식
     * @param stDate
     * @param edDate
     * @param cPage
     * @param rows
     * @return
     */
    public String getPerformanceList(String stDate, String edDate, int cPage, int rows) {
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/pblprfr")
                            .queryParam("service", kopisApiConfig.getApi().getKey())
                            .queryParam("stdate", stDate)
                            .queryParam("eddate", edDate)
                            .queryParam("cpage", cPage)
                            .queryParam("rows", rows)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(kopisApiConfig.getApi().getTimeout()))
                    .block();
        } catch(Exception e) {
            log.error("KOPIS API 호출 실패: {}", e.getMessage());
            throw new RuntimeException("KOPIS API 호출 실패:", e);
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
     * 시설 목록 조회
     */
    public String getFacilityList(int cPage, int rows) {
        try {
            return webClient.get()
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
        } catch(Exception e) {
            log.error("KOPIS 시설 목록 조회 실패: {}", e.getMessage());
            throw new RuntimeException("KOPIS 시설 목록 조회 실패", e);
        }
    }

    public String getFacilityDetail(String mt10id) {
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/prfplc/{mt10id}")
                            .queryParam("service", kopisApiConfig.getApi().getKey())
                            .build(mt10id))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(kopisApiConfig.getApi().getTimeout()))
                    .block();
        } catch(Exception e) {
            log.error("KOPIS 시설 상세 조회 실패: {}", e.getMessage());
            throw new RuntimeException("KOPIS 시설 상세 조회 실패", e);
        }
    }
}