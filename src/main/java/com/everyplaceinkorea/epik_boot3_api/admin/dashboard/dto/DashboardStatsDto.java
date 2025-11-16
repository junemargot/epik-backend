package com.everyplaceinkorea.epik_boot3_api.admin.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    private Long totalContents;
    private Long ongoingContents;
    private Long todayContents;
    private Long totalConcerts;
    private Long totalMusicals;
    private Long totalExhibitions;
    private Long totalPopups;
    private List<RegionStatsDto> regionStats;
    private List<GenreStatsDto> genreStats;
    private LocalDateTime lastKopisSyncTime;
    private Map<String, Long> ongoingContentsByType;
    private Map<String, Long> todayContentsByType;
}
