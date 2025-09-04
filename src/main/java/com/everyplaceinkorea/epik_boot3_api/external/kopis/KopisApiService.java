package com.everyplaceinkorea.epik_boot3_api.external.kopis;

import com.everyplaceinkorea.epik_boot3_api.config.KopisApiConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

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
    
    public Mono<String> getPerformanceList(String stDate, String edDate, int cPage, int rows) {
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
                .timeout(Duration.ofMillis(kopisApiConfig.getApi().getTimeout()));
    }
    
    public Mono<String> getPerformanceDetail(String mt20id) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/pblprfr/{mt20id}")
                        .queryParam("service", kopisApiConfig.getApi().getKey())
                        .build(mt20id))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(kopisApiConfig.getApi().getTimeout()));
    }
}