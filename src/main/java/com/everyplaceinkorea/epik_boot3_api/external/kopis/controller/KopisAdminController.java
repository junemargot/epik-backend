package com.everyplaceinkorea.epik_boot3_api.external.kopis.controller;

import com.everyplaceinkorea.epik_boot3_api.external.kopis.service.KopisDataSyncService;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.dto.SyncResult;
import com.everyplaceinkorea.epik_boot3_api.repository.concert.ConcertRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.musical.MusicalRepository;
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
    private final ConcertRepository concertRepository;
    private final MusicalRepository musicalRepository;

    /**
     * 전체 데이터 수동 동기화
     */
    @PostMapping("/sync/all")
    public ResponseEntity<Map<String, Object>> syncAllData() {
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

    /**
     * KOPIS API 테스트 (실제 API 호출만)
     */
    @GetMapping("/test-api")
    public ResponseEntity<Map<String, Object>> testKopisApi() {
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

    /**
     * 콘서트 데이터만 동기화
     */
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

    /**
     * 뮤지컬 데이터만 동기화
     */
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
    @PostMapping("/sync/performance/{kopisId}")
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
     * 동기화 상태 확인
     */
    @GetMapping("/sync/status")
    public ResponseEntity<Map<String, Object>> getSyncStatus() {
        Map<String, Object> response = new HashMap<>();

        try {
            // 실제로는 데이터베이스에서 마지막 동기화 시간 등을 조회해야 함
            // 여기서는 기본 응답만 제공
            response.put("success", true);
            response.put("lastSyncTime", "2024-01-01 02:00:00"); // 실제 마지막 동기화 시간
            response.put("totalSyncedItems", 0); // 실제 동기화된 아이템 수
            response.put("status", "READY"); // RUNNING, COMPLETED, ERROR, READY
            response.put("message", "동기화 시스템이 정상적으로 동작 중입니다.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "상태 조회 중 오류가 발생했습니다: " + e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    // 테스트용 (개별 동기화 및 일부데이터)
    @PostMapping("/test-sync/single/{kopisId}")
    public ResponseEntity<Map<String, Object>> testSyncSingle(@PathVariable String kopisId) {
        Map<String, Object> response= new HashMap<>();

        try {
            log.info("개별 공연 동기화 테스트: {}", kopisId);

            syncService.syncSinglePerformance(kopisId);

            response.put("success", true);
            response.put("message", "개별 공연 동기화 성공: " + kopisId);

            return ResponseEntity.ok(response);
        } catch(Exception e) {
            log.error("개별 공연 동기화 테스트 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "동기화 실패: " + e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }
}
