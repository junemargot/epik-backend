package com.everyplaceinkorea.epik_boot3_api.external.kopis.service;

import com.everyplaceinkorea.epik_boot3_api.entity.concert.Concert;
import com.everyplaceinkorea.epik_boot3_api.entity.concert.ConcertTicketOffice;
import com.everyplaceinkorea.epik_boot3_api.entity.musical.Musical;
import com.everyplaceinkorea.epik_boot3_api.entity.musical.MusicalTicketOffice;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.dto.TicketOfficeScrapeResult;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.enums.TicketOfficeSource;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.scraper.KopisWebScraperService;
import com.everyplaceinkorea.epik_boot3_api.repository.concert.ConcertRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.concert.ConcertTicketOfficeRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.musical.MusicalRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.musical.MusicalTicketOfficeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * KOPIS 웹사이트 스크래핑 결과와 데이터베이스에 수기로 입력된 데이터를 병합하여,
 * Musical 및 Concert 엔티티의 예매처 정보를 동기화(업데이트)하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketOfficeSyncService {

  private final KopisWebScraperService scraperService;
  private final MusicalRepository musicalRepository;
  private final ConcertRepository concertRepository;
  private final MusicalTicketOfficeRepository musicalTicketOfficeRepository;
  private final ConcertTicketOfficeRepository concertTicketOfficeRepository;

  /**
   * 특정 뮤지컬의 예매처 정보를 스크래핑하고, 기존 데이터 병합하여 업데이트
   * KOPIS 웹사이트에서 스크래핑한 데이터, DB에 수기 입력된 데이터, 엔티티의 JSON 필드
   * 데이터를 병합하여 최종 결과를 다시 엔티티의 JSON 필드에 저장
   *
   * @param musicalId 예매처 정보를 업데이트할 뮤지컬의 데이터베이스 ID
   */
  @Transactional
  public void updateMusicalTicketOffices(Long musicalId) {
    Optional<Musical> musicalOpt = musicalRepository.findById(musicalId);

    if(musicalOpt.isEmpty()) {
      log.warn("뮤지컬을 찾을 수 없음: ID={}", musicalId);
      return;
    }

    Musical musical = musicalOpt.get();
    String kopisId = musical.getKopisId();

    if(kopisId == null || kopisId.trim().isEmpty()) {
      log.warn("KOPIS ID가 없음: Musical ID={}", musicalId);
      return;
    }

    try {
      // 1. 스크래핑으로 예매처 정보 수집
      List<TicketOfficeScrapeResult> scrapedOffices = scraperService.scrapeTicketOffices(kopisId);

      if(scrapedOffices.isEmpty()) {
        log.info("스크래핑된 예매처 정보가 없음: KOPIS ID={}", kopisId);
        return;
      }

      // 2. 기존 수기입력 데이터와 병합
      Map<String, String> finalOffices = mergeMusicalTicketOffices(musical, scrapedOffices);

      // 3. JSON 필드 업데이트
      musical.setTicketOffices(finalOffices);
      musical.setKopisTicketOfficesSource(determineMusicalSource(musical, scrapedOffices));

      musicalRepository.save(musical);

      log.info("뮤지컬 예매처 정보 업데이트 완료: Musical ID={}, 예매처 수={}", musicalId, finalOffices.size());

    } catch(Exception e) {
      log.error("뮤지컬 예매처 정보 업데이트 실패: Musical ID={}, 에러={}", musicalId, e.getMessage(), e);
    }
  }

  /**
   * 특정 콘서트의 예매처 정보를 스크래핑하고, 기존 데이터와 병합하여 업데이트
   *
   * @param concertId 예매처 정보를 업데이트할 콘서트 데이터베이스 ID
   */
  @Transactional
  public void updateConcertTicketOffices(Long concertId) {
    Optional<Concert> concertOpt = concertRepository.findById(concertId);

    if (concertOpt.isEmpty()) {
      log.warn("콘서트를 찾을 수 없음: ID={}", concertId);
      return;
    }

    Concert concert = concertOpt.get();
    String kopisId = concert.getKopisId();

    if (kopisId == null || kopisId.trim().isEmpty()) {
      log.warn("KOPIS ID가 없음: Concert ID={}", concertId);
      return;
    }

    try {
      // 1. 스크래핑으로 예매처 정보 수집
      List<TicketOfficeScrapeResult> scrapedOffices = scraperService.scrapeTicketOffices(kopisId);

      if (scrapedOffices.isEmpty()) {
        log.info("스크래핑된 예매처 정보가 없음: KOPIS ID={}", kopisId);
        return;
      }

      // 2. 기존 수기입력 데이터와 병합
      Map<String, String> finalOffices = mergeConcertTicketOffices(concert, scrapedOffices);

      // 3. JSON 필드 업데이트
      concert.setTicketOffices(finalOffices);
      concert.setKopisTicketOfficesSource(determineConcertSource(concert, scrapedOffices));

      concertRepository.save(concert);

      log.info("콘서트 예매처 정보 업데이트 완료: Concert ID={}, 예매처 수={}",
              concertId, finalOffices.size());

    } catch (Exception e) {
      log.error("콘서트 예매처 정보 업데이트 실패: Concert ID={}, 에러={}",
              concertId, e.getMessage(), e);
    }
  }

  /**
   * 뮤지컬의 예매처 데이터를 병합
   * 병합 우선순위: 1. 기존 JSON 필드 -> 2. 수기 입력 테이블 -> 3. 스크래핑 결과
   * 동일한 예매처 이름(정규화된 키)이 존재할 경우, 나중에 추가된 데이터가 이전 데이터를 덮어씀
   *
   * @param musical 병합 대상 뮤지컬 엔티티
   * @param scrapedOffices 스크래핑으로 수집된 예매처 정보 리스트
   * @return 병합된 예매처 맵 (key: 정규화된 예매처 이름, value: 예매처 URL)
   */
  private Map<String, String> mergeMusicalTicketOffices(Musical musical, List<TicketOfficeScrapeResult> scrapedOffices) {
    Map<String, String> result = new HashMap<>();

    // 1. 기존 JSON 데이터 추가
    Map<String, String> existingOffices = musical.getTicketOffices();
    if (existingOffices != null) {
      result.putAll(existingOffices);
    }

    // 2. 기존 TicketOffice 테이블 데이터 추가 (수기입력)
    List<MusicalTicketOffice> manualOffices = musicalTicketOfficeRepository.findByMusical(musical);
    for (MusicalTicketOffice office : manualOffices) {
      String normalizedName = normalizeOfficeName(office.getName());
      result.put(normalizedName, office.getLink());
      log.debug("수기입력 예매처 추가: {} -> {}", normalizedName, office.getLink());
    }

    // 3. 스크래핑 데이터 추가 (기존 데이터 덮어쓰기)
    for (TicketOfficeScrapeResult dto : scrapedOffices) {
      result.put(dto.getOfficeName(), dto.getTicketUrl());
      log.debug("스크래핑 예매처 추가: {} -> {}", dto.getOfficeName(), dto.getTicketUrl());
    }

    return result;
  }

  /**
   * 콘서트의 예매처 데이터를 병합
   *
   * @param concert 병합 대상 콘서트 엔티티
   * @param scrapedOffices 스크래핑으로 수집된 예매처 정보 리스트
   * @return 병합된 예매처 맵 (key: 정규화된 예매처 이름, value: 예매처 URL)
   */
  private Map<String, String> mergeConcertTicketOffices(Concert concert, List<TicketOfficeScrapeResult> scrapedOffices) {
    Map<String, String> result = new HashMap<>();

    // 1. 기존 JSON 데이터 추가
    Map<String, String> existingOffices = concert.getTicketOffices();
    if (existingOffices != null) {
      result.putAll(existingOffices);
    }

    // 2. 기존 TicketOffice 테이블 데이터 추가 (수기입력)
    List<ConcertTicketOffice> manualOffices = concertTicketOfficeRepository.findByConcert(concert);
    for (ConcertTicketOffice office : manualOffices) {
      String normalizedName = normalizeOfficeName(office.getName());
      result.put(normalizedName, office.getLink());
      log.debug("수기입력 예매처 추가: {} -> {}", normalizedName, office.getLink());
    }

    // 3. 스크래핑 데이터 추가 (기존 데이터 덮어쓰기)
    for (TicketOfficeScrapeResult dto : scrapedOffices) {
      result.put(dto.getOfficeName(), dto.getTicketUrl());
      log.debug("스크래핑 예매처 추가: {} -> {}", dto.getOfficeName(), dto.getTicketUrl());
    }

    return result;
  }

  /**
   * 예매처 이름을 정규화하여 일관된 키로 사용하도록 변환
   * 주요 예매처는 사전 정의된 키로 매핑하며, 그 외에는 공백과 특수문자를 제거
   *
   * @param name 원본 예매처 이름
   * @return 정규화된 예매처 이름(키)
   */
  private String normalizeOfficeName(String name) {
    if (name == null) return "other";

    name = name.trim().toLowerCase();

    if (name.contains("인터파크")) return "interpark";
    if (name.contains("티켓링크")) return "ticketlink";
    if (name.contains("yes24")) return "yes24";
    if (name.contains("옥션")) return "auction";
    if (name.contains("g마켓") || name.contains("지마켓")) return "gmarket";
    if (name.contains("교보문고")) return "kyobo";

    return name.replaceAll("[^a-z0-9]", "");
  }

  /**
   * 뮤지컬 예메처 데이터의 출처 계산
   *
   * @param musical 대상 뮤지컬 엔티티
   * @param scrapedOffices 스크래핑된 예매처 리스트
   * @return 데이터 출처 (MANUAL, SCRAPED, MIXED)
   */
  private TicketOfficeSource determineMusicalSource(Musical musical, List<TicketOfficeScrapeResult> scrapedOffices) {
    boolean hasManual = !musicalTicketOfficeRepository.findByMusical(musical).isEmpty()
            || !musical.getTicketOffices().isEmpty();
    boolean hasScraped = !scrapedOffices.isEmpty();

    if (hasManual && hasScraped) return TicketOfficeSource.MIXED;
    if (hasScraped) return TicketOfficeSource.SCRAPED;
    return TicketOfficeSource.MANUAL;
  }

  /**
   * 콘서트 예매처 데이터의 출처 계산
   *
   * @param concert 대상 콘서트 엔티티
   * @param scrapedOffices 스크래핑된 예매처 리스트
   * @return 데이터 출처 (MANUAL, SCRAPED, MIXED)
   */
  private TicketOfficeSource determineConcertSource(Concert concert, List<TicketOfficeScrapeResult> scrapedOffices) {
    boolean hasManual = !concertTicketOfficeRepository.findByConcert(concert).isEmpty()
            || !concert.getTicketOffices().isEmpty();
    boolean hasScraped = !scrapedOffices.isEmpty();

    if (hasManual && hasScraped) return TicketOfficeSource.MIXED;
    if (hasScraped) return TicketOfficeSource.SCRAPED;
    return TicketOfficeSource.MANUAL;
  }

  /**
   * KOPIS ID가 존재하는 모든 뮤지컬에 대해 예매처 정보 일괄 업데이트
   */
  @Transactional
  public void updateAllMusicalTicketOffices() {
    List<Musical> musicals = musicalRepository.findByKopisIdIsNotNull();

    log.info("뮤지컬 예매처 정보 업데이트 시작: 대상 개수={}", musicals.size());

    int successCount = 0;
    int failCount = 0;
    int skipCount = 0;

    for (Musical musical : musicals) {
      try {
        if(hasMusicalTicketOfficeData(musical)) {
          skipCount++;
          log.info("예매처 데이터가 이미 존재하여 건너뜀: ID={}, 제목={}", musical.getId(), musical.getTitle());
          continue;
        }
        updateMusicalTicketOffices(musical.getId());
        successCount++;
        Thread.sleep(3000); // 3초 대기

      } catch (Exception e) {
        failCount++;
        log.error("뮤지컬 예매처 업데이트 실패: ID={}, 에러={}",
                musical.getId(), e.getMessage());
      }
    }

    log.info("전체 뮤지컬 예매처 정보 업데이트 완료: 성공={}, 실패={}, 건너뜀={}", successCount, failCount, skipCount);
  }

  /**
   * 최근 15개의 뮤지컬에 대해 예매처 정보를 테스트 목적으로 업데이트
   */
  public void updateRecentMusicalTicketOffices() {
    LocalDateTime startTime = LocalDateTime.now();
    log.info("=== 예매처 데이터가 없는 최근 15개 예매처 정보 업데이트 시작 ===");
    log.info("테스트 시작시간: {}", startTime);

    Pageable pageable = PageRequest.of(0, 15, Sort.by("id").descending());
    Page<Musical> musicalPage = musicalRepository.findMusicalsWithoutTicketOffices(pageable);
    List<Musical> musicals = musicalPage.getContent();

    log.info("업데이트할 뮤지컬 컨텐츠 개수: {}", musicals.size());

    if(musicals.isEmpty()) {
      log.info("예매처 데이터가 없는 뮤지컬이 없습니다. 업데이트 종료.");
      return;
    }

    int successCount = 0;
    int failCount = 0;

    for(int i = 0; i < musicals.size(); i++) {
      Musical musical = musicals.get(i);
      try {
        log.info("뮤지컬 처리 중 [{}/{}]: ID={}, 제목={}",
                i + 1, musicals.size(), musical.getId(), musical.getTitle());

        updateMusicalTicketOffices(musical.getId());
        successCount++;

        log.info("뮤지컬 처리 완료 [{}/{}]: ID={}", i + 1, musicals.size(), musical.getId());
      } catch (Exception e) {
        failCount++;
        log.error("뮤지컬 예매처 업데이트 실패 [{}/{}]: Musical ID={}, 에러={}",
                i + 1, musicals.size(), musical.getId(), e.getMessage());
      }
    }
    LocalDateTime endTime = LocalDateTime.now();
    Duration duration = Duration.between(startTime, endTime);

    log.info("=== 예매처 데이터가 없는 최근 15개 뮤지컬 예매처 정보 업데이트 완료 ===");
    log.info("1. 완료시간: {}", endTime);
    log.info("2. 총 소요시간: {}초", duration.toSeconds());
    log.info("3. 성공: {}개, 실패: {}개", successCount, failCount);
  }

  /**
   * KOPIS ID가 존재하는 모든 콘서트에 대해 예매처 정보를 일괄 업데이트
   */
  @Transactional
  public void updateAllConcertTicketOffices() {
    List<Concert> concerts = concertRepository.findByKopisIdIsNotNull();

    log.info("전체 콘서트 예매처 정보 업데이트 시작: 대상 개수={}", concerts.size());

    int successCount = 0;
    int failCount = 0;
    int skipCount = 0;

    for(Concert concert : concerts) {
      try {
        if(hasConcertTicketOfficeData(concert)) {
          skipCount++;
          log.info("예매처 데이터가 이미 존재하여 건너뜀: ID={}, 제목={}", concert.getId(), concert.getTitle());
          continue;
        }
        updateConcertTicketOffices(concert.getId());
        successCount++;
        Thread.sleep(3000);

      } catch(Exception e) {
        failCount++;
        log.error("콘서트 예매처 업데이트 실패: ID={}, 에러={}",
                concert.getId(), e.getMessage());
      }
    }

    log.info("전체 콘서트 예매처 정보 업데이트 완료: 성공={}, 실패={}, 건너뜀={}", successCount, failCount, skipCount);
  }

  /**
   * 최신 15개의 콘서트에 대해 예매처 정보를 테스트 목적으로 업데이트
   */
  public void updateRecentConcertTicketOffices() {
    LocalDateTime startTime = LocalDateTime.now();
    log.info("=== 예매처 데이터가 없는 최근 15개 예매처 정보 업데이트 시작 ===");
    log.info("콘서트 예매처 테스트 시작시간: {}", startTime);

    Pageable pageable = PageRequest.of(0, 15, Sort.by("id").descending());
    Page<Concert> concertPage = concertRepository.findConcertsWithoutTicketOffices(pageable);
    List<Concert> concerts = concertPage.getContent();

    log.info("업데이트할 콘서트 컨텐츠 개수: {}", concerts.size());

    if(concerts.isEmpty()) {
      log.info("예매처 데이터가 없는 콘서트가 없습니다. 업데이트 종료.");
      return;
    }

    int successCount = 0;
    int failCount = 0;

    for(int i = 0; i < concerts.size(); i++) {
      Concert concert = concerts.get(i);
      try {
        log.info("콘서트 처리 중 [{}/{}]: ID={}, 제목={}",
                i + 1, concerts.size(), concert.getId(), concert.getTitle());

        updateConcertTicketOffices(concert.getId());
        successCount++;

        log.info("콘서트 처리 완료 [{}/{}]: ID={}", i + 1, concerts.size(), concert.getId());
      } catch (Exception e) {
        failCount++;
        log.error("콘서트 예매처 업데이트 실패 [{}/{}]: Concert ID={}, 에러={}",
                i + 1, concerts.size(), concert.getId(), e.getMessage());
      }
    }
    LocalDateTime endTime = LocalDateTime.now();
    Duration duration = Duration.between(startTime, endTime);

    log.info("=== 예매처 데이터가 없는 최근 15개 콘서트 예매처 정보 업데이트 완료 ===");
    log.info("1. 완료시간: {}", endTime);
    log.info("2. 총 소요시간: {}초", duration.toSeconds());
    log.info("3. 성공: {}개, 실패: {}개", successCount, failCount);
  }

  private boolean hasMusicalTicketOfficeData(Musical musical) {
    boolean hasJsonData = musical.getKopisTicketOffices() != null
            && !musical.getKopisTicketOffices().trim().isEmpty()
            && !musical.getKopisTicketOffices().equals("{}");

    boolean hasManualData = !musicalTicketOfficeRepository.findByMusical(musical).isEmpty();
    return hasJsonData || hasManualData;
  }

  private boolean hasConcertTicketOfficeData(Concert concert) {
    boolean hasJsonData = concert.getKopisTicketOffices() != null
            && !concert.getKopisTicketOffices().trim().isEmpty()
            && !concert.getKopisTicketOffices().equals("{}");

    boolean hasManualData = !concertTicketOfficeRepository.findByConcert(concert).isEmpty();

    return hasJsonData || hasManualData;
  }
}
