package com.everyplaceinkorea.epik_boot3_api.external.kopis.controller;

import com.everyplaceinkorea.epik_boot3_api.external.kopis.KopisApiService;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.dto.KopisFacilityDto;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.dto.MigrationResult;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.service.FacilityService;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.service.KopisDataSyncService;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.dto.SyncResult;
import com.everyplaceinkorea.epik_boot3_api.repository.concert.ConcertRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.musical.MusicalRepository;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin/kopis")
@RequiredArgsConstructor
public class KopisAdminController {
    private final KopisDataSyncService syncService;
    private final KopisApiService kopisApiService;
    private final FacilityService facilityService;

    @PostMapping("/sync/all")
    public ResponseEntity<Map<String, Object>> syncAll() {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("관리자 요청: 전체 KOPIS 데이터 동기화");

            LocalDate now = LocalDate.now();
            String startDate = now.withDayOfMonth(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String endDate = now.withDayOfMonth(now.lengthOfMonth()).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            // 전체 동기화 실행
            SyncResult concertResult = syncService.syncConcerts(startDate, endDate);
            SyncResult musicalResult = syncService.syncMusicals(startDate, endDate);

            // 결과 통합
            int totalSuccess = concertResult.getSuccessCount() + musicalResult.getSuccessCount();
            int totalFailure = concertResult.getFailureCount() + musicalResult.getFailureCount();
            int totalProcessed = concertResult.getTotalProcessed() + musicalResult.getTotalProcessed();

            response.put("success", true);
            response.put("message", "KOPIS 데이터 동기화가 완료되었습니다.");
            response.put("concertResult", concertResult);
            response.put("musicalResult", musicalResult);
            response.put("totalSummary", Map.of(
                "totalProcessed", totalProcessed,
                "totalSuccess", totalSuccess,
                "totalFailure", totalFailure
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("KOPIS 데이터 동기화 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "동기화 중 오류가 발생했습니다: " + e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/sync/concerts")
    public ResponseEntity<SyncResult> syncConcerts() {
        try {
            log.info("=== 관리자 요청: 콘서트 KOPIS 데이터 동기화 ===");

            SyncResult result = syncService.syncConcerts();

            log.info("콘서트 동기화 완료 - 처리 결과: 총 {}건 (성공: {}, 실패: {})",
                    result.getTotalProcessed(), result.getSuccessCount(), result.getFailureCount());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("콘서트 데이터 동기화 실패: {}", e.getMessage(), e);

            SyncResult errorResult = new SyncResult("CONCERT");
            errorResult.addFailure("콘서트 동기화 실패: " + e.getMessage());
            errorResult.complete();

            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    @PostMapping("/sync/musicals")
    public ResponseEntity<SyncResult> syncMusicals() {
        try {
            log.info("=== 관리자 요청: 뮤지컬 KOPIS 데이터 동기화 ===");

            SyncResult result = syncService.syncMusicals();

            log.info("뮤지컬 동기화 완료 - 처리 결과: 총 {}건 (성공: {}, 실패: {})",
                    result.getTotalProcessed(), result.getSuccessCount(), result.getFailureCount());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("뮤지컬 데이터 동기화 실패: {}", e.getMessage());

            SyncResult errorResult = new SyncResult("MUSICAL");
            errorResult.addFailure("뮤지컬 동기화 실패: " + e.getMessage());
            errorResult.complete();

            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * 개별 공연 동기화
     */
    @PostMapping("/sync/{kopisId}")
    public ResponseEntity<Map<String, Object>> syncSinglePerformance(@PathVariable String kopisId) {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("관리자 요청: 개별 공연 동기화 - {}", kopisId);

            syncService.syncSinglePerformance(kopisId);

            response.put("success", true);
            response.put("message", "공연 ID " + kopisId + " 동기화가 완료되었습니다.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("개별 공연 동기화 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "동기화 중 오류가 발생했습니다: " + e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * KOPIS API 테스트 (실제 API 호출만)
     */
    @GetMapping("/sync/test")
    public ResponseEntity<Map<String, Object>> testApiConnection() {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("KOPIS API 테스트 시작");

            LocalDate now = LocalDate.now();
            String startDate = now.withDayOfMonth(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String endDate = now.withDayOfMonth(now.lengthOfMonth()).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            // KOPIS API만 호출해서 데이터 받아오기 테스트
            String xmlResponse = syncService.testKopisApiCall(startDate, endDate);

            response.put("success", true);
            response.put("message", "KOPIS API 호출 성공");
            response.put("dataLength", xmlResponse != null ? xmlResponse.length() : 0);
            response.put("hasData", xmlResponse != null && !xmlResponse.trim().isEmpty());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("KOPIS API 테스트 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "KOPIS API 호출 실패: " + e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/test/facility/{facilityId}")
    public ResponseEntity<Map<String, Object>> testFacilityApi(@PathVariable String facilityId) {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("=== Facility API 테스트: {}", facilityId);

            // 1. API 호출
            String xmlResponse = kopisApiService.getFacilityDetail(facilityId);
            response.put("success", true);
            response.put("facilityId", facilityId);
            response.put("responseLength", xmlResponse != null ? xmlResponse.length() : 0);
            response.put("xmlResponse", xmlResponse); // 전체 XML 응답
            log.info("API 응답 길이: {}", xmlResponse != null ? xmlResponse.length() : 0);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Facility API 테스트 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Facility 파싱 테스트 (XML -> DTO 변환 확인)
     */
    @GetMapping("/test/facility/{facilityId}/parse")
    public ResponseEntity<Map<String, Object>> testFacilityParse(@PathVariable String facilityId) {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("=== Facility 파싱 테스트: {} ===", facilityId);

            // 1. API 호출
            String xmlResponse = kopisApiService.getFacilityDetail(facilityId);

            if (xmlResponse == null || xmlResponse.isEmpty()) {
                response.put("success", false);
                response.put("error", "API 응답 없음");
                return ResponseEntity.ok(response);
            }

            // 2. 파싱 시도
            KopisFacilityDto dto = facilityService.parseFacilityFromXml(xmlResponse);

            response.put("success", true);
            response.put("facilityId", facilityId);
            response.put("rawXml", xmlResponse);
            response.put("parsedDto", dto);
            response.put("hallCount", dto != null && dto.getHalls() != null ? dto.getHalls().size() : 0);

            log.info("파싱 결과 - 시설명: {}, Hall 개수: {}",
                    dto != null ? dto.getFcltynm() : "null",
                    dto != null && dto.getHalls() != null ? dto.getHalls().size() : 0);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Facility 파싱 테스트 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("stackTrace", e.getStackTrace());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 공연 상세 API 응답 확인용
     */
    @GetMapping("/test/performance/{performanceId}")
    public ResponseEntity<Map<String, Object>> testPerformanceApi(
            @PathVariable @Pattern(regexp = "^[A-Z]{2}\\d{6}$", message = "공연ID 형식이 올바르지 않습니다 (예: PF123456)")
            String performanceId) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (performanceId == null || performanceId.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "공연 ID는 필수입니다.");
                return ResponseEntity.badRequest().body(response);
            }

            log.info("=== Performance API 테스트: {} ===", performanceId);
            String xmlResponse = kopisApiService.getPerformanceDetail(performanceId);

            response.put("success", true);
            response.put("performanceId", performanceId);
            response.put("responseLength", xmlResponse != null ? xmlResponse.length() : 0);
            response.put("xmlResponse", xmlResponse);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            response.put("success", false);
            response.put("error", "잘못된 공연 ID 형식입니다.");
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Performance API 테스트 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", "API 호출 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
