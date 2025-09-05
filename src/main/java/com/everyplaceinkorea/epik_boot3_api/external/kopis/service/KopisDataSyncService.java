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

    /**
     * 전체 Concert 동기화
     */
    public SyncResult syncAllConcerts(String startDate, String endDate) {
        log.info("Concert 동기화 시작: {} ~ {}", startDate, endDate);
        SyncResult result = new SyncResult("CONCERT");

        try {
            String xmlResponse = kopisApiService.getPerformanceList(startDate, endDate, 1, 100); // 1000 → 100으로 줄임
            
            if (xmlResponse != null) {
                List<KopisPerformanceDto> performances = parseXmlToPerformanceList(xmlResponse);
                log.info("KOPIS API로부터 {}개의 공연 데이터 수신", performances.size());

                Member systemMember = getSystemMember();
                Region defaultRegion = getDefaultRegion();

                for (KopisPerformanceDto performance : performances) {
                    try {
                        if (isConcertGenre(performance.getGenrenm())) {
                            log.debug("콘서트 동기화 중: {} ({})", performance.getPrfnm(), performance.getMt20id());
                            syncSingleConcert(performance, systemMember, defaultRegion, result);
                        } else {
                            result.addSkipped("콘서트 장르가 아님: " + performance.getGenrenm());
                        }
                    } catch (Exception e) {
                        result.addFailure(String.format("공연 ID %s 동기화 실패: %s", 
                                performance.getMt20id(), e.getMessage()));
                        log.error("개별 공연 동기화 실패: {}", performance.getMt20id(), e);
                    }
                    
                    // 진행률 로깅 (10개마다)
                    if ((result.getTotalProcessed() + 1) % 10 == 0) {
                        log.info("콘서트 동기화 진행률: {}/{}", result.getTotalProcessed() + 1, performances.size());
                    }
                }
            } else {
                result.addFailure("KOPIS API 응답이 null입니다.");
            }

            result.complete();
            log.info("Concert 동기화 완료: {}", result.getSummary());
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
            String xmlResponse = kopisApiService.getPerformanceList(startDate, endDate, 1, 100); // 1000 → 100으로 줄임
            
            if (xmlResponse != null) {
                List<KopisPerformanceDto> performances = parseXmlToPerformanceList(xmlResponse);
                log.info("KOPIS API로부터 {}개의 공연 데이터 수신", performances.size());

                Member systemMember = getSystemMember();
                Region defaultRegion = getDefaultRegion();

                for (KopisPerformanceDto performance : performances) {
                    try {
                        if (isMusicalGenre(performance.getGenrenm())) {
                            log.debug("뮤지컬 동기화 중: {} ({})", performance.getPrfnm(), performance.getMt20id());
                            syncSingleMusical(performance, systemMember, defaultRegion, result);
                        } else {
                            result.addSkipped("뮤지컬 장르가 아님: " + performance.getGenrenm());
                        }
                    } catch (Exception e) {
                        result.addFailure(String.format("공연 ID %s 동기화 실패: %s", 
                                performance.getMt20id(), e.getMessage()));
                        log.error("개별 뮤지컬 동기화 실패: {}", performance.getMt20id(), e);
                    }
                    
                    // 진행률 로깅 (10개마다)
                    if ((result.getTotalProcessed() + 1) % 10 == 0) {
                        log.info("뮤지컬 동기화 진행률: {}/{}", result.getTotalProcessed() + 1, performances.size());
                    }
                }
            } else {
                result.addFailure("KOPIS API 응답이 null입니다.");
            }

            result.complete();
            log.info("Musical 동기화 완료: {}", result.getSummary());
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
        
        Optional<Concert> existingConcert = concertRepository.findByKopisId(dto.getMt20id());
        
        if (existingConcert.isPresent()) {
            Concert concert = existingConcert.get();
            concert.updateFromKopisData(dto);
            concertRepository.save(concert);
            result.addSuccess(false); // 업데이트
            log.debug("콘서트 업데이트: {}", dto.getPrfnm());
        } else {
            Concert newConcert = Concert.fromKopisData(dto, defaultRegion, systemMember);
            concertRepository.save(newConcert);
            result.addSuccess(true); // 신규 생성
            log.debug("새 콘서트 생성: {}", dto.getPrfnm());
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
    private List<KopisPerformanceDto> parseXmlToPerformanceList(String xmlResponse) {
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
        String lowerGenre = genrenm.toLowerCase();
        String[] concertGenres = {"클래식", "오페라", "국악", "무용", "콘서트"};
        
        for (String genre : concertGenres) {
            if (lowerGenre.contains(genre.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean isMusicalGenre(String genrenm) {
        if (genrenm == null) return false;
        return genrenm.toLowerCase().contains("뮤지컬");
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
}
