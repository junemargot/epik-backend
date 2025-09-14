package com.everyplaceinkorea.epik_boot3_api.external.kopis.service;

import com.everyplaceinkorea.epik_boot3_api.entity.Region;
import com.everyplaceinkorea.epik_boot3_api.entity.common.DataSource;
import com.everyplaceinkorea.epik_boot3_api.entity.concert.Concert;
import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import com.everyplaceinkorea.epik_boot3_api.entity.musical.Musical;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.KopisApiService;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.dto.KopisPerformanceDto;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.dto.SyncResult;
import com.everyplaceinkorea.epik_boot3_api.repository.Member.MemberRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.RegionRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.concert.ConcertRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.musical.MusicalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // KOPIS 장르 코드 매핑
    private static final Map<String, String> GENRE_CODE_MAP = Map.of(
            "CCCA", "서양음악(클래식)",
            "CCCD", "대중음악",
            "BBBC", "무용(서양/한국무용)",
            "BBBE", "대중무용",
            "EEEA", "복합",
            "GGGA", "뮤지컬"
    );

    // 콘서트 관련 장르 코드들
    private static final String[] CONCERT_GENRE_CODES = {"CCCA", "CCCD", "BBBC", "BBBE", "EEEA"};
    private static final String MUSICAL_GENRE_CODE = "GGGA";

    /**
     * 전체 Concert 동기화: 장르별 개별 조회
     */
    public SyncResult syncAllConcerts(String startDate, String endDate) {
        log.info("Concert 동기화 시작: {} ~ {}", startDate, endDate);
        SyncResult result = new SyncResult("CONCERT");

        try {
            String[] concertGenres = {"CCCA", "CCCD", "BBBC", "BBBE", "EEEA"};

            Member systemMember = getSystemMember();
            Region defaultRegion = getDefaultRegion();

            // 장르별 조회
            for (String genreCode : CONCERT_GENRE_CODES) {
                log.info("=== 장르 코드 {} 조회 시작 ===", genreCode);

                try {
                    String xmlResponse = kopisApiService.getPerformanceListByGenre(startDate, endDate, 1, 100, genreCode);

                    if(xmlResponse != null) {
                        List<KopisPerformanceDto> performances = parseXmlToPerformanceList(xmlResponse);
                        log.info("장르 {}에서 {}개의 공연 데이터 수신", genreCode, performances.size());

                        for(KopisPerformanceDto performance : performances) {
                            try {
                                syncSingleConcert(performance, systemMember, defaultRegion, result);
                            } catch(Exception e) {
                                result.addFailure(String.format("공연 ID %s 동기화 실패: %s",
                                        performance.getMt20id(), e.getMessage()));
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("장르 {} 조회 실패: {}", genreCode, e.getMessage());
                    result.addFailure("장르 " + genreCode + " 조회 실패: " + e.getMessage());
                }
            }

            result.complete();
            log.info("Concert 동기화 완료: 총 {}건 처리", result.getTotalProcessed());
            return result;

        } catch (Exception e) {
            log.error("Concert 동기화 실패", e);
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

            // 페이지별로 모든 뮤지컬 데이터 가져오기
            int page = 1;
            boolean hasMoreData = true;

            while(hasMoreData) {
                String xmlResponse = kopisApiService.getPerformanceListByGenre(startDate, endDate, page, 100, MUSICAL_GENRE_CODE);

                if (xmlResponse != null) {
                    List<KopisPerformanceDto> performances = parseXmlToPerformanceList(xmlResponse);
                    log.info("뮤지컬에서 {}개의 공연 데이터 수신", performances.size());

                    if(performances.isEmpty()) {
                        hasMoreData = false;
                    } else {
                        for (KopisPerformanceDto performance : performances) {
                            try {
                                syncSingleMusical(performance, systemMember, defaultRegion, result);
                            } catch (Exception e) {
                                result.addFailure(String.format("공연 ID %s 동기화 실패: %s",
                                        performance.getMt20id(), e.getMessage()));
                            }
                        }
                        page++;

                        if(page > 50) {
                            log.warn("뮤지컬 최대 페이지 수 도달");
                            hasMoreData = false;
                        }
                    }
                } else {
                    hasMoreData = false;
                }
            }

            result.complete();
            log.info("Musical 동기화 완료: 총 {}건 처리", result.getTotalProcessed());
            return result;

        } catch (Exception e) {
            log.error("Musical 동기화 실패", e);
            result.addFailure("전체 동기화 실패: " + e.getMessage());
            result.complete();
            return result;
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
                result.addSuccess(false); // 업데이트
            } else {
                concert = Concert.fromKopisData(dto, defaultRegion, systemMember);
                result.addSuccess(true); // 신규생성
            }

            concertRepository.save(concert);
        } catch(Exception e) {
            log.error("공연 상세 정보 처리 실패: {}", dto.getMt20id(), e.getMessage());
            result.addFailure("공연 ID " + dto.getMt20id() + "처리 실패: " + e.getMessage());
        }
    }

    private void mergeDetailInfo(KopisPerformanceDto basicDto, KopisPerformanceDto detailDto) {
        // 상세 정보를 기본 정보에 복사
        if(detailDto.getPrftime() != null) {
            basicDto.setPrftime(detailDto.getPrftime());
        }

        if(detailDto.getPcseguidance() != null) {
            basicDto.setPcseguidance(detailDto.getPcseguidance());
        }
        
        if(detailDto.getEntrpsnmS() != null) {
            basicDto.setEntrpsnmS(detailDto.getEntrpsnmS());
        }
    }

    /**
     * 개별 뮤지컬 동기화
     */
    private void syncSingleMusical(KopisPerformanceDto dto, Member systemMember, 
                                   Region defaultRegion, SyncResult result) {
        
        Optional<Musical> existingMusical = musicalRepository.findByKopisId(dto.getMt20id());
        
        if (existingMusical.isPresent()) {
            Musical musical = existingMusical.get();
            musical.updateFromKopisData(dto);
            musicalRepository.save(musical);
            result.addSuccess(false); // 업데이트
            log.debug("뮤지컬 업데이트: {}", dto.getPrfnm());
        } else {
            Musical newMusical = Musical.fromKopisData(dto, defaultRegion, systemMember);
            musicalRepository.save(newMusical);
            result.addSuccess(true); // 신규 생성
            log.debug("새 뮤지컬 생성: {}", dto.getPrfnm());
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
            
            if (xmlResponse != null) {
                List<KopisPerformanceDto> performances = parseXmlToPerformanceList(xmlResponse);
                
                if (!performances.isEmpty()) {
                    KopisPerformanceDto performance = performances.get(0);
                    Member systemMember = getSystemMember();
                    Region defaultRegion = getDefaultRegion();
                    SyncResult dummyResult = new SyncResult("SINGLE");
                    
                    if (isConcertGenre(performance.getGenrenm())) {
                        syncSingleConcert(performance, systemMember, defaultRegion, dummyResult);
                    } else if (isMusicalGenre(performance.getGenrenm())) {
                        syncSingleMusical(performance, systemMember, defaultRegion, dummyResult);
                    } else {
                        log.warn("알 수 없는 장르: {} - {}", kopisId, performance.getGenrenm());
                    }
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
            dto.setOpenrun(extractXmlValue(xmlContent, "openrun"));
            dto.setPrfstate(extractXmlValue(xmlContent, "prfstate"));

            dto.setPrftime(extractXmlValue(xmlContent, "prftime"));
            dto.setPcseguidance(extractXmlValue(xmlContent, "pcseguidance"));
            dto.setDtguidance(extractXmlValue(xmlContent, "dtguidance"));
            dto.setStyurls(extractXmlValue(xmlContent, "styurls"));
            dto.setEntrpsnmP(extractXmlValue(xmlContent, "entrpsnmP"));
            dto.setEntrpsnmA(extractXmlValue(xmlContent, "entrpsnmA"));
            dto.setEntrpsnmH(extractXmlValue(xmlContent, "entrpsnmH"));
            dto.setEntrpsnmS(extractXmlValue(xmlContent, "entrpsnmS"));
            dto.setPrfcast(extractXmlValue(xmlContent, "prfcast"));
            dto.setPrfcrew(extractXmlValue(xmlContent, "prfcrew"));
            dto.setPrfruntime(extractXmlValue(xmlContent, "prfruntime"));
            dto.setPrfage(extractXmlValue(xmlContent, "prfage"));


            return dto;
        } catch (Exception e) {
            log.warn("개별 공연 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * XML에서 특정 태그의 값 추출
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
            return value.isEmpty() ? null : value;
        }
        
        return null;
    }

    /**
     * 장르 판별 메서드들
     */
    private boolean isConcertGenre(String genrenm) {
        if (genrenm == null) return false;
        log.info("콘서트 장르 확인: {} ===", genrenm);

        // 장르 코드로 서치
//        String lowerGenre = genrenm.toLowerCase();
//        String[] concertGenres = {"클래식", "오페라", "국악", "무용", "콘서트"};
        String[] concertCodes = {"CCCA", "CCCC", "CCCD", "BBBC", "BBBE", "EEEA", "EEEB"};
        // CCCA: 서양음악(클래식), CCCC: 한국음악(국악), CCCD: 대중음악
        // BBBC: 무용(서양/한국무용), BBBE: 대중무용
        // EEEA: 복합, EEEB: 서커스/마술

        for (String code : concertCodes) {
            if(code.equals(genrenm)) {
                log.info("콘서트 장르 매칭: {} -> {}", genrenm, code);
                return true;
            }
        }

        log.debug("콘서트 장르 아님: {}", genrenm);
        return false;
    }

    private boolean isMusicalGenre(String genrenm) {
        if (genrenm == null) return false;
        log.info("=== 뮤지컬 장르 확인: [{}] ===", genrenm);

        // 뮤지컬 장르 코드 GGGA
        if("GGGA".equals(genrenm)) {
            log.info("뮤지컬 장르 매칭: {} -> GGGA", genrenm);
            return true;
        }
        
        log.debug("뮤지컬 장르 아님: {}", genrenm);
        return false;
    }

    /**
     * 단순한 Concert 생성 테스트 (트랜잭션 없이)
     */
    public void testCreateConcert() {
        log.info("Concert 생성 테스트 시작");
        
        try {
            // 1. Member 조회
            Member systemMember = memberRepository.findById(1L).orElse(null);
            if (systemMember == null) {
                throw new RuntimeException("시스템 멤버(ID: 1)를 찾을 수 없습니다.");
            }
            log.info("시스템 멤버 조회 성공: {}", systemMember.getId());
            
            // 2. Region 조회  
            Region defaultRegion = regionRepository.findById(1L).orElse(null);
            if (defaultRegion == null) {
                throw new RuntimeException("기본 지역(ID: 1)을 찾을 수 없습니다.");
            }
            log.info("기본 지역 조회 성공: {}", defaultRegion.getId());
            
            // 3. Concert 생성
            Concert testConcert = new Concert();
            testConcert.setTitle("테스트 콘서트 " + System.currentTimeMillis());
            testConcert.setContent("테스트 내용입니다.");
            testConcert.setAddress("테스트 주소");
            testConcert.setVenue("테스트 공연장");
            testConcert.setStartDate(LocalDate.now());
            testConcert.setEndDate(LocalDate.now().plusDays(1));
            testConcert.setMember(systemMember);
            testConcert.setRegion(defaultRegion);
            testConcert.setViewCount(0);
            testConcert.setDataSource(DataSource.MANUAL);
            testConcert.setStatus(com.everyplaceinkorea.epik_boot3_api.entity.musical.Status.ACTIVE);
            
            log.info("Concert 엔티티 생성 완료, 저장 시도...");
            
            // 4. 저장
            Concert savedConcert = concertRepository.save(testConcert);
            log.info("테스트 콘서트 생성 성공: ID {}", savedConcert.getId());
            
        } catch (Exception e) {
            log.error("Concert 생성 테스트 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 데이터베이스 연결 테스트
     */
    public long testDatabaseConnection() {
        log.info("데이터베이스 연결 테스트");
        
        try {
            // Member 테이블 카운트 조회
            long memberCount = memberRepository.count();
            log.info("Member 테이블 레코드 수: {}", memberCount);
            
            // Region 테이블 카운트 조회
            long regionCount = regionRepository.count();
            log.info("Region 테이블 레코드 수: {}", regionCount);
            
            // Concert 테이블 카운트 조회
            long concertCount = concertRepository.count();
            log.info("Concert 테이블 레코드 수: {}", concertCount);
            
            return memberCount;
        } catch (Exception e) {
            log.error("데이터베이스 연결 테스트 실패: {}", e.getMessage(), e);
            throw e;
        }
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

    public String testKopisApiCallByGenre(String startDate, String endDate, int page, int rows, String genreCode) {
        return kopisApiService.getPerformanceListByGenre(startDate, endDate, page, rows, genreCode);
    }
}
