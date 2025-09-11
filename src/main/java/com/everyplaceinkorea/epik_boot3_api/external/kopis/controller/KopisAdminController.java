package com.everyplaceinkorea.epik_boot3_api.external.kopis.controller;

import com.everyplaceinkorea.epik_boot3_api.entity.concert.Concert;
import com.everyplaceinkorea.epik_boot3_api.entity.musical.Musical;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.dto.KopisPerformanceDto;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.service.KopisDataSyncService;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.scheduler.KopisSchedulerService;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.dto.SyncResult;
import com.everyplaceinkorea.epik_boot3_api.repository.concert.ConcertRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.musical.MusicalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin/kopis")
@RequiredArgsConstructor
public class KopisAdminController {

    private final KopisDataSyncService syncService;
    private final KopisSchedulerService schedulerService;
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
            SyncResult concertResult = syncService.syncAllConcerts(startDate, endDate);
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
     * Concert 생성 테스트
     */
    @PostMapping("/test-create")
    public ResponseEntity<Map<String, Object>> testCreateConcert() {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Concert 생성 테스트 요청");

            syncService.testCreateConcert();

            response.put("success", true);
            response.put("message", "테스트 Concert 생성 성공");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Concert 생성 테스트 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Concert 생성 실패: " + e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 데이터베이스 연결 테스트
     */
    @GetMapping("/test-db")
    public ResponseEntity<Map<String, Object>> testDatabase() {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("데이터베이스 연결 테스트 시작");

            // 기본 엔티티들 조회 테스트
            long memberCount = syncService.testDatabaseConnection();

            response.put("success", true);
            response.put("message", "데이터베이스 연결 정상");
            response.put("memberCount", memberCount);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("데이터베이스 테스트 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "데이터베이스 연결 실패: " + e.getMessage());

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
     * 테스트용 엔드포인트
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "KOPIS 컨트롤러가 정상 작동합니다.");
        response.put("timestamp", java.time.LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    /**
     * 콘서트 데이터만 동기화
     */
    @PostMapping("/sync/concerts")
    public ResponseEntity<SyncResult> syncConcerts() {
        try {
            log.info("관리자 요청: 콘서트 KOPIS 데이터 동기화");

            LocalDate now = LocalDate.now();
            String startDate = now.withDayOfMonth(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String endDate = now.withDayOfMonth(now.lengthOfMonth()).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            log.info("콘서트 동기화 날짜 범위: {} ~ {}", startDate, endDate);

            SyncResult result = syncService.syncAllConcerts(startDate, endDate);
            log.info("콘서트 동기화 완료 - 응답 데이터: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("콘서트 데이터 동기화 실패: {}", e.getMessage());
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
            log.info("관리자 요청: 뮤지컬 KOPIS 데이터 동기화");

            LocalDate now = LocalDate.now();
            String startDate = now.withDayOfMonth(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String endDate = now.withDayOfMonth(now.lengthOfMonth()).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            log.info("뮤지컬 동기화 날짜 범위: {} ~ {}", startDate, endDate);

            SyncResult result = syncService.syncMusicals(startDate, endDate);
            log.info("뮤지컬 동기화 완료 - 응답 데이터: {}", result);
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

    /**
     * 수동 스케줄 실행 (테스트용)
     */
    @PostMapping("/schedule/trigger")
    public ResponseEntity<Map<String, Object>> triggerSchedule() {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("관리자 요청: 수동 스케줄 실행");

            // 비동기로 실행 (실제 운영에서는 @Async 사용 권장)
            new Thread(() -> {
                schedulerService.manualSync();
            }).start();

            response.put("success", true);
            response.put("message", "스케줄된 동기화가 백그라운드에서 시작되었습니다.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("수동 스케줄 실행 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "스케줄 실행 중 오류가 발생했습니다: " + e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 데이터 규모 조사 - 전체 기간
     */
    @GetMapping("/analyze/data-volume")
    public ResponseEntity<Map<String, Object>> analyzeDataVolume(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 기본값: 올해 1월 1일부터 내년 12월 31일까지
            if (startDate == null) {
                startDate = LocalDate.now().withDayOfYear(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            }
            if (endDate == null) {
                endDate = LocalDate.now().plusYears(1).withDayOfYear(1).minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            }

            log.info("데이터 볼륨 분석 시작: {} ~ {}", startDate, endDate);

            // 페이지별로 데이터 조회하여 전체 규모 파악
            int totalCount = 0;
            int page = 1;
            int pageSize = 500;
            Map<String, Integer> genreStats = new HashMap<>();

            while(true) {
                log.info("페이지 {} 조회중...", page);
                String xmlResponse = syncService.testKopisApiCall(startDate, endDate, page, pageSize);
                log.info("=== KOPIS XML 응답 전체 ===");
                log.info("{}", xmlResponse);
                log.info("=== 응답 끝 ===");
                List<KopisPerformanceDto> performances = syncService.parseXmlToPerformanceList(xmlResponse);

                log.info("페이지 {}: {}개 데이터 수신", page, performances.size()); // 디버깅 로그

                if(performances.isEmpty()) {
                    log.info("빈 응답으로 인한 루프 종료");
                    break;
                }

                totalCount += performances.size();

                // 장르별 통계
                for(KopisPerformanceDto perf : performances) {
                    String genre = perf.getGenrenm() != null ? perf.getGenrenm() : "null";
                    genreStats.put(genre, genreStats.getOrDefault(genre, 0) + 1);
                }

                // 마지막 페이지 확인
                if(performances.size() < pageSize) {
                    log.info("마지막 페이지 도달: {}개 < {}", performances.size(), pageSize);
                    break;
                }
                page++;

                // 너무 많은 페이지 방지 (최대 20페이지 = 10,000건)
                if(page > 20) {
                    response.put("warning", "20페이지(10,000건) 초과로 조사 중단");
                    break;
                }
            }

            response.put("success", true);
            response.put("period", Map.of("startDate", startDate, "endDate", endDate));
            response.put("totalPages", page);
            response.put("totalPerformances", totalCount);
            response.put("genreStatistics", genreStats);
            response.put("estimatedMemoryUsage", totalCount * 2 + "KB"); // 대략적인 메모리 사용량
            response.put("recommendSpringBatch", totalCount > 5000); // 5000건 이상이면 Spring Batch 권장

            return ResponseEntity.ok(response);
        } catch(Exception e) {
            log.error("데이터 볼륨 분석 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "분석 중 오류 발생: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 월별 데이터 분석
     */
    @GetMapping("/analyze/monthly-volume")
    public ResponseEntity<Map<String, Object>> analyzeMonthlyVolume(@RequestParam int year) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Integer> monthlyStats = new HashMap<>();

        try {
            // 연도 전체를 한 번에 조회
            String startDate = year + "0101";
            String endDate = year + "1231";

            // 페이지네이션으로 전체 데이터 수집
            List<KopisPerformanceDto> allPerformances = new ArrayList<>();
            int page = 1;
            int pageSize = 500;

            while(true) {
                String xmlResponse = syncService.testKopisApiCall(startDate, endDate, page, pageSize);
                List<KopisPerformanceDto> performances = syncService.parseXmlToPerformanceList(xmlResponse);

                if(performances.isEmpty()) break;
                allPerformances.addAll(performances);
                if(performances.size() < pageSize) break;
                page++;

                if(page > 20) break;
            }

            // 메모리에서 월별 분류
            for(int month = 1; month <= 12; month++) {
                monthlyStats.put(month + "월", 0);
            }

            for(KopisPerformanceDto perf : allPerformances) {
                try {
                    // 공연시작일에서 월 추출
                    String startDateStr = perf.getPrfpdfrom();
                    if(startDateStr != null && startDateStr.length() >= 6) {
                        int perfMonth = Integer.parseInt(startDateStr.substring(4, 6));
                        String monthKey = perfMonth + "월";
                        monthlyStats.put(monthKey, monthlyStats.get(monthKey) + 1);
                    }
                } catch(Exception e) {
                    log.warn("날짜 파싱 실패: {}", perf.getPrfpdfrom(), e);
                }
            }

            response.put("success", true);
            response.put("year", year);
            response.put("monthlyStatistics", monthlyStats);
            response.put("totalYearlyEstimate", monthlyStats.values().stream().mapToInt(Integer::intValue).sum());

            return ResponseEntity.ok(response);
        } catch(Exception e) {
            log.error("월별 분석 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "월별 분석 중 오류 발생: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

//    @GetMapping("/analyze/db-volume")
//    public ResponseEntity<Map<String, Object>> analyzeDbVolume(
//            @RequestParam(required = false) String startDate,
//            @RequestParam(required = false) String endDate) {
//
//        // DB에서 직접 조회
//        List<Concert> concerts = concertRepository.findByDateRange(startDate, endDate);
//        List<Musical> musicals = musicalRepository.findByDateRange(startDate, endDate);
//
//        // 통계 생성
//        Map<String, Object> response = new HashMap<>();
//        response.put("totalConcerts", concerts.size());
//        response.put("totalMusicals", musicals.size());
//        response.put("totalPerformances", concerts.size() + musicals.size());
//
//        return ResponseEntity.ok(response);
//    }

    @GetMapping("/analyze/db-volume")
    public ResponseEntity<Map<String, Object>> analyzeDbVolume(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("DB 볼륨 분석 시작 - startDate: {}, endDate: {}", startDate, endDate);
            // 기본값 설정
            if (startDate == null) {
                startDate = LocalDate.now().withDayOfYear(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            }
            if (endDate == null) {
                endDate = LocalDate.now().plusYears(1).withDayOfYear(1).minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            }

            log.info("처리할 날짜 범위: {} ~ {}", startDate, endDate);

            // String을 LocalDate로 변환
            log.info("날짜 파싱 시작");
            LocalDate startLocalDate = LocalDate.parse(startDate, DateTimeFormatter.ofPattern("yyyyMMdd"));
            LocalDate endLocalDate = LocalDate.parse(endDate, DateTimeFormatter.ofPattern("yyyyMMdd"));
            log.info("날짜 파싱 완료: {} ~ {}", startLocalDate, endLocalDate);

            // Repository 호출
            log.info("콘서트 데이터 조회 시작");
            List<Concert> concerts = concertRepository.findByStartDateBetween(startLocalDate, endLocalDate);
            log.info("콘서트 데이터 조회 완료: {}건", concerts.size());

            log.info("뮤지컬 데이터 조회 시작");
            List<Musical> musicals = musicalRepository.findByStartDateBetween(startLocalDate, endLocalDate);
            log.info("뮤지컬 데이터 조회 완료: {}건", musicals.size());

            // 장르별 통계
            Map<String, Integer> genreStats = new HashMap<>();
            genreStats.put("콘서트", concerts.size());
            genreStats.put("뮤지컬", musicals.size());

            response.put("success", true);
            response.put("dataSource", "DATABASE");
            response.put("period", Map.of("startDate", startDate, "endDate", endDate));
            response.put("totalConcerts", concerts.size());
            response.put("totalMusicals", musicals.size());
            response.put("totalPerformances", concerts.size() + musicals.size());
            response.put("genreStatistics", genreStats);
            response.put("recommendSpringBatch", (concerts.size() + musicals.size()) > 5000);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("DB 볼륨 분석 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "DB 분석 중 오류 발생: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
