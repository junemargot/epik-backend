package com.everyplaceinkorea.epik_boot3_api.external.kopis.dto;

public enum SyncStatus {
  IN_PROGRESS("진행중"),
  SUCCESS("성공"),
  PARTIAL_SUCCESS("부분 성공"),
  FAILURE("실패"),
  CANCELLED("취소됨");

  private final String description;

  SyncStatus(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
