package com.everyplaceinkorea.epik_boot3_api.external.kopis.scheduler;

import com.everyplaceinkorea.epik_boot3_api.external.kopis.service.KopisDataSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "kopis.sync.enabled", havingValue = "true", matchIfMissing = true)
public class KopisSchedulerService {
    
    private final KopisDataSyncService syncService;
    
    /**
     * 매일 오전 2시에 KOPIS 데이터 정기 동기화 실행
     */
    @Scheduled(cron = "${kopis.sync.cron:0 0 2 * * ?}")
    public void scheduledDataSync() {
        log.info("=== KOPIS 데이터 정기 동기화 시작 ===");
        
        LocalDate now = LocalDate.now();
        String startDate = now.withDayOfMonth(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String endDate = now.withDayOfMonth(now.lengthOfMonth()).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        try {
            // 콘서트 동기화
            syncService.syncAllConcerts(startDate, endDate);
            
            // 뮤지컬 동기화
            syncService.syncMusicals(startDate, endDate);
            
            log.info("=== KOPIS 데이터 정기 동기화 완료 ===");
        } catch (Exception e) {
            log.error("KOPIS 데이터 동기화 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 수동 동기화 메서드 (관리자용)
     */
    public void manualSync() {
        log.info("=== KOPIS 데이터 수동 동기화 시작 ===");
        scheduledDataSync();
    }
}
