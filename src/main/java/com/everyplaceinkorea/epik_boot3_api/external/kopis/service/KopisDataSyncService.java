package com.everyplaceinkorea.epik_boot3_api.external.kopis.service;

import com.everyplaceinkorea.epik_boot3_api.entity.Region;
import com.everyplaceinkorea.epik_boot3_api.entity.concert.Concert;
import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import com.everyplaceinkorea.epik_boot3_api.entity.musical.Musical;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.KopisApiService;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.dto.KopisPerformanceDto;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.dto.SyncResult;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.utils.KopisGenreUtil;
import com.everyplaceinkorea.epik_boot3_api.repository.Member.MemberRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.RegionRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.concert.ConcertRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.musical.MusicalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class KopisDataSyncService {

    private final KopisApiService kopisApiService;
    private final ConcertRepository concertRepository;
    private final MusicalRepository musicalRepository;
    private final RegionRepository regionRepository;
    private final MemberRepository memberRepository;

    /**
     * 전체 Concert 동기화: 장르별 개별 조회
     */
    public SyncResult syncConcerts(String startDate, String endDate) {
        log.info("Concert 동기화 시작: {} ~ {}", startDate, endDate);
        SyncResult result = new SyncResult("CONCERT");

        try {
            Member systemMember = getSystemMember();
            Region defaultRegion = getDefaultRegion();

            String[] concertCodes = KopisGenreUtil.ConcertGenre.getAllCodes();
            // 장르별 조회 및 처리
            for (String genreCode : concertCodes) {
                String genreName = KopisGenreUtil.getConcertGenreName(genreCode);
                log.info("콘서트 장르 {} 조회 시작 - {}", genreCode, genreName);

                try {
                    syncByGenrePaginated(genreCode, startDate, endDate, systemMember, defaultRegion, result, "CONCERT");

                } catch (Exception e) {
                    log.error("콘서트 장르 {} 동기화 실패: {}", genreCode, e.getMessage(), e);
                    result.addFailure(String.format("콘서트 장르 %s (%s) 동기화 실패: %s",
                            genreCode, genreName, e.getMessage()));
                }
            }

            result.complete();
            log.info("=== Concert 동기화 완료: 총 {}건 처리 (성공: {}, 실패: {}) ===",
                    result.getTotalProcessed(), result.getSuccessCount(), result.getFailureCount());

            return result;

        } catch (Exception e) {
            log.error("Concert 동기화 전체 실패", e);
            result.addFailure("전체 동기화 실패: " + e.getMessage());
            result.complete();
            return result;
        }
    }

    /**
     * 전체 Musical 동기화
     */
    public SyncResult syncMusicals(String startDate, String endDate) {
        log.info("Musical 동기화 시작: {} ~ {}", startDate, endDate);
        SyncResult result = new SyncResult("MUSICAL");

        try {
            Member systemMember = getSystemMember();
            Region defaultRegion = getDefaultRegion();

            String genreCode = KopisGenreUtil.MusicalGenre.MUSICAL.getCode();
            String genreName = KopisGenreUtil.getMusicalGenreName(genreCode);
            log.info("뮤지컬 장르 {} 조회 시작 - {}", genreCode, genreName);

            try {
                syncByGenrePaginated(genreCode, startDate, endDate,
                        systemMember, defaultRegion, result, "MUSICAL");

            } catch (Exception e) {
                log.error("뮤지컬 장르 {} 동기화 실패: {}", genreCode, e.getMessage(), e);
                result.addFailure(String.format("뮤지컬 장르 %s 동기화 실패: %s", genreCode, e.getMessage()));
            }

            result.complete();
            log.info("=== Musical 동기화 완료: 총 {}건 처리 (성공: {}, 실패: {}) ===",
                    result.getTotalProcessed(), result.getSuccessCount(), result.getFailureCount());

            return result;

        } catch (Exception e) {
            log.error("Musical 동기화 전체 실패", e);
            result.addFailure("전체 동기화 실패: " + e.getMessage());
            result.complete();
            return result;
        }
    }

    private void syncByGenrePaginated(String genreCode, String startDate, String endDate,
                                    Member systemMember, Region defaultRegion,
                                    SyncResult result, String syncType) {

        int pageNum = 1;
        boolean hasMoreData = true;
        int totalProcessedForGenre = 0;

        while(hasMoreData && pageNum <= 10) {
            try {
                log.debug("{} 장르 {} - {}페이지 조회", syncType, genreCode, pageNum);

                String xmlResponse = kopisApiService.getPerformanceListByGenre(
                        startDate, endDate, pageNum, 100, genreCode);

                if(xmlResponse != null) {
                    List<KopisPerformanceDto> performances = parseXmlToPerformanceList(xmlResponse);
                    log.debug("{} 장르 {} - {}페이지: {}개 데이터 수신",
                            syncType, genreCode, pageNum, performances.size());

                    if(performances.isEmpty()) {
                        hasMoreData = false;
                    } else {
                        for (KopisPerformanceDto performance : performances) {
                            try {
                                if("CONCERT".equals(syncType)) {
                                    syncSingleConcert(performance, systemMember, defaultRegion, result);
                                } else if("MUSICAL".equals(syncType)) {
                                    syncSingleMusical(performance, systemMember, defaultRegion, result);
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

        String genreName = "CONCERT".equals(syncType) ?
                KopisGenreUtil.getConcertGenreName(genreCode) :
                KopisGenreUtil.getMusicalGenreName(genreCode);

        log.info("{} 장르 {} 동기화 완료: {}건 처리", syncType, genreName, totalProcessedForGenre);
    }

    /**
     * 장르별 콘서트 동기화 (신규 메서드)
     */
    private void syncConcertsByGenre(
            String genreCode, String startDate, String endDate,
            Member systemMember, Region defaultRegion, SyncResult result) {

        int pageNum = 1;
        boolean hasMoreData = true;
        int totalProcessedForGenre = 0;

        while (hasMoreData && pageNum <= 10) { // 최대 10페이지
            try {
                log.debug("장르 {} - {}페이지 조회", genreCode, pageNum);

                String xmlResponse = kopisApiService.getPerformanceListByGenre(
                        startDate, endDate, pageNum, 100, genreCode);

                if (xmlResponse != null) {
                    List<KopisPerformanceDto> performances = parseXmlToPerformanceList(xmlResponse);
                    log.debug("장르 {} - {}페이지: {}개 데이터 수신", genreCode, pageNum, performances.size());

                    if (performances.isEmpty()) {
                        hasMoreData = false;
                    } else {
                        for (KopisPerformanceDto performance : performances) {
                            try {
                                syncSingleConcertEnhanced(performance, systemMember, defaultRegion, result);
                                totalProcessedForGenre++;
                            } catch (Exception e) {
                                log.error("개별 콘서트 처리 실패: ID={}, 오류={}",
                                        performance.getMt20id(), e.getMessage());
                                result.addFailure(String.format("공연 ID %s 동기화 실패: %s",
                                        performance.getMt20id(), e.getMessage()));
                            }
                        }

                        // 다음 페이지 확인
                        if (performances.size() < 100) {
                            hasMoreData = false;
                        }
                        pageNum++;
                    }
                } else {
                    log.warn("장르 {} - {}페이지: 응답 데이터 없음", genreCode, pageNum);
                    hasMoreData = false;
                }

                // 요청 간격 (API 부하 방지)
                if (hasMoreData) {
                    Thread.sleep(500); // 0.5초 대기
                }

            } catch (Exception e) {
                log.error("장르 {} - {}페이지 조회 실패: {}", genreCode, pageNum, e.getMessage());
                hasMoreData = false;
            }
        }
    }

    /**
     * 개별 콘서트 동기화 (대폭 개선된 버전)
     */
    private void syncSingleConcertEnhanced(
            KopisPerformanceDto basicDto, Member systemMember,
            Region defaultRegion, SyncResult result) {

        String kopisId = basicDto.getMt20id();
        log.debug("콘서트 동기화 처리: ID={}, 제목={}", kopisId, basicDto.getPrfnm());

        try {
            // 1. 상세 정보 조회 및 병합
            KopisPerformanceDto enhancedDto = fetchAndMergeDetailInfo(basicDto);

            // 2. 데이터 검증
            if (!validatePerformanceData(enhancedDto)) {
                result.addFailure("데이터 검증 실패: " + kopisId);
                return;
            }

            // 3. 기존 데이터 확인 및 처리
            Optional<Concert> existingConcert = concertRepository.findByKopisId(kopisId);

            Concert concert;
            boolean isNewRecord = false;

            if (existingConcert.isPresent()) {
                // 기존 데이터 업데이트
                concert = existingConcert.get();
//                concert.updateFromKopisData(enhancedDto);

                // 상세 정보가 있으면 추가 업데이트
                if (hasDetailInfo(enhancedDto)) {
                    concert.updateFromKopisDetailData(enhancedDto);
                }

                log.debug("기존 콘서트 업데이트: {}", concert.getTitle());
            } else {
                // 신규 데이터 생성
                concert = Concert.fromKopisData(enhancedDto, defaultRegion, systemMember);

                // 상세 정보가 있으면 추가 설정
                if (hasDetailInfo(enhancedDto)) {
                    concert.updateFromKopisDetailData(enhancedDto);
                }

                isNewRecord = true;
                log.debug("신규 콘서트 생성: {}", concert.getTitle());
            }

            // 4. 저장
            Concert savedConcert = concertRepository.save(concert);
            result.addSuccess(isNewRecord);

            log.debug("콘서트 저장 완료: DB_ID={}, KOPIS_ID={}, 제목={}",
                    savedConcert.getId(), savedConcert.getKopisId(), savedConcert.getTitle());

        } catch (Exception e) {
            log.error("콘서트 동기화 실패: KOPIS_ID={}, 오류={}", kopisId, e.getMessage(), e);
            result.addFailure("콘서트 ID " + kopisId + " 처리 실패: " + e.getMessage());
        }
    }



    /**
     * 개별 콘서트 동기화
     */
    private void syncSingleConcert(KopisPerformanceDto dto, Member systemMember,
                                   Region defaultRegion, SyncResult result) {
        try {
            // 1. 상세 정보 추가 조회
            String detailXml = kopisApiService.getPerformanceDetail(dto.getMt20id());
            if (detailXml != null) {
                List<KopisPerformanceDto> detailList = parseXmlToPerformanceList(detailXml);
                if (!detailList.isEmpty()) {
                    KopisPerformanceDto detailDto = detailList.get(0);
                    // 상세 정보를 기본 정보에 병합
                    mergeDetailInfo(dto, detailDto);
                }
            }

            // 2. 기본 정보로 엔티티 생성/업데이트
            Optional<Concert> existingConcert = concertRepository.findByKopisId(dto.getMt20id());

            Concert concert;
            if (existingConcert.isPresent()) {
                concert = existingConcert.get();
                concert.updateFromKopisData(dto);
                // 핵심 추가: 상세 정보 업데이트
                if (hasDetailData(dto)) {
                    log.debug("상세 정보 발견됨, updateFromKopisDetailData 호출: {}", concert.getTitle());
                    concert.updateFromKopisDetailData(dto);
                    log.debug("상세 정보 업데이트 완료: {}", concert.getTitle());
                } else {
                    log.debug("상세 정보 없음: {}", concert.getTitle());
                }
                result.addSuccess(false); // 업데이트
            } else {
                concert = Concert.fromKopisData(dto, defaultRegion, systemMember);
                // 핵심 추가: 신규 생성 시에도 상세 정보 적용
                if (hasDetailData(dto)) {
                    log.debug("신규 생성 시 상세 정보 발견됨, updateFromKopisDetailData 호출: {}", concert.getTitle());
                    concert.updateFromKopisDetailData(dto);
                    log.debug("신규 생성 시 상세 정보 적용: {}", concert.getTitle());
                } else {
                    log.debug("신규 생성 시 상세 정보 없음: {}", concert.getTitle());
                }
                result.addSuccess(true); // 신규생성
            }

            concertRepository.save(concert);
        } catch (Exception e) {
            log.error("공연 상세 정보 처리 실패: {}", dto.getMt20id(), e.getMessage());
            result.addFailure("공연 ID " + dto.getMt20id() + "처리 실패: " + e.getMessage());
        }
    }

    private void mergeDetailInfo(KopisPerformanceDto basicDto, KopisPerformanceDto detailDto) {
        // 완전한 병합 메서드 호출로 교체
        mergeAllDetailInfo(basicDto, detailDto);
    }

    /**
     * 개별 뮤지컬 동기화
     */
    private void syncSingleMusical(
            KopisPerformanceDto dto,
            Member systemMember,
            Region defaultRegion,
            SyncResult result) {

        try {
            // 1. 상세 정보 추가 조회
            String detailXml = kopisApiService.getPerformanceDetail(dto.getMt20id());
            if (detailXml != null) {
                List<KopisPerformanceDto> detailList = parseXmlToPerformanceList(detailXml);
                if (!detailList.isEmpty()) {
                    KopisPerformanceDto detailDto = detailList.get(0);
                    // 상세 정보를 기본 정보에 병합
                    mergeDetailInfo(dto, detailDto);
                }
            }

            // 2. 기존 정보로 엔티티 생성/업데이트
            Optional<Musical> existingMusical = musicalRepository.findByKopisId(dto.getMt20id());

            Musical musical;
            if (existingMusical.isPresent()) {
                musical = existingMusical.get();
                musical.updateFromKopisData(dto);
                if (hasDetailData(dto)) {
                    log.debug("상세 정보 발견됨, updateFromKopisDetailData 호출: {}", musical.getTitle());
                    musical.updateFromKopisDetailData(dto);
                    log.debug("상세 정보 업데이트 완료: {}", musical.getTitle());
                } else {
                    log.debug("상세 정보 없음: {}", musical.getTitle());
                }
                result.addSuccess(false); // 업데이트

            } else {
                musical = Musical.fromKopisData(dto, defaultRegion, systemMember);
                if (hasDetailData(dto)) {
                    log.debug("신규 생성 시 상세 정보 발견됨, updateFromKopisDetailData 호출: {}", musical.getTitle());
                    musical.updateFromKopisDetailData(dto);
                    log.debug("신규 생성 시 상세 정보 적용: {}", musical.getTitle());
                } else {
                    log.debug("신규 생성 시 상세 정보 없음: {}", musical.getTitle());
                }
                result.addSuccess(true); // 신규생성
            }

            musicalRepository.save(musical);
        } catch (Exception e) {
            log.error("공연 상세 정보 처리 실패: {}", dto.getMt20id(), e.getMessage());
            result.addFailure("공연 ID " + dto.getMt20id() + "처리 실패: " + e.getMessage());
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
     * XML 문자열을 KopisPerformanceDto 리스트로 파싱
     */
    public List<KopisPerformanceDto> parseXmlToPerformanceList(String xmlResponse) {
        List<KopisPerformanceDto> performances = new ArrayList<>();

        try {
            // 간단한 정규식을 사용한 XML 파싱
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
     * 개별 공연 정보 파싱
     */
    private KopisPerformanceDto parsePerformanceFromXml(String xmlContent) {
        try {
            KopisPerformanceDto dto = new KopisPerformanceDto();

            dto.setMt20id(extractXmlValue(xmlContent, "mt20id"));
            dto.setPrfnm(extractXmlValue(xmlContent, "prfnm"));
            dto.setPrfpdfrom(extractXmlValue(xmlContent, "prfpdfrom"));
            dto.setPrfpdto(extractXmlValue(xmlContent, "prfpdto"));
            dto.setFcltynm(extractXmlValue(xmlContent, "fcltynm"));
            dto.setPoster(extractXmlValue(xmlContent, "poster"));
            dto.setArea(extractXmlValue(xmlContent, "area"));
            dto.setGenrenm(extractXmlValue(xmlContent, "genrenm"));
//            dto.setOpenrun(extractXmlValue(xmlContent, "openrun"));
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
     * XML에서 특정 태그의 값 추출 (개선된 버전)
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

            // 특별한 태그들 처리
            if ("styurls".equals(tagName)) {
                return extractStyurls(value);
            }

            // 빈 값이거나 공백만 있는 경우 null 반환
            return value.isEmpty() || value.trim().isEmpty() ? null : value;
        }

        return null;
    }

    /**
     * styurls 태그 내의 여러 styurl 추출
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
                if (urls.length() > 0) {
                    urls.append(",");
                }
                urls.append(url);
            }
        }

        return urls.length() > 0 ? urls.toString() : null;
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
     * 테스트용 KOPIS API 호출(페이지 지정 가능)
     */
    public String testKopisApiCall(String startDate, String endDate, int page, int rows) {
        return kopisApiService.getPerformanceList(startDate, endDate, page, rows);
    }



    /**
     * KOPIS 상세 정보 XML 직접 조회 (디버깅용)
     */
    public String getKopisDetailXml(String kopisId) {
        return kopisApiService.getPerformanceDetail(kopisId);
    }

    /**
     * 완전한 상세 정보 병합 (12개 필드 모두 처리)
     */
    private void mergeAllDetailInfo(KopisPerformanceDto basicDto, KopisPerformanceDto detailDto) {
        // 기존 3개 필드
        if (isValidString(detailDto.getPrftime())) {
            basicDto.setPrftime(removeEmojis(detailDto.getPrftime()));
        }
        if (isValidString(detailDto.getPcseguidance())) {
            basicDto.setPcseguidance(removeEmojis(detailDto.getPcseguidance()));
        }

        // 누락된 중요 필드들 추가
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
                isValidString(dto.getStyurls());
    }

    /**
     * 상세 정보 조회 및 기본 정보와 병합 (신규 메서드)
     */
    private KopisPerformanceDto fetchAndMergeDetailInfo(KopisPerformanceDto basicDto) {
        try {
            // 상세 정보 조회
            String detailXml = kopisApiService.getPerformanceDetail(basicDto.getMt20id());

            if (detailXml != null) {
                List<KopisPerformanceDto> detailList = parseXmlToPerformanceList(detailXml);

                if (!detailList.isEmpty()) {
                    KopisPerformanceDto detailDto = detailList.get(0);

                    // 완전한 병합 수행
                    return mergeCompleteInfo(basicDto, detailDto);
                }
            }
        } catch (Exception e) {
            log.warn("상세 정보 조회 실패: KOPIS_ID={}, 계속 진행: {}",
                    basicDto.getMt20id(), e.getMessage());
        }

        return basicDto; // 기본 정보만 반환
    }

    /**
     * 완전한 정보 병합 (기존 mergeDetailInfo의 대체)
     */
    private KopisPerformanceDto mergeCompleteInfo(KopisPerformanceDto basicDto, KopisPerformanceDto detailDto) {
        // 기본 정보를 복사
        KopisPerformanceDto mergedDto = copyBasicInfo(basicDto);

        // 상세 정보 병합 (null이 아닌 값들만)
        if (isValidString(detailDto.getPrftime())) {
            mergedDto.setPrftime(detailDto.getPrftime());
        }

        if (isValidString(detailDto.getPcseguidance())) {
            mergedDto.setPcseguidance(detailDto.getPcseguidance());
        }

        if (isValidString(detailDto.getDtguidance())) {
            mergedDto.setDtguidance(detailDto.getDtguidance());
        }

        if (isValidString(detailDto.getStyurls())) {
            mergedDto.setStyurls(detailDto.getStyurls());
        }

        if (isValidString(detailDto.getPrfruntime())) {
            mergedDto.setPrfruntime(detailDto.getPrfruntime());
        }

        if (isValidString(detailDto.getPrfage())) {
            mergedDto.setPrfage(detailDto.getPrfage());
        }

        log.debug("정보 병합 완료: 기본+상세 데이터 결합");
        return mergedDto;
    }

    /**
     * 기본 정보 복사
     */
    private KopisPerformanceDto copyBasicInfo(KopisPerformanceDto source) {
        KopisPerformanceDto copy = new KopisPerformanceDto();

        copy.setMt20id(source.getMt20id());
        copy.setPrfnm(source.getPrfnm());
        copy.setPrfpdfrom(source.getPrfpdfrom());
        copy.setPrfpdto(source.getPrfpdto());
        copy.setFcltynm(source.getFcltynm());
        copy.setPoster(source.getPoster());
        copy.setArea(source.getArea());
        copy.setGenrenm(source.getGenrenm());
        copy.setPrfstate(source.getPrfstate());

        return copy;
    }

    /**
     * 데이터 검증
     */
    private boolean validatePerformanceData(KopisPerformanceDto dto) {
        // 필수 필드 검증
        if (!isValidString(dto.getMt20id())) {
            log.warn("KOPIS ID가 없음");
            return false;
        }

        if (!isValidString(dto.getPrfnm())) {
            log.warn("공연명이 없음: KOPIS_ID={}", dto.getMt20id());
            return false;
        }

        // 날짜 검증
        if (!isValidString(dto.getPrfpdfrom()) || !isValidString(dto.getPrfpdto())) {
            log.warn("공연 날짜 정보가 없음: KOPIS_ID={}", dto.getMt20id());
            return false;
        }

        return true;
    }

    /**
     * 상세 정보 존재 여부 확인
     */
    private boolean hasDetailInfo(KopisPerformanceDto dto) {
        return isValidString(dto.getPrftime()) ||
                isValidString(dto.getPcseguidance()) ||
                isValidString(dto.getDtguidance()) ||
                isValidString(dto.getStyurls());
    }

    /**
     * 문자열 유효성 검사 및 이모지 제거
     */
    private boolean isValidString(String str) {
        return str != null && !str.trim().isEmpty();
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

}