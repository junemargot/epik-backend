package com.everyplaceinkorea.epik_boot3_api.image.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ImageCacheService {

    @Value("${image-cache.thread-pool-size:5}")
    private int threadPoolSize;

    @Value("${image-cache.connect-timeout:10000}")
    private int connectTimeout;

    @Value("${image-cache.read-timeout:10000}")
    private int readTimeout;

    @Value("${image-cache.cache-directory:uploads/cache/kopis}")
    private String CACHE_DIR;

    private ExecutorService executorService;
    private final Set<String> downloadingImages = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void init() {
        executorService = Executors.newFixedThreadPool(threadPoolSize);
        log.info("ImageCacheService 초기화 완료");
        log.info("ThreadPool 크기: {}", threadPoolSize);
        log.info("연결 타임아웃: {}ms", connectTimeout);
        log.info("읽기 타임아웃: {}ms", readTimeout);
        log.info("캐시 디렉토리: {}", CACHE_DIR);
    }

    /**
     * KOPIS 이미지 동적 캐싱
     * @param imageUrl KOPIS 원본 이미지 URL
     * @param performanceId 공연 ID
     * @return 캐시된 이미지 경로 또는 원본 URL
     */
    public String getOrCacheImage(String imageUrl, String performanceId) {
        if(imageUrl == null || imageUrl.isEmpty()) return null;

        try {
            // 1. 캐시 파일명 생성
            String fileExtension = extractFileExtension(imageUrl);
            String cachedFileName = performanceId + "_poster." + fileExtension;
            Path cachePath = Paths.get(CACHE_DIR, cachedFileName);

            // 2. 캐시 HIT - 즉시 반환
            if (cachePath.toFile().exists()) {
                log.debug("캐시 HIT: {}", cachedFileName);
                return "/cache/kopis/" + cachedFileName;
            }

            // 3. 이미 다운로드 중인지 확인 (중복 방지)
            if (downloadingImages.contains(cachedFileName)) {
                log.debug("다운로드 진행 중: {} - 원본 URL 반환", cachedFileName);
                return imageUrl;
            }

            // 3. 캐시 MISS: 이미지 다운로드
            log.info("캐시 MISS: {} 비동기 다운로드 시작", cachedFileName);
            downloadingImages.add(cachedFileName); // 다운로드 중 표시

            // 백그라운드 다운로드
            executorService.submit(() -> {
                try {
                    log.info("백그라운드 다운로드: {}", cachedFileName);
                    byte[] imageBytes = downloadImage(imageUrl);

                    Files.createDirectories(cachePath.getParent());
                    Files.write(cachePath, imageBytes);

                    log.info("캐싱 완료: {} ({}KB)",
                            cachedFileName,
                            imageBytes.length / 1024);

                } catch (Exception e) {
                    log.error("비동기 캐싱 실패: {}", cachedFileName, e);

                } finally {
                    // 다운로드 완료 (성공/실패 무관)
                    downloadingImages.remove(cachedFileName);
                }
            });

            return imageUrl;

        } catch(Exception e) {
            log.error("캐싱 프로세스 에러: {}", imageUrl, e);
            return imageUrl;
        }
    }

    /**
     * KOPIS 서버에서 이미지 다운로드
     * @param url
     * @return
     * @throws IOException
     */
    private byte[] downloadImage(String url) throws IOException {
        RestTemplate restTemplate = new RestTemplate();

        // 타임아웃 설정
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        restTemplate.setRequestFactory(factory);

        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);

        if(response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        }

        throw new IOException("다운로드 실패: " + response.getStatusCode());
    }

    /**
     * 서버 종료 시 스레드풀 정리
     */
    @PreDestroy
    public void shutdown() {
        log.info("ImageCacheService 종료 중...");
        executorService.shutdown();

        try {
            if(!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("60초 내 종료 실패 - 강제 종료");
                executorService.shutdownNow();
            }
        } catch(InterruptedException e) {
            log.error("종료 중 인터럽트 발생");
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("ImageCacheService 종료 완료");
    }

    /**
     * 캐시 통계
     * @return
     */
    public Map<String, Object> getCacheStats() {
        try {
            long cacheCount = Files.list(Paths.get(CACHE_DIR)).count();
            long totalSize = Files.walk(Paths.get(CACHE_DIR))
                    .filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch(IOException e) {
                            return 0L;
                        }
                    })
                    .sum();

            return Map.of(
                    "cachedFiles", cacheCount,
                    "totalSizeMB", totalSize / 1024 / 1024,
                    "downloadingCount", downloadingImages.size()
            );
        } catch(IOException e) {
            return Map.of("error", e.getCause());
        }
    }

    /**
     * URL에서 파일 확장자 추출
     * @param performanceId
     * @param originalUrl
     * @return
     */
    private String extractFileExtension(String url) {
        // URL에서 파일명 추출
        String fileName = url.substring(url.lastIndexOf('/') + 1);

        // 쿼리 파라미터 제거
        if(fileName.contains("?")) {
            fileName = fileName.substring(0, fileName.indexOf('?'));
        }

        // 확장자 추출
        if(fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        }

        return "jpg";
    }
}
