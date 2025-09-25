package com.everyplaceinkorea.epik_boot3_api.external.kopis.scheduler;

import com.everyplaceinkorea.epik_boot3_api.external.kopis.config.SchedulerProperties;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.service.TicketOfficeSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "scraping.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class TicketOfficeScheduler {

  private final TicketOfficeSyncService jsonService;
  private final SchedulerProperties schedulerProperties;

  /**
   * 매일 새벽 2시에 예매처 정보 업데이트
   * - 정기 업데이트
   * - 최근 추가/변경된 공연 위주로 업데이트
   */
  @Scheduled(cron = "0 0 2 * * ?")
  public void scheduledTicketOfficesUpdate() {
    executeTicketOfficeUpdate("예매처 일일 정기 업데이트");
  }

  /**
   * 공통 예매처 업데이트 로직
   */
  private void executeTicketOfficeUpdate(String taskName) {
    log.info("{} 시작 :", taskName);
    long startTime = System.currentTimeMillis();

    try {
      log.info("뮤지컬 예매처 정보 업데이트 시작");
      jsonService.updateAllMusicalTicketOffices();

      log.info("콘서트 예매처 정보 업데이트 시작");
      jsonService.updateAllConcertTicketOffices();

      long executionTime = System.currentTimeMillis() - startTime;
      log.info("{} 완료 - 소요시간: {} ms", taskName, executionTime);

    } catch (Exception e) {
      long executionTime = System.currentTimeMillis() - startTime;
      log.error("{} 실패 - 소요시간: {} ms, 에러: {}", taskName, executionTime, e.getMessage());
    }
  }
}
