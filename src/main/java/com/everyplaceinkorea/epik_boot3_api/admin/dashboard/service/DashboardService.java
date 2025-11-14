package com.everyplaceinkorea.epik_boot3_api.admin.dashboard.service;

import com.everyplaceinkorea.epik_boot3_api.admin.dashboard.dto.DashboardStatsDto;
import com.everyplaceinkorea.epik_boot3_api.admin.dashboard.dto.GenreStatsDto;
import com.everyplaceinkorea.epik_boot3_api.admin.dashboard.dto.RegionStatsDto;
import com.everyplaceinkorea.epik_boot3_api.entity.musical.Status;
import com.everyplaceinkorea.epik_boot3_api.repository.concert.ConcertRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.exhibition.ExhibitionRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.musical.MusicalRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.popup.PopupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {
    
    private final ConcertRepository concertRepository;
    private final MusicalRepository musicalRepository;
    private final ExhibitionRepository exhibitionRepository;
    private final PopupRepository popupRepository;

    /**
     * 대시보드 통계 데이터를 조회하고 DTO로 반환
     * @return 대시보드에 표시할 모든 통계 데이터
     */
    public DashboardStatsDto getDashboardStats() {
        log.info("대시보드 통계 조회 시작");
        
        LocalDate today = LocalDate.now();
        
        ContentCounts contentCounts = getTotalContentCounts();
        long ongoingContents = getOngoingContentCounts(today);
        Map<String, Long> onGoingContentsByType = getOngoingContentsByType(today);
        long todayContents = getTodayContentCounts();
        List<RegionStatsDto> regionStats = getRegionStats();
        List<GenreStatsDto> genreStats = getGenreStats();
        LocalDateTime lastSyncTime = getLastKopisSyncTime();
        
        log.info("대시보드 통계 조회 완료 - 전체: {}, 진행중: {}, 오늘등록: {}",
                contentCounts.total, ongoingContents, todayContents);
        
        return DashboardStatsDto.builder()
                .totalContents(contentCounts.total)
                .ongoingContents(ongoingContents)
                .ongoingContentsByType(onGoingContentsByType)
                .todayContents(todayContents)
                .totalConcerts(contentCounts.concerts)
                .totalMusicals(contentCounts.musicals)
                .totalExhibitions(contentCounts.exhibitions)
                .totalPopups(contentCounts.popups)
                .regionStats(regionStats)
                .genreStats(genreStats)
                .lastKopisSyncTime(lastSyncTime)
                .build();
    }

    /**
     * 전체 콘텐츠 수 조회
     * @return ContentCounts 객체 (각 타입별 + 전체 콘텐츠 수)
     */
    private ContentCounts getTotalContentCounts() {
        long concerts = concertRepository.countByStatus(Status.ACTIVE);
        long musicals = musicalRepository.countByStatus(Status.ACTIVE);
        long exhibitions = exhibitionRepository.countByStatus(Status.ACTIVE);
        long popups = popupRepository.countByStatus(
            com.everyplaceinkorea.epik_boot3_api.admin.contents.popup.enums.Status.ACTIVE
        );
        
        long total = concerts + musicals + exhibitions + popups;
        
        log.debug("콘텐츠 통계 - 전체: {}, 팝업: {}, 콘서트: {}, 뮤지컬: {}, 전시: {}",
                total, popups, concerts, musicals, exhibitions);
        
        return new ContentCounts(total, concerts, musicals, exhibitions, popups);
    }

    /**
     * 진행 중인 콘텐츠 수 조회
     * @param today 오늘 날짜
     * @return 현재 진행 중인 콘텐츠 총 수
     */
    private long getOngoingContentCounts(LocalDate today) {
        long ongoingConcerts = concertRepository
            .countByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                Status.ACTIVE, today, today
            );
        
        long ongoingMusicals = musicalRepository
            .countByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                Status.ACTIVE, today, today
            );
        
        long ongoingExhibitions = exhibitionRepository
            .countByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                Status.ACTIVE, today, today
            );
        
        long ongoingPopups = popupRepository
            .countByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                com.everyplaceinkorea.epik_boot3_api.admin.contents.popup.enums.Status.ACTIVE, 
                today, today
            );
        
        long total = ongoingConcerts + ongoingMusicals + ongoingExhibitions + ongoingPopups;
        
        log.debug("진행 중인 콘텐츠 - 총 {}건", total);
        
        return total;
    }

    private Map<String, Long> getOngoingContentsByType(LocalDate today) {
        Map<String, Long> result = new HashMap<>();

        long onGoingConcerts = concertRepository
                .countByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        Status.ACTIVE, today, today
                );

        long ongoingMusicals = musicalRepository
                .countByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        Status.ACTIVE, today, today
                );

        long ongoingExhibitions = exhibitionRepository
                .countByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        Status.ACTIVE, today, today
                );

        long ongoingPopups = popupRepository
                .countByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        com.everyplaceinkorea.epik_boot3_api.admin.contents.popup.enums.Status.ACTIVE,
                        today, today
                );

        result.put("concerts", onGoingConcerts);
        result.put("musicals", ongoingMusicals);
        result.put("exhibitions", ongoingExhibitions);
        result.put("popups", ongoingPopups);

        return result;
    }

    /**
     * 오늘 등록된 콘텐츠 수 조회
     * @return 오늘 등록된 콘텐츠 총 수
     */
    private long getTodayContentCounts() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59, 999999999);
        
        long todayConcerts = concertRepository.countByWriteDateBetween(startOfDay, endOfDay);
        long todayMusicals = musicalRepository.countByWriteDateBetween(startOfDay, endOfDay);
        long todayExhibitions = exhibitionRepository.countByWriteDateBetween(startOfDay, endOfDay);
        long todayPopups = popupRepository.countByWriteDateBetween(startOfDay, endOfDay);
        
        long total = todayConcerts + todayMusicals + todayExhibitions + todayPopups;
        
        log.debug("오늘 등록된 콘텐츠 - 총 {}건", total);
        
        return total;
    }

    /**
     * 지역별 콘텐츠 분포 통계 조회
     * Popup은 제외 (성수, 강남 등 서울 내 특정 지역에 집중됨)
     * Concert, Musical, Exhibition만 집계
     * @return 지역별 통계 리스트 (카운트 내림차순)
     */
    private List<RegionStatsDto> getRegionStats() {
        Map<String, Long> regionMap = new HashMap<>();
        
        List<Object[]> concertRegions = concertRepository.countByRegionGrouped(Status.ACTIVE);
        for (Object[] row : concertRegions) {
            String regionName = (String) row[0];
            Long count = (Long) row[1];
            regionMap.merge(regionName, count, Long::sum);
        }
        
        List<Object[]> musicalRegions = musicalRepository.countByRegionGrouped(Status.ACTIVE);
        for (Object[] row : musicalRegions) {
            String regionName = (String) row[0];
            Long count = (Long) row[1];
            regionMap.merge(regionName, count, Long::sum);
        }
        
        List<Object[]> exhibitionRegions = exhibitionRepository.countByRegionGrouped(Status.ACTIVE);
        for (Object[] row : exhibitionRegions) {
            String regionName = (String) row[0];
            Long count = (Long) row[1];
            regionMap.merge(regionName, count, Long::sum);
        }
        
//        List<Object[]> popupRegions = popupRepository.countByRegionGrouped(
//            com.everyplaceinkorea.epik_boot3_api.admin.contents.popup.enums.Status.ACTIVE
//        );
//        for (Object[] row : popupRegions) {
//            String regionName = (String) row[0];
//            Long count = (Long) row[1];
//            regionMap.merge(regionName, count, Long::sum);
//        }
        
        List<RegionStatsDto> regionStats = regionMap.entrySet().stream()
            .map(entry -> new RegionStatsDto(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing(RegionStatsDto::getCount).reversed())
            .collect(Collectors.toList());
        
        log.debug("지역별 통계 - {}개 지역", regionStats.size());
        
        return regionStats;
    }

    /**
     * 장르별 콘텐츠 분포 통계 조회
     * 장르 정보는 Concert, Musical만 가지고 있음 (KOPIS API에서 제공)
     * Exhibition, Popup은 장르 개념이 없으므로 제회
     * @return 장르별 통계 리스트 (카운트 내림차순)
     */
    private List<GenreStatsDto> getGenreStats() {
        Map<String, Long> genreMap = new HashMap<>();
        
        List<Object[]> concertGenres = concertRepository.countByGenreGrouped(Status.ACTIVE);
        for (Object[] row : concertGenres) {
            String genre = (String) row[0];
            Long count = (Long) row[1];
            
            if (genre != null && !genre.trim().isEmpty()) {
                genreMap.put(genre, count);
            }
        }
        
        long musicalCount = musicalRepository.countByStatus(Status.ACTIVE);
        if (musicalCount > 0) {
            genreMap.put("뮤지컬", musicalCount);
        }
        
        List<GenreStatsDto> genreStats = genreMap.entrySet().stream()
            .map(entry -> new GenreStatsDto(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing(GenreStatsDto::getCount).reversed())
            .collect(Collectors.toList());
        
        log.debug("장르별 통계 - {}개 항목", genreStats.size());
        
        return genreStats;
    }

    /**
     * 최근 KOPIS 동기화 시간 조회
     * Concert, Musical 중에서 가장 최근 동기화된 시간
     * KOPIS API로 동기화한 콘텐츠만 체크 (수동 등록은 제외)
     * @return 가장 최근 동기화 시간 (없으면 null)
     */
    private LocalDateTime getLastKopisSyncTime() {
        Optional<LocalDateTime> lastConcertSync = concertRepository.findMaxLastSynced();
        Optional<LocalDateTime> lastMusicalSync = musicalRepository.findMaxLastSynced();
        
        if (lastConcertSync.isEmpty() && lastMusicalSync.isEmpty()) {
            log.debug("KOPIS 동기화 기록 없음");
            return null;
        } else if (lastConcertSync.isEmpty()) {
            log.debug("KOPIS 최근 동기화: {} (Musical만)", lastMusicalSync.get());
            return lastMusicalSync.get();
        } else if (lastMusicalSync.isEmpty()) {
            log.debug("KOPIS 최근 동기화: {} (Concert만)", lastConcertSync.get());
            return lastConcertSync.get();
        } else {
            LocalDateTime concertTime = lastConcertSync.get();
            LocalDateTime musicalTime = lastMusicalSync.get();
            LocalDateTime latest = concertTime.isAfter(musicalTime) ? concertTime : musicalTime;
            
            log.debug("KOPIS 최근 동기화: {}", latest);
            return latest;
        }
    }

    /**
     * 내부 클래스: 콘텐츠 타입별 카운트를 담는 컨테이너
     */
    private static class ContentCounts {
        final long total;
        final long concerts;
        final long musicals;
        final long exhibitions;
        final long popups;
        
        ContentCounts(long total, long concerts, long musicals, long exhibitions, long popups) {
            this.total = total;
            this.concerts = concerts;
            this.musicals = musicals;
            this.exhibitions = exhibitions;
            this.popups = popups;
        }
    }
}
