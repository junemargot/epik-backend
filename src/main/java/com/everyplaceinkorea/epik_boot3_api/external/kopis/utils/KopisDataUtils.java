package com.everyplaceinkorea.epik_boot3_api.external.kopis.utils;

import com.everyplaceinkorea.epik_boot3_api.external.kopis.dto.KopisPerformanceDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * KOPIS 데이터 처리 공통 유틸리티 클래스
 * 
 * Concert와 Musical 엔티티에서 중복되던 로직들을 공통화
 * - 제목 정리 (HTML 엔티티 디코딩, 불필요한 텍스트 제거)
 * - 날짜 파싱
 * - 시간 변환
 * - 연령 제한 정규화
 * - 기타 공통 처리 로직
 */
@Component
@Slf4j
public class KopisDataUtils {
    
    /**
     * KOPIS 제목 정리 (HTML 엔티티 디코딩 + 불필요한 텍스트 제거)
     * 
     * @param title 원본 KOPIS 제목
     * @return 정리된 제목
     */
    public static String normalizeTitle(String title) {
        if (!isValidString(title)) {
            return null;
        }

        log.debug("제목 정리: 원본=[{}]", title);

        String cleaned = title.trim()
                // HTML 엔티티 디코딩
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                // 불필요한 텍스트 제거
                .replaceAll("\\[.*?\\]", "")  // [대학로], [앵콜] 등
                .replaceAll("\\(.*?재공연.*?\\)", "")  // (재공연)
                .replaceAll("\\(.*?앵콜.*?\\)", "")   // (앵콜)
                .replaceAll("\\s+", " ")      // 연속된 공백 정리
                .trim();

        log.debug("제목 정리: 결과=[{}]", cleaned);
        return cleaned.isEmpty() ? title : cleaned;
    }

    /**
     * 안전한 날짜 파싱
     * KOPIS 날짜 형식 (yyyyMMdd)을 LocalDate로 변환
     *
     * @param kopisDate KOPIS 날짜 문자열
     * @return 파싱된 LocalDate (실패 시 현재 날짜)
     */
    public static LocalDate parseDate(String kopisDate) {
        if (!isValidString(kopisDate)) {
            log.warn("빈 날짜 정보: {}", kopisDate);
            return LocalDate.now();
        }

        try {
            String cleanDate = kopisDate.trim().replaceAll("[^0-9]", "");
            if (cleanDate.length() == 8) {
                return LocalDate.parse(cleanDate, DateTimeFormatter.ofPattern("yyyyMMdd"));
            } else {
                log.warn("잘못된 날짜 형식: {} -> {}", kopisDate, cleanDate);
                return LocalDate.now();
            }
        } catch(Exception e) {
            log.error("날짜 파싱 실패: {}", kopisDate, e);
            return LocalDate.now();
        }
    }

    /**
     * 러닝타임 결정
     * KOPIS DTO에서 러닝타임 정보를 추출하고 변환
     *
     * @param dto KOPIS 공연 정보
     * @return 변환된 러닝타임 (분 단위)
     */
    public static String determineRunningTime(KopisPerformanceDto dto) {
        if (isValidString(dto.getPrfruntime())) {
            return dto.getPrfruntime();
        }
        return null;
    }

    /**
     * URL에서 파일명 추출
     *
     * @param url 이미지 URL
     * @return 추출된 파일명 (실패 시 기본값)
     */
    public static String generateFileName(String url) {
        if (!isValidString(url)) {
            return "default_poster.jpg";
        }

        try {
            // URL에서 파일명 부분 추출
            String fileName = url.substring(url.lastIndexOf("/") + 1);
            // 쿼리 파라미터 제거
            if (fileName.contains("?")) {
                fileName = fileName.substring(0, fileName.indexOf("?"));
            }
            // 확장자가 없는 경우 기본 확장자 추가
            if (!fileName.contains(".")) {
                fileName += ".jpg";
            }
            return fileName;

        } catch (Exception e) {
            log.warn("URL 파일명 추출 실패: {}", url, e);
            return "kopis_poster_" + System.currentTimeMillis() + ".jpg";
        }
    }

    /**
     * 기본 콘텐츠 생성
     * KOPIS 정보를 기반으로 표준 설명 텍스트 생성
     *
     * @param dto KOPIS 공연 정보
     * @return 생성된 콘텐츠
     */
    public static String generateContent(KopisPerformanceDto dto) {
        StringBuilder content = new StringBuilder();

        if (isValidString(dto.getGenrenm())) {
            content.append("장르: ").append(dto.getGenrenm()).append("\n");
        }
        if (isValidString(dto.getPrfstate())) {
            content.append("공연상태: ").append(dto.getPrfstate()).append("\n");
        }
        if (isValidString(dto.getArea())) {
            content.append("지역: ").append(dto.getArea()).append("\n");
        }
        if (isValidString(dto.getFcltynm())) {
            content.append("공연장: ").append(dto.getFcltynm()).append("\n");
        }

        content.append("\nKOPIS에서 제공하는 공연 정보입니다.");
        return content.toString();
    }

    /**
     * 상세 주소 생성
     * 지역 정보와 공연장명을 조합하여 상세 주소 생성
     *
     * @param area 지역 정보
     * @param fcltynm 공연장명
     * @return 생성된 상세 주소
     */
    public static String generateAddress(String area, String fcltynm) {
        StringBuilder address = new StringBuilder();

        if (isValidString(area)) {
            address.append(area);
        }
        if (isValidString(fcltynm)) {
            if (address.length() > 0) {
                address.append(" ");
            }
            address.append(fcltynm);
        }

        return address.length() > 0 ? address.toString() : "주소 정보 없음";
    }
    
    /**
     * 문자열 유효성 검사
     * 
     * @param str 검사할 문자열
     * @return 유효한 문자열 여부
     */
    public static boolean isValidString(String str) {
        return str != null && !str.trim().isEmpty();
    }
}
