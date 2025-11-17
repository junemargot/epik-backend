package com.everyplaceinkorea.epik_boot3_api.entity.feed;

/**
 * 피드의 상태를 나타내는 Enum
 *
 * - ACTIVE: 정상 표시 (기본값)
 * - DELETED: 사용자가 삭제한 피드
 * - REPORTED: 신고 접수된 피드
 * - HIDDEN: 관리자가 숨긴 피드 (신고 처리)
 */
public enum FeedStatus {
    ACTIVE("정상"),
    DELETED("삭제됨"),
    REPORTED("신고됨"),
    HIDDEN("숨김");

    private final String description;

    FeedStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

}
