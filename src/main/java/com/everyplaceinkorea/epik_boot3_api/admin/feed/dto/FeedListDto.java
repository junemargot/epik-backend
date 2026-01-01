package com.everyplaceinkorea.epik_boot3_api.admin.feed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedListDto {
    private List<FeedResponseDto> feedList;
    private long totalCount;
    private int totalPages;
    private Boolean hasNext;
    private Boolean hasPrev;
    private List<Long> pages;
}
