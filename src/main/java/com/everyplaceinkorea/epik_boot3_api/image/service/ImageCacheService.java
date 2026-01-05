package com.everyplaceinkorea.epik_boot3_api.image.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
        executorService = Executors.newFixedThreadPool(threadPoolSize);
        log.info("ImageCacheService 초기화 완료");
        log.info("ThreadPool 크기: {}", threadPoolSize);
        log.info("연결 타임아웃: {}ms", connectTimeout);
        log.info("읽기 타임아웃: {}ms", readTimeout);
        log.info("캐시 디렉토리: {}", CACHE_DIR);
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
            // 1. WebP 형식으로 캐시 파일명 생성
            String cachedFileName = performanceId + "_poster.webp";
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
                    byte[] originalBytes = downloadImage(imageUrl);
                    long originalSize = originalBytes.length;

                    // WebP 변환
                    byte[] webpBytes = convertToWebP(originalBytes);

                    // 저장
                    Files.createDirectories(cachePath.getParent());
                    Files.write(cachePath, webpBytes);

                    log.info("캐싱 완료: {} (원본: {}KB -> WebP: {}KB, {:.1f}% 감소)",
                            cachedFileName,
                            originalSize / 1024,
                            webpBytes.length / 1024,
                            (1 - (double)webpBytes.length / originalSize) * 100);

                } catch (Exception e) {
                    log.error("비동기 캐싱 실패: {}", cachedFileName, e);

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
     * 이미지 byte 배열을 WebP 형식으로 변환
     * @param imageBytes 원본 이미지의 byte 배열
     * @return WebP로 변환된 이미지의 byte 배열. 실패 시 원본 반환
     * @throws IOException 원본 이미지를 읽을 수 없을 때 발생
     */
    private byte[] convertToWebP(byte[] imageBytes) throws IOException {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if(image == null) {
                throw new IOException("원본 이미지 데이터를 읽을 수 없습니다.");
            }

            // WebP ImageWriter 가져오기
            ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/webp").next();

            // WebP 변환 파라미터 설정 (손실 압축, 품질 80%)
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionType("Lossy");
            writeParam.setCompressionQuality(0.8f);

            // 메모리 내의 ByteArray로 WebP 이미지 출력
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try(ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), writeParam);
            }

            writer.dispose();
            return baos.toByteArray();

        } catch(Exception e) {
            log.warn("WebP 변환 실패, 원본 이미지 형식으로 저장: {}", e.getMessage());
            return imageBytes;
        }
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
     * 현재 캐시 상태에 대한 통계를 반환
     * @return 캐시된 파일 수, 총 크기(MB), 현재 다운로드 중인 파일 수를 담은 Map
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
            log.error("캐시 통계 조회 실패");
            return Map.of("error", e.getCause());
        }
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
}
