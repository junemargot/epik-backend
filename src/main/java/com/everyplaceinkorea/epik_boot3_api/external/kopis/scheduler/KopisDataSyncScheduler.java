package com.everyplaceinkorea.epik_boot3_api.external.kopis.scheduler;

import com.everyplaceinkorea.epik_boot3_api.external.kopis.config.SchedulerProperties;
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
public class KopisDataSyncScheduler {
    
    private final KopisDataSyncService syncService;

    @Scheduled(cron = "${kopis.sync.cron:0 0 2 * * ?}")
    public void scheduledDataSync() {
        log.info("=== KOPIS 데이터 정기 동기화 시작 ===");
        
        LocalDate now = LocalDate.now();
        String startDate = now.withDayOfMonth(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String endDate = now.withDayOfMonth(now.lengthOfMonth()).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        try {
            syncService.syncAllConcerts(startDate, endDate);
            syncService.syncMusicals(startDate, endDate);
            
            log.info("=== KOPIS 데이터 정기 동기화 완료 ===");

        } catch (Exception e) {
            log.error("KOPIS 데이터 동기화 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
