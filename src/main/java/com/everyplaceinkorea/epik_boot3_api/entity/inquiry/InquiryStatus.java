package com.everyplaceinkorea.epik_boot3_api.entity.inquiry;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InquiryStatus {
    PENDING("답변 대기"),
    ANSWERED("답변 완료");

    private final String description;
}
