package com.everyplaceinkorea.epik_boot3_api.external.kopis.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MigrationResult {
  private boolean success;
  private String message;
  private long totalTarget;
  private long successCount;
  private long failureCount;
  private long skipCount;
  private long noDataCount;
  private long remainingCount;
  private long executionTimeMs;
  private double executionTimeSec;
  private String error;
}
