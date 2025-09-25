package com.everyplaceinkorea.epik_boot3_api.external.kopis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "scraping.scheduler")
public class SchedulerProperties {

  // 스케줄링 활성화 여부
  private boolean enabled = true;

  // 업데이트 실행 시간 (cron 표현식) - 기본값: 매일 새벽 2시
  private String cronExpression = "0 0 2 * * ?";

  // 업데이트 간 딜레이
  private long delayBetweenCatches = 5000;

  // 배치 사이즈 (한 번에 처리할 수)
  private int batchSize = 50;
}
