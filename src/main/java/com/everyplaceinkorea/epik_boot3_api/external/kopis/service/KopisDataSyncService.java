package com.everyplaceinkorea.epik_boot3_api.external.kopis.service;

import com.everyplaceinkorea.epik_boot3_api.entity.Facility;
import com.everyplaceinkorea.epik_boot3_api.entity.Hall;
import com.everyplaceinkorea.epik_boot3_api.entity.Region;
import com.everyplaceinkorea.epik_boot3_api.entity.concert.Concert;
import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import com.everyplaceinkorea.epik_boot3_api.entity.musical.Musical;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.KopisApiService;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.dto.KopisPerformanceDto;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.dto.MigrationResult;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.dto.SyncResult;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.utils.KopisGenreUtil;
import com.everyplaceinkorea.epik_boot3_api.repository.HallRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.Member.MemberRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.RegionRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.concert.ConcertRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.musical.MusicalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.TimeoutException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class KopisDataSyncService {

    private final KopisApiService kopisApiService;
    private final FacilityService facilityService;
    private final ConcertRepository concertRepository;
    private final MusicalRepository musicalRepository;
    private final RegionRepository regionRepository;
    private final MemberRepository memberRepository;
    private final HallRepository hallRepository;

    public SyncResult syncConcerts() {
        LocalDate now = LocalDate.now();
        String startDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String endDate = now.plusMonths(6).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return syncConcerts(startDate, endDate);
    }

    /**
     * 전체 Concert 동기화: 장르별 개별 조회
     */
    public SyncResult syncConcerts(String startDate, String endDate) {
        log.info("=== Concert 동기화 시작: {} ~ {} ===", startDate, endDate);
        SyncResult result = new SyncResult("CONCERT");

        try {
            Member systemMember = getSystemMember();
            Region defaultRegion = getDefaultRegion();
            String[] concertCodes = KopisGenreUtil.ConcertGenre.getAllCodes();
            Map<String, Integer> genreStats = new HashMap<>();
            log.info("콘서트 동기화 대상 장르: {}개", concertCodes.length);

            // 장르별 조회 및 처리
            for (String genreCode : concertCodes) {
                String genreName = KopisGenreUtil.getConcertGenreName(genreCode);
                log.info("콘서트 장르 {} 조회 시작 - {}", genreCode, genreName);
                int beforeCount = result.getSuccessCount(); // 해당 장르 처리 전 성공 건수

                try {
                    syncByGenrePaginated(
                            genreCode, startDate, endDate, systemMember, defaultRegion, result, "CONCERT");

                } catch (Exception e) {
                    log.error("콘서트 장르 {} 동기화 실패: {}", genreCode, e.getMessage(), e);
                    result.addFailure(String.format("콘서트 장르 %s (%s) 동기화 실패: %s",
                            genreCode, genreName, e.getMessage()));
                }

                int afterCount = result.getSuccessCount();
                int genreProcessed = afterCount - beforeCount;
                genreStats.put(genreCode, genreProcessed);
                log.info("장르 '{}' 처리 완료: {}건", genreName, genreProcessed);
            }

            result.complete();

            log.info("[CONCERT] 장르별 동기화 통계:");
            genreStats.forEach((genreName, count) -> {
                log.info(" {}: {}건", genreName, count);
            });

            log.info("[CONCERT] 전체 동기화 완료 통계");
            log.info("   총 처리: {}건", result.getTotalProcessed());
            log.info("   성공: {}건", result.getSuccessCount());
            log.info("   실패: {}건", result.getFailureCount());
            log.info("   성공률: {:.1f}%", result.getSuccessRate());
            log.info("   소요시간: {:.2f}초", result.getDurationMs() / 1000.0);

            return result;

        } catch (Exception e) {
            log.error("Concert 동기화 전체 실패", e);
            result.addFailure("전체 동기화 실패: " + e.getMessage());
            result.complete();
            return result;
        }
    }

    public SyncResult syncMusicals() {
        LocalDate now = LocalDate.now();
        String startDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String endDate = now.plusMonths(6).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return syncMusicals(startDate, endDate);
    }

    /**
     * 전체 Musical 동기화
     */
    public SyncResult syncMusicals(String startDate, String endDate) {
        log.info("=== Musical 동기화 시작: {} ~ {} ===", startDate, endDate);
        SyncResult result = new SyncResult("MUSICAL");

        try {
            Member systemMember = getSystemMember();
            Region defaultRegion = getDefaultRegion();

            String genreCode = KopisGenreUtil.MusicalGenre.MUSICAL.getCode();
            String genreName = KopisGenreUtil.getMusicalGenreName(genreCode);
            log.info("뮤지컬 장르 {} 조회 시작 - {}", genreCode, genreName);

            try {
                syncByGenrePaginated(
                        genreCode, startDate, endDate, systemMember, defaultRegion, result, "MUSICAL");

            } catch (Exception e) {
                log.error("뮤지컬 장르 {} 동기화 실패: {}", genreCode, e.getMessage(), e);
                result.addFailure(String.format("뮤지컬 장르 %s 동기화 실패: %s", genreCode, e.getMessage()));
            }

            result.complete();
            log.info("[MUSICAL] 전체 동기화 완료 통계");
            log.info("   총 처리: {}건", result.getTotalProcessed());
            log.info("   성공: {}건", result.getSuccessCount());
            log.info("   실패: {}건", result.getFailureCount());
            log.info("   성공률: {:.1f}%", result.getSuccessRate());
            log.info("   소요시간: {:.2f}초", result.getDurationMs() / 1000.0);

            return result;

        } catch (Exception e) {
            log.error("Musical 동기화 전체 실패", e);
            result.addFailure("전체 동기화 실패: " + e.getMessage());
            result.complete();
            return result;
        }
    }

    private void syncByGenrePaginated(
            String genreCode, String startDate, String endDate,
            Member systemMember, Region defaultRegion, SyncResult result, String syncType) {

        int pageNum = 1;
        boolean hasMoreData = true;
        int totalProcessedForGenre = 0;
        int skippedCount = 0;

        while(hasMoreData) {
            try {
                log.debug("{} 장르 {} - {}페이지 조회", syncType, genreCode, pageNum);

                // 1차 API 호출(기본정보)
                String xmlResponse = kopisApiService.getPerformanceListByGenre(startDate, endDate, pageNum, 100, genreCode);
                if(xmlResponse != null) {
                    // 응답데이터(xml) 파싱
                    List<KopisPerformanceDto> performances = parseXmlToPerformanceList(xmlResponse);
                    String genreName = "CONCERT".equals(syncType) ?
                            KopisGenreUtil.getConcertGenreName(genreCode) :
                            KopisGenreUtil.getMusicalGenreName(genreCode);

                    log.info(" [{}] 장르 '{}' - {}페이지: API 요청 100개, 실제 응답 {}개",
                            syncType, genreName, pageNum, performances.size());

                    log.debug("{} 장르 {} - {}페이지: {}개 데이터 수신",
                            syncType, genreCode, pageNum, performances.size());

                    if(performances.isEmpty()) {
                        hasMoreData = false;
                    } else {
                        for (KopisPerformanceDto performance : performances) {
                            try {
                                // 증분 동기화 체크 (추가) - 이미 DB에 있으면 스킵
                                if(shouldSkipSync(performance.getMt20id(), syncType)) {
                                    skippedCount++;
                                    log.debug("이미 동기화됨, 스킵: {} ({})", performance.getPrfnm(), performance.getMt20id());
                                    continue;
                                }

                                // 신규 데이터만 처리 (shouldSkipSync에서 이미 기존 데이터 필터링됨)
                                if("CONCERT".equals(syncType)) {
                                    syncSingleConcertNew(performance, systemMember, defaultRegion, result);
                                } else if("MUSICAL".equals(syncType)) {
                                    syncSingleMusicalNew(performance, systemMember, defaultRegion, result);
                                }
                                totalProcessedForGenre++;

                            } catch (Exception e) {
                                log.error("개별 {} 처리 실패: ID={}, 오류={}",
                                        syncType, performance.getMt20id(), e.getMessage());

                                result.addFailure(String.format("%s ID %s 동기화 실패: %s",
                                        syncType, performance.getMt20id(), e.getMessage()));
                            }
                        }

                        if(performances.size() < 100) {
                            hasMoreData = false;
                        }
                        pageNum++;
                    }
                } else {
                    log.warn("{} 장르 {} - {}페이지: 응답 데이터 없음", syncType, genreCode, pageNum);
                    hasMoreData = false;
                }

                if(hasMoreData) {
                    Thread.sleep(500);
                }
            } catch (Exception e) {
                log.error("{} 장르 {} - {}페이지 조회 실패: {}", syncType, genreCode, pageNum, e.getMessage());
                hasMoreData = false;
            }
        }
    }

    // 동기화 스킵 여부 판단 - DB에 존재하면 스킵
    private boolean shouldSkipSync(String kopisId, String syncType) {
        try {
            if("CONCERT".equals(syncType)) {
                return concertRepository.existsByKopisId(kopisId);
            } else if("MUSICAL".equals(syncType)) {
                return musicalRepository.existsByKopisId(kopisId);
            }
            return false;

        } catch (Exception e) {
            log.warn("동기화 스킵 체크 실패: {}, 동기화 진행", kopisId);
            return false; // 오류 시 동기화 진행
        }
    }

    /**
     * 개별 콘서트 동기화
     */
    private void syncSingleConcert(KopisPerformanceDto dto, Member systemMember, Region defaultRegion, SyncResult result) {
        try {
            Optional<Concert> existingConcert = concertRepository.findByKopisId(dto.getMt20id());
            Concert concert;

            if (existingConcert.isPresent()) {
                concert = existingConcert.get();
                concert.updateFromKopisData(dto);

                // 상세 정보 조회 안함, 상세 정보가 비어있으면 보완 => 신규 데이터만 상세정보 업데이트
                if (!hasDetailData(dto)) {
                    log.debug("상세 정보 없음, API 호출하여 조회: {}", concert.getTitle());
                    fetchAndMergeDetailToDto(dto);
                    concert.updateFromKopisDetailData(dto); // 엔티티에 적용
                    log.debug("상세 정보 보완 완료: {}", concert.getTitle());
                }
                // Facility/Hall 동기화
                syncFacilityAndHall(concert, dto);
                result.addSuccess(false);

            } else {
                log.debug("신규 공연, 상세 정보 조회: {}", dto.getPrfnm());
                fetchAndMergeDetailToDto(dto);
                concert = Concert.fromKopisData(dto, defaultRegion, systemMember);
                if (hasDetailData(dto)) {
                    concert.updateFromKopisDetailData(dto);
                }

                result.addSuccess(true);
            }

            // lastSynced 업데이트
            concert.setLastSynced(LocalDateTime.now());
            concertRepository.save(concert);

        } catch (Exception e) {
            log.error("콘서트 동기화 실패: KOPIS_ID={}, 오류={}", dto.getMt20id(), e.getMessage(), e);
            result.addFailure("콘서트 ID " + dto.getMt20id() + " 처리 실패: " + e.getMessage());
        }
    }

    /**
     * 신규 콘서트 동기화 (증분 동기화 전용 - DB 조회 없음)
     * shouldSkipSync()에서 이미 기존 데이터 필터링됨
     */
    private void syncSingleConcertNew(KopisPerformanceDto dto, Member systemMember, Region defaultRegion, SyncResult result) {
        try {
            log.debug("신규 콘서트 공연 생성 시작: {} ({})", dto.getPrfnm(), dto.getMt20id());
            // 상세 정보 추가 조회
            fetchAndMergeDetailToDto(dto);

            // 추가 - 시설ID 검증
            if(dto.getMt10id() == null || dto.getMt10id().trim().isEmpty()) {
                log.error("상세 정보 조회 후에도 시설 ID 없음, 동기화 스킵: {} ({})", dto.getPrfnm(), dto.getMt20id());
                result.addFailure(String.format("mt10id 없음: %s (%s)", dto.getPrfnm(), dto.getMt20id()));
                return;
            }

            // Concert 생성 (무조건 신규)
            Concert concert = Concert.fromKopisData(dto, defaultRegion, systemMember);

            // 상세 정보 적용
            if (hasDetailData(dto)) {
                concert.updateFromKopisDetailData(dto);
            }

            // Facility/Hall 동기화
            syncFacilityAndHall(concert, dto);

            // 동기화 시간 기록
            concert.setLastSynced(LocalDateTime.now());

            // 저장
            concertRepository.save(concert);
            result.addSuccess(true); // 신규생성

            log.debug("신규 공연 생성 완료: {}", concert.getTitle());

        } catch (DataIntegrityViolationException e) {
            log.warn("이미 다른 스레드에서 생성됨, 스킵: {} ({})", dto.getPrfnm(), dto.getMt20id());

        } catch (Exception e) {
            log.error("콘서트 생성 실패: KOPIS_ID={}, 오류={}", dto.getMt20id(), e.getMessage(), e);
            result.addFailure("콘서트 ID " + dto.getMt20id() + " 생성 실패: " + e.getMessage());
        }
    }

    private void fetchAndMergeDetailToDto(KopisPerformanceDto dto) {
        try {
            String detailXml = kopisApiService.getPerformanceDetail(dto.getMt20id());
            if(detailXml != null) {
                List<KopisPerformanceDto> detailList = parseXmlToPerformanceList(detailXml);
                if(!detailList.isEmpty()) {
                    KopisPerformanceDto detailDto = detailList.get(0);
                    mergeDetailInfo(dto, detailDto); // dto에 병합
                }
            }
        } catch (Exception e) {
            log.warn("상세 정보 조회 실패: {}", dto.getMt20id());
        }
    }

    private void mergeDetailInfo(KopisPerformanceDto basicDto, KopisPerformanceDto detailDto) {
        // 완전한 병합 메서드 호출로 교체
        mergeAllDetailInfo(basicDto, detailDto);
    }

    /**
     * 개별 뮤지컬 동기화
     */
    private void syncSingleMusical(KopisPerformanceDto dto, Member systemMember, Region defaultRegion, SyncResult result) {
        try {
            Optional<Musical> existingMusical = musicalRepository.findByKopisId(dto.getMt20id());
            Musical musical;

            if (existingMusical.isPresent()) {
                musical = existingMusical.get();
                musical.updateFromKopisData(dto);

                // 상세 정보가 없으면 API 호출해서 보완
                if(!hasDetailData(dto)) {
                    log.debug("뮤지컬 데이터 - 상세 정보 없음, API 호출하여 조회: {}", musical.getTitle());
                    fetchAndMergeDetailToDto(dto);
                    musical.updateFromKopisDetailData(dto);
                    log.debug("뮤지컬 데이터 - 상세 정보 보완 완료: {}", musical.getTitle());
                }
                // Facility/Hall 동기화
                syncFacilityAndHall(musical, dto);
                result.addSuccess(false);
            } else {
                log.debug("신규 뮤지컬, 상세 정보 조회: {}", dto.getPrfnm());
                fetchAndMergeDetailToDto(dto);
                musical = Musical.fromKopisData(dto, defaultRegion, systemMember);
                if (hasDetailData(dto)) {
                    musical.updateFromKopisDetailData(dto);
                }

                result.addSuccess(true);
            }

            musical.setLastSynced(LocalDateTime.now());
            musicalRepository.save(musical);

        } catch (Exception e) {
            log.error("뮤지컬 동기화 실패: KOPIS_ID={}, 오류={}", dto.getMt20id(), e.getMessage(), e);
            result.addFailure("뮤지컬 ID " + dto.getMt20id() + " 처리 실패: " + e.getMessage());
        }
    }

    /**
     * 신규 뮤지컬 동기화 (증분 동기화 전용 - DB 조회 없음)
     * shouldSkipSync()에서 이미 기존 데이터 필터링됨
     */
    private void syncSingleMusicalNew(KopisPerformanceDto dto, Member systemMember,
                                       Region defaultRegion, SyncResult result) {
        try {
            log.debug("신규 뮤지컬 생성 시작: {} ({})", dto.getPrfnm(), dto.getMt20id());
            fetchAndMergeDetailToDto(dto);

            // 시설ID 검증
            if(dto.getMt10id() == null || dto.getMt10id().trim().isEmpty()) {
                log.error("상세 정보 조회 후에도 시설 ID 없음, 동기화 스킵: {} ({})", dto.getPrfnm(), dto.getMt20id());
                result.addFailure(String.format("mt10id 없음: %s (%s)", dto.getPrfnm(), dto.getMt20id()));
                return;
            }

            // Musical 생성 (무조건 신규)
            Musical musical = Musical.fromKopisData(dto, defaultRegion, systemMember);

            // 상세 정보 적용
            if (hasDetailData(dto)) {
                musical.updateFromKopisDetailData(dto);
            }

            // Facility/Hall 동기화
            syncFacilityAndHall(musical, dto);

            // 동기화 시간 기록
            musical.setLastSynced(LocalDateTime.now());

            // 저장
            musicalRepository.save(musical);
            result.addSuccess(true); // 신규생성

            log.debug("신규 뮤지컬 생성 완료: {}", musical.getTitle());

        } catch (DataIntegrityViolationException e) {
            log.warn("이미 다른 스레드에서 생성됨: {} ({})", dto.getPrfnm(), dto.getMt20id());
            
        } catch (Exception e) {
            log.error("뮤지컬 생성 실패: KOPIS_ID={}, 오류={}", dto.getMt20id(), e.getMessage(), e);
            result.addFailure("뮤지컬 ID " + dto.getMt20id() + " 생성 실패: " + e.getMessage());
        }
    }

    /**
     * 개별 공연 동기화 (외부 호출용)
     */
    @Transactional
    public void syncSinglePerformance(String kopisId) {
        log.info("개별 공연 동기화: {}", kopisId);

        try {
            String xmlResponse = kopisApiService.getPerformanceDetail(kopisId);
            log.info("=== XML 응답 확인 ===");
            log.info("응답 null 여부: {}", xmlResponse == null);
            if (xmlResponse != null) {
                log.info("응답 길이: {}", xmlResponse.length());
                log.info("응답 내용: {}", xmlResponse);
            }

            if (xmlResponse != null) {
                List<KopisPerformanceDto> performances = parseXmlToPerformanceList(xmlResponse);
                log.info("파싱된 공연 수: {}", performances.size());

                if (!performances.isEmpty()) {
                    KopisPerformanceDto performance = performances.get(0);
                    log.info("파싱된 공연 정보:");
                    log.info("- mt20id: {}", performance.getMt20id());
                    log.info("- prfnm: {}", performance.getPrfnm());
                    log.info("- genrenm: {}", performance.getGenrenm());

                    Member systemMember = getSystemMember();
                    Region defaultRegion = getDefaultRegion();
                    SyncResult dummyResult = new SyncResult("SINGLE");

                    if (isConcertGenre(performance.getGenrenm())) {
                        log.info("콘서트 장르로 처리");
                        syncSingleConcert(performance, systemMember, defaultRegion, dummyResult);
                    } else if (isMusicalGenre(performance.getGenrenm())) {
                        log.info("뮤지컬 장르로 처리");
                        syncSingleMusical(performance, systemMember, defaultRegion, dummyResult);
                    } else {
                        log.warn("알 수 없는 장르: {} - {}", kopisId, performance.getGenrenm());
                    }
                } else {
                    log.warn("파싱된 공연 데이터가 없습니다");
                }
            }

        } catch (Exception e) {
            log.error("개별 공연 동기화 실패: {}", kopisId, e);
            throw new RuntimeException("공연 동기화 실패: " + e.getMessage());
        }
    }

    /**
     * XML 응답 문자열에서 개별 공연 데이터 블록을 추출하여 KopisPerformanceDto 리스트로 파싱
     */
    public List<KopisPerformanceDto> parseXmlToPerformanceList(String xmlResponse) {
        List<KopisPerformanceDto> performances = new ArrayList<>();

        try {
            Pattern dbPattern = Pattern.compile("<db>(.*?)</db>", Pattern.DOTALL);
            Matcher dbMatcher = dbPattern.matcher(xmlResponse);

            while (dbMatcher.find()) {
                String dbContent = dbMatcher.group(1);
                KopisPerformanceDto dto = parsePerformanceFromXml(dbContent);
                if (dto != null) {
                    performances.add(dto);
                }
            }

        } catch (Exception e) {
            log.error("XML 파싱 실패: {}", e.getMessage());
            throw new RuntimeException("KOPIS API 응답 파싱 실패", e);
        }

        return performances;
    }

    /**
     * 개별 공연 XML 블록을 DTO 객체로 변환
     */
    private KopisPerformanceDto parsePerformanceFromXml(String xmlContent) {
        try {
            KopisPerformanceDto dto = new KopisPerformanceDto();
            dto.setMt20id(extractXmlValue(xmlContent, "mt20id"));
            dto.setMt10id(extractXmlValue(xmlContent, "mt10id"));
            dto.setPrfnm(extractXmlValue(xmlContent, "prfnm"));
            dto.setPrfpdfrom(extractXmlValue(xmlContent, "prfpdfrom"));
            dto.setPrfpdto(extractXmlValue(xmlContent, "prfpdto"));
            dto.setFcltynm(extractXmlValue(xmlContent, "fcltynm"));
            dto.setPoster(extractXmlValue(xmlContent, "poster"));
            dto.setArea(extractXmlValue(xmlContent, "area"));
            dto.setGenrenm(extractXmlValue(xmlContent, "genrenm"));
            dto.setPrfstate(extractXmlValue(xmlContent, "prfstate"));
            dto.setPrftime(extractXmlValue(xmlContent, "prftime"));
            dto.setPcseguidance(extractXmlValue(xmlContent, "pcseguidance"));
            dto.setDtguidance(extractXmlValue(xmlContent, "dtguidance"));
            dto.setStyurls(extractXmlValue(xmlContent, "styurls"));
            dto.setPrfruntime(extractXmlValue(xmlContent, "prfruntime"));
            dto.setPrfage(extractXmlValue(xmlContent, "prfage"));

            return dto;

        } catch (Exception e) {
            log.warn("개별 공연 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * XML 문자열에서 특정 태그의 값 추출, CDATA나 특수 태그 처리
     */
    private String extractXmlValue(String xml, String tagName) {
        Pattern pattern = Pattern.compile("<" + tagName + ">(.*?)</" + tagName + ">", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(xml);

        if (matcher.find()) {
            String value = matcher.group(1).trim();

            // CDATA 처리
            if (value.startsWith("<![CDATA[") && value.endsWith("]]>")) {
                value = value.substring(9, value.length() - 3);
            }

            // 중첩된 태그 처리
            if ("styurls".equals(tagName)) {
                return extractStyurls(value);
            }

            // 빈 값이거나 공백만 있는 경우 null 반환
            return value.isEmpty() || value.trim().isEmpty() ? null : value;
        }

        return null;
    }

    /**
     * styurls(소개이미지) 태그 내의 중첩된 여러 styurl 태그 추출
     */
    private String extractStyurls(String styurlsContent) {
        if (styurlsContent == null || styurlsContent.trim().isEmpty()) {
            return null;
        }

        Pattern pattern = Pattern.compile("<styurl>(.*?)</styurl>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(styurlsContent);

        StringBuilder urls = new StringBuilder();
        while (matcher.find()) {
            String url = matcher.group(1).trim();
            if (!url.isEmpty()) {
                if (!urls.isEmpty()) {
                    urls.append(",");
                }
                urls.append(url);
            }
        }

        return !urls.isEmpty() ? urls.toString() : null;
    }

    /**
     * 장르 판별 메서드들
     */
    private boolean isConcertGenre(String genrenm) {
        return KopisGenreUtil.isConcertGenre(genrenm);
    }

    private boolean isMusicalGenre(String genrenm) {
        return KopisGenreUtil.isMusicalGenre(genrenm);
    }

    /**
     * KOPIS API 호출 테스트 (DB 저장 없이)
     */
    public String testKopisApiCall(String startDate, String endDate) {
        log.info("KOPIS API 호출 테스트: {} ~ {}", startDate, endDate);

        try {
            String xmlResponse = kopisApiService.getPerformanceList(startDate, endDate, 1, 10); // 소량만

            if (xmlResponse != null) {
                List<KopisPerformanceDto> performances = parseXmlToPerformanceList(xmlResponse);
                log.info("파싱된 공연 데이터: {}개", performances.size());

                // 첫 번째 데이터 로깅
                if (!performances.isEmpty()) {
                    KopisPerformanceDto first = performances.get(0);
                    log.info("첫 번째 공연: {} - {} ({})", first.getPrfnm(), first.getGenrenm(), first.getMt20id());
                }
            }

            return xmlResponse;
        } catch (Exception e) {
            log.error("KOPIS API 호출 테스트 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 헬퍼 메서드들
     */
    private Member getSystemMember() {
        return memberRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("시스템 계정을 찾을 수 없습니다"));
    }

    private Region getDefaultRegion() {
        return regionRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("기본 지역을 찾을 수 없습니다"));
    }

    /**
     * 완전한 상세 정보 병합 (12개 필드 모두 처리)
     */
    private void mergeAllDetailInfo(KopisPerformanceDto basicDto, KopisPerformanceDto detailDto) {

        if(isValidString(detailDto.getMt10id())) {
            basicDto.setMt10id(detailDto.getMt10id());
        }

        if (isValidString(detailDto.getFcltynm())) {
            basicDto.setFcltynm(detailDto.getFcltynm());
        }

        if (isValidString(detailDto.getPrftime())) {
            basicDto.setPrftime(removeEmojis(detailDto.getPrftime()));
        }
        if (isValidString(detailDto.getPcseguidance())) {
            basicDto.setPcseguidance(removeEmojis(detailDto.getPcseguidance()));
        }

        if (isValidString(detailDto.getDtguidance())) {
            basicDto.setDtguidance(removeEmojis(detailDto.getDtguidance()));
        }
        if (isValidString(detailDto.getStyurls())) {
            basicDto.setStyurls(removeEmojis(detailDto.getStyurls()));
        }
        if (isValidString(detailDto.getPrfruntime())) {
            basicDto.setPrfruntime(removeEmojis(detailDto.getPrfruntime()));
        }
        if (isValidString(detailDto.getPrfage())) {
            basicDto.setPrfage(removeEmojis(detailDto.getPrfage()));
        }

        log.debug("상세 정보 병합 완료 - 총 12개 필드 처리");
    }

    /**
     * 상세 정보 존재 여부 확인
     */
    private boolean hasDetailData(KopisPerformanceDto dto) {
        return isValidString(dto.getPrftime()) ||
                isValidString(dto.getPcseguidance()) ||
                isValidString(dto.getDtguidance()) ||
                isValidString(dto.getPrfruntime()) ||
                isValidString(dto.getStyurls());
    }


    /**
     * 문자열 유효성 검사 및 이모지 제거
     */
    private boolean isValidString(String str) {
        return str != null && !str.trim().isEmpty() && !"null".equals(str);
    }

    /**
     * 이모지 및 4바이트 UTF-8 문자 완전 제거
     */
    private String removeEmojis(String text) {
        if (text == null) return null;

        // 4바이트 UTF-8 문자(이모지) 완전 제거
        String result = text;

        // 방법 1: 4바이트 문자 제거
        result = result.replaceAll("[\\x{10000}-\\x{10FFFF}]", "");

        // 방법 2: 이모지 범위 제거
        result = result.replaceAll("[\\x{1F600}-\\x{1F64F}]", ""); // 감정표현
        result = result.replaceAll("[\\x{1F300}-\\x{1F5FF}]", ""); // 기호&그림문자
        result = result.replaceAll("[\\x{1F680}-\\x{1F6FF}]", ""); // 교통&지도
        result = result.replaceAll("[\\x{1F1E0}-\\x{1F1FF}]", ""); // 국기
        result = result.replaceAll("[\\x{2600}-\\x{26FF}]", "");   // 기타 기호
        result = result.replaceAll("[\\x{2700}-\\x{27BF}]", "");   // 딩뱃

        // 방법 3: 바이트 레벨에서 4바이트 문자 제거
        StringBuilder sb = new StringBuilder();
        for (char c : result.toCharArray()) {
            if (Character.isSurrogate(c)) {
                continue; // 서로게이트 페어(4바이트) 건너뛰기
            }
            sb.append(c);
        }

        return sb.toString().trim();
    }

    /**
     * 공연의 Facility와 Hall 정보 동기화
     */
    private void syncFacilityAndHall(Object performance, KopisPerformanceDto dto) {
        log.info("=== syncFacilityAndHall 시작 ===");
        try {
            // 1. 공연 시설 ID 확인
            String mt10id = dto.getMt10id();
            if(mt10id == null || mt10id.trim().isEmpty()) {
                log.debug("시설 ID 없음, Facility/Hall 동기화 스킵: {}", dto.getPrfnm());
                return;
            }

            // 2. Facility 동기화
            Optional<Facility> facilityOpt = facilityService.syncFacility(mt10id);

            if(facilityOpt.isEmpty()) {
                log.warn("Facility 동기화 실패: {}", mt10id);
                return;
            }

            Facility facility = facilityOpt.get();
            log.info("Facility 조회 성공: ID={}, Name={}", facility.getId(), facility.getName());

            // 4. Concert 또는 Musical에 Facility 설정
            if(performance instanceof Concert) {
                Concert concert = (Concert) performance;
                concert.setFacility(facility);

                // Facility 정확한 주소로 업데이트
                if(facility.getAddress() != null && !facility.getAddress().isEmpty()) {
                    concert.setAddress(facility.getAddress());
                    log.debug("Concert 주소 업데이트: {}", facility.getAddress());
                }

                // 5. Hall 매칭 시도
                Hall hall = matchHallFromVenue(facility, dto.getFcltynm());
                if(hall != null) {
                    concert.setHall(hall);
                    log.debug("Hall 매칭 성공: {} -> {}", dto.getPrfnm(), hall.getName());
                }
            } else if(performance instanceof Musical) {
                Musical musical = (Musical) performance;
                musical.setFacility(facility);

                // Facility의 정확한 주소로 업데이트
                if(facility.getAddress() != null && !facility.getAddress().isEmpty()) {
                    musical.setAddress(facility.getAddress());
                    log.debug("Musical 주소 업데이트: {}", facility.getAddress());
                }

                Hall hall = matchHallFromVenue(facility, dto.getFcltynm());
                if (hall != null) {
                    musical.setHall(hall);
                    log.debug("Hall 매칭 성공: {} → {}", dto.getPrfnm(), hall.getName());
                }
            }
            log.info("Facility/Hall 동기화 완료: {} -> Facility={}", dto.getPrfnm(), facility.getName());

        } catch(Exception e) {
            log.error("Facility/Hall 동기화 실패: {}, 에러: {}", dto.getPrfnm(), e.getMessage());
        }
    }

    /**
     * venue 문자열에서 Hall 매칭
     */
    private Hall matchHallFromVenue(Facility facility, String fcltynm) {
        if(fcltynm == null || fcltynm.trim().isEmpty() || facility == null) {
            return null;
        }

        try {
            // 1. 해당 시설의 모든 Hall 조회
            List<Hall> halls = hallRepository.findByFacility_id(facility.getId());
            if(halls == null || halls.isEmpty()) {
                log.debug("Facility에 Hall이 없음: Facility ID={}", facility.getId());
                return null;
            }
            log.debug("Hall 매칭 시도: fcltynm='{}', Facility Hall 개수={}", fcltynm, halls.size());

            // 2. 각 Hall의 이름이 fcltynm 문자열에 포함되어 있는지 확인
            for(Hall hall : halls) {
                String hallName = hall.getName();
                if(hallName != null && !hallName.trim().isEmpty()) {
                    if(fcltynm.contains(hallName)) {
                        log.info("Hall 매칭 성공: '{}'에서 '{}' 찾음", fcltynm, hallName);
                        return hall;
                    }
                }
            }

        } catch (Exception e) {
            log.warn("Hall 매칭 중 오류: {}", e.getMessage());
        }

        return null;
    }
}