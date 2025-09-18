package com.everyplaceinkorea.epik_boot3_api.external.kopis.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class SyncResult {

  private String syncType;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private Long durationMs;

  private int totalProcessed;
  private int successCount;
  private int failureCount;
  private int skippedCount;
  private int newItemCount;
  private int updatedItemCount;
  private int skippedItemCount;

  private SyncStatus status;
  private List<String> errorMessages;
  private List<String> warningMessages;

  public SyncResult(String syncType) {
    this.syncType = syncType;
    this.startTime = LocalDateTime.now();
    this.status = SyncStatus.IN_PROGRESS;
    this.errorMessages = new ArrayList<>();
    this.warningMessages = new ArrayList<>();
  }

  /**
   * 성공한 항목 추가
   */
  public void addSuccess(boolean isNew) {
    this.successCount++;
    this.totalProcessed++;
    if (isNew) {
      this.newItemCount++;
    } else {
      this.updatedItemCount++;
    }
  }

  /**
   * 실패한 항목 추가
   */
  public void addFailure(String errorMessage) {
    this.failureCount++;
    this.totalProcessed++;
    this.errorMessages.add(errorMessage);
  }

  /**
   * 건너뛴 항목 추가
   */
  public void addSkipped(String reason) {
    this.skippedItemCount++;
    this.skippedCount++;
    this.totalProcessed++;
    if (reason != null) {
      this.warningMessages.add("건너뜀: " + reason);
    }
  }

  /**
   * 동기화 완료 처리
   */
  public void complete() {
    this.endTime = LocalDateTime.now();
    this.durationMs = java.time.Duration.between(startTime, endTime).toMillis();

    if (failureCount == 0) {
      this.status = SyncStatus.SUCCESS;
    } else if (successCount > 0) {
      this.status = SyncStatus.PARTIAL_SUCCESS;
    } else {
      this.status = SyncStatus.FAILURE;
    }
  }

  /**
   * 성공률 계산
   */
  public double getSuccessRate() {
    if (totalProcessed == 0) return 0.0;
    return (double) successCount / totalProcessed * 100.0;
  }

  /**
   * 요약 메시지 생성
   */
  public String getSummary() {
    return String.format("%s 동기화 완료: 성공 %d개, 실패 %d개, 소요시간 %.2f초",
            syncType, successCount, failureCount, durationMs / 1000.0);
  }
}
