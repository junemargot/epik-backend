package com.everyplaceinkorea.epik_boot3_api.image.service;

import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.*;

/**
 * 이미지 비동기 캐싱 및 WebP 변환을 처리하는 서비스 클래스
 * 외부 이미지 URL을 받아, 다운로드 후 WebP 형식으로 변환하여 내부에 캐싱
 * Non-blocking 방식으로 동작하여 캐시가 없는 경우에도 원본 URL을 즉시 반환하여 응답 지연을 최소화
 */
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
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadPoolSize);
        executor.setMaxPoolSize(threadPoolSize * 2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("image-cache-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        executorService = executor.getThreadPoolExecutor();

        log.info("ImageCacheService 초기화 완료 - ThreadPool 크기: {}", threadPoolSize);
    }


    /**
     * KOPIS 이미지 동적 캐싱
     * @param imageUrl 캐싱할 KOPIS 원본 이미지 URL
     * @param performanceId 캐시 파일명을 생성하기 위한 고유 ID
     * @return 캐시된 이미지 경로 또는 원본 URL
     */
    public String getOrCacheImage(String imageUrl, String performanceId) {
        if(imageUrl == null || imageUrl.isEmpty()) return null;

        try {
            // 1. 캐시 파일명 생성
            String fileExtension = extractFileExtension(imageUrl);
            String cachedFileName = performanceId + "_poster." + fileExtension;
            Path cachePath = Paths.get(CACHE_DIR, cachedFileName);

            // 2. 캐시 HIT
            if (cachePath.toFile().exists()) {
                log.debug("캐시 HIT: {}", cachedFileName);
                return "/cache/kopis/" + cachedFileName;
            }

            // 3. 중복 다운로드 방지
            if (downloadingImages.contains(cachedFileName)) {
                log.debug("다운로드 진행 중: {}", cachedFileName);
                return imageUrl;
            }

            // 3. 캐시 MISS: 비동기 다운로드 및 변환
            log.info("캐시 MISS: {} 비동기 다운로드 시작", cachedFileName);
            downloadingImages.add(cachedFileName);

            executorService.submit(() -> {
                try {
                    log.info("백그라운드 다운로드: {}", cachedFileName);

                    // 원본 다운로드
                    byte[] imageBytes = downloadImage(imageUrl);
                    log.info("다운로드 완료: {}KB", imageBytes.length / 1024);

                    // 저장
                    Files.createDirectories(cachePath.getParent());
                    Files.write(cachePath, imageBytes);

                    log.info("캐싱 완료: {} ({}KB)", cachedFileName, imageBytes.length / 1024);

                } catch (IOException e) {
                    log.error("네트워크 오류로 캐싱 실패: {}", cachedFileName, e);
                } catch (SecurityException e) {
                    log.error("보안 검증 실패: {}", cachedFileName, e);
                } catch (Exception e) {
                    log.error("예상치 못한 오류로 캐싱 실패: {}", cachedFileName, e);
                } finally {
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
     * @param url 다운로드할 이미지의 URL
     * @return 다운로드된 이미지의 byte 배열
     * @throws IOException 다운로드 실패 또는 HTTP 상태 코드가 200대가 아닐 경우
     */
    private byte[] downloadImage(String url) throws IOException {
        // URL 검증
        if(!isValidImageUrl(url)) {
            throw new IllegalArgumentException("Invalid image URL: " + url);
        }

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(createHttpRequestFactory());

        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);

        // Content-Type 검증
        String contentType = response.getHeaders().getContentType().toString();
        if(!contentType.startsWith("image/")) {
            throw new IOException("Invalid content type: " + contentType);
        }

        return response.getBody();
    }

    /**
     * 애플리케이션 종료 시 스레드 풀 종료
     */
    @PreDestroy
    public void shutdown() {
        log.info("ImageCacheService 종료 중...");
        executorService.shutdown();

        try {
            if(!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("60초 내 종료 실패 - 강제 종료 실행");
                executorService.shutdownNow();
            }
        } catch(InterruptedException e) {
            log.error("종료 중 인터럽트 발생, 강제 종료");
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("ImageCacheService 종료 완료");
    }

    /**
     * URL 문자열에서 파일 확장자 추출
     * @param url 확장자를 추출할 URL
     * @return 소문자로 변환된 파일 확장자. 없으면 "jpg"를 기본값으로 반환
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

    /**
     * HTTP 요청 Factory 생성 (타임아웃 설정 포함)
     */
    private SimpleClientHttpRequestFactory createHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return factory;
    }


    private boolean isValidImageUrl(String url) {
        try {
            URL urlObj = new URL(url);
            String host = urlObj.getHost().toLowerCase();

            // 내부 IP 및 localhost 차단
            if (host.equals("localhost") || host.equals("127.0.0.1") ||
                    host.startsWith("192.168.") || host.startsWith("10.") ||
                    host.startsWith("172.")) {
                return false;
            }

            // KOPIS 도메인만 허용하도록 제한
            return host.contains("kopis.or.kr");

        } catch (Exception e) {
            return false;
        }
    }
}
