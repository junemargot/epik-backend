package com.everyplaceinkorea.epik_boot3_api.admin.dashboard.controller;

import com.everyplaceinkorea.epik_boot3_api.admin.dashboard.dto.DashboardStatsDto;
import com.everyplaceinkorea.epik_boot3_api.admin.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    
    private final DashboardService dashboardService;
    
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        log.info("대시보드 통계 API 호출됨");
        
        DashboardStatsDto stats = dashboardService.getDashboardStats();
        
        log.info("대시보드 통계 API 응답 완료 - 전체: {}, 진행중: {}, 오늘등록: {}",
                stats.getTotalContents(),
                stats.getOngoingContents(),
                stats.getTodayContents());
        
        return ResponseEntity.ok(stats);
    }
}
