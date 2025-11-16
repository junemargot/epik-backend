package com.everyplaceinkorea.epik_boot3_api.admin.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RegionStatsDto {
    private String regionName;
    private Long count;
}
