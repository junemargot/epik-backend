package com.everyplaceinkorea.epik_boot3_api.image.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
public class ImageCacheService {

    private static final String CACHE_DIR = "uploads/cache/kopis";

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

            // 2. 캐시 확인
            if(cachePath.toFile().exists()) {
                log.debug("캐시 HIT: {}", cachedFileName);
                return "/cache/kopis/" + cachedFileName;
            }

            // 3. 캐시 MISS: 이미지 다운로드
            log.info("캐시 MISS: {} 다운로드 시작", imageUrl);
            byte[] imageBytes = downloadImage(imageUrl);

            // 4. 디렉토리 생성 및 저장
            Files.createDirectories(cachePath.getParent());
            Files.write(cachePath, imageBytes);

            log.info("캐싱 완료: {}", cachedFileName);
            return "/cache/kopis/" + cachedFileName;

        } catch(Exception e) {
            log.error("캐싱 실패: {}", imageUrl, e);
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
        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);

        if(response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        }

        throw new IOException("다운로드 실패: " + response.getStatusCode());
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
