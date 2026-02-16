package com.everyplaceinkorea.epik_boot3_api.entity.inquiry;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum InquiryCategory {
    // 회원/이벤트
    MEMBER_INFO("회원/이벤트", "회원정보"),
    EVENT_PARTICIPATION("회원/이벤트", "이벤트내용/참여"),
    ACCOUNT_ISSUES("회원/이벤트", "회원가입/탈퇴"),

    // 서비스/오류/기타
    SYSTEM_ERROR("서비스/오류/기타", "시스템오류/장애"),
    SERVICE_SUGGESTION("서비스/오류/기타", "서비스 제안/개선"),
    INFO_CORRECTION("서비스/오류/기타", "정보수정요청"),
    ETC("서비스/오류/기타", "기타(직접입력)"),

    // 비즈니스 및 광고 문의
    BUSINESS_INQUIRY("비즈니스/광고", "비즈니스 문의"),
    ADVERTISING_INQUIRY("비즈니스/광고", "광고 문의");

    private final String parentCategory; // 1차 분류
    private final String description;    // 2차 분류 설명

    // 1차 카테고리 목록을 미리 뽑아서 보관 (성능 최적화)
    private static final List<String> PARENT_CATEGORIES = Arrays.stream(values())
            .map(InquiryCategory::getParentCategory)
            .distinct()
            .toList();

    // 특정 1차 카테고리에 속하는 2차 카테고리 목록 반환
    public static List<InquiryCategory> getByParentCategory(String parentCategory) {
        return Arrays.stream(values())
                .filter(category -> category.parentCategory.equals(parentCategory))
                .toList();
    }

    // 1차 카테고리 목록 반환 (중복 제거)
    public static List<String> getAllParentCategories() {
        return PARENT_CATEGORIES;
    }

    public static Map<String, List<InquiryCategory>> getCategoryMap() {
        return Arrays.stream(values())
                .collect(Collectors.groupingBy(
                        InquiryCategory::getParentCategory,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }
}
