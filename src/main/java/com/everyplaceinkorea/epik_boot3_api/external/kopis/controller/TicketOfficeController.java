package com.everyplaceinkorea.epik_boot3_api.external.kopis.controller;

import com.everyplaceinkorea.epik_boot3_api.external.kopis.dto.TicketOfficeScrapeResult;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.scraper.KopisWebScraperService;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.service.TicketOfficeSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/ticket")
@RequiredArgsConstructor
public class TicketOfficeController {

  private final KopisWebScraperService scraperService;
  private final TicketOfficeSyncService jsonService;

  /**
   * 특정 KOPIS ID의 예매처 정보 스크래핑 테스트
   * @param kopisId 스크래핑할 공연의 KOPIS ID. URL 경로 변수로 전달
   * @return 성공 시, 해당 공연의 예매처 정보 목록과 상태를 반환
   */
  @GetMapping("/scrape/{kopisId}")
  public ResponseEntity<List<TicketOfficeScrapeResult>> testScraping(@PathVariable String kopisId) {
    try {
      List<TicketOfficeScrapeResult> result = scraperService.scrapeTicketOffices(kopisId);
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      return ResponseEntity.status(500).body(null);
    }
  }

  /**
   * 특정 뮤지컬의 예매처 정보 JSON 업데이트
   */
  @PostMapping("/musical/{musicalId}/update")
  public ResponseEntity<String> updateMusicalTicketOffices(@PathVariable Long musicalId) {
    try {
      jsonService.updateMusicalTicketOffices(musicalId);
      return ResponseEntity.ok("뮤지컬 예매처 정보 업데이트 완료");
    } catch (Exception e) {
      return ResponseEntity.status(500).body("업데이트 실패: " + e.getMessage());
    }
  }

  /**
   * 모든 뮤지컬 예매처 정보 업데이트 (주의: 시간이 오래 걸림)
   */
  @PostMapping("/musical/update-all")
  public ResponseEntity<String> updateAllMusicalTicketOffices() {
    try {
      // 비동기 처리를 위해 별도 스레드에서 실행
      new Thread(() -> jsonService.updateAllMusicalTicketOffices()).start();
      return ResponseEntity.ok("모든 뮤지컬 예매처 정보 업데이트 시작됨 (백그라운드 실행)");
    } catch (Exception e) {
      return ResponseEntity.status(500).body("업데이트 시작 실패: " + e.getMessage());
    }
  }

  /**
   * 특정 콘서트의 예매처 정보 JSON 업데이트
   */
  @PostMapping("/concert/{concertId}/update")
  public ResponseEntity<String> updateConcertTicketOffices(@PathVariable Long concertId) {
    try {
      jsonService.updateConcertTicketOffices(concertId);
      return ResponseEntity.ok("콘서트 예매처 정보 업데이트 완료");
    } catch (Exception e) {
      return ResponseEntity.status(500).body("업데이트 실패: " + e.getMessage());
    }
  }

  /**
   * 모든 콘서트 예매처 정보 업데이트 (주의: 시간이 오래 걸림)
   */
  @PostMapping("/concert/update-all")
  public ResponseEntity<String> updateAllConcertTicketOffices() {
    try {
      // 비동기 처리를 위해 별도 스레드에서 실행
      new Thread(() -> jsonService.updateAllConcertTicketOffices()).start();
      return ResponseEntity.ok("모든 콘서트 예매처 정보 업데이트 시작됨 (백그라운드 실행)");
    } catch (Exception e) {
      return ResponseEntity.status(500).body("업데이트 시작 실패: " + e.getMessage());
    }
  }

  /**
   * 최근 5개 뮤지컬 예매처 정보 업데이트 (테스트용)
   */
  @PostMapping("/musical/update-recent")
  public ResponseEntity<String> updateRecentMusicalTicketOffices() {
    try {
      // 비동기 처리를 위해 별도 스레드에서 실행
      new Thread(() -> jsonService.updateRecentMusicalTicketOffices()).start();
      return ResponseEntity.ok("최근 5개 뮤지컬 예매처 정보 업데이트 시작됨 (백그라운드 실행)");
    } catch (Exception e) {
      return ResponseEntity.status(500).body("업데이트 시작 실패: " + e.getMessage());
    }
  }

  @PostMapping("/concert/update-recent")
  public ResponseEntity<String> updateRecentConcertTicketOffices() {
    try {
      new Thread(jsonService::updateRecentConcertTicketOffices).start();
      return ResponseEntity.ok("최근 5개 콘서트 예매처 정보 업데이트 시작됨 (백그라운드 실행)");
    } catch (Exception e) {
      return ResponseEntity.status(500).body("업데이트 시작 실패: " + e.getMessage());
    }
  }
}
