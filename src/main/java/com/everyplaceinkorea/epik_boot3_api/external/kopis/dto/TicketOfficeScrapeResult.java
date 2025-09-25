package com.everyplaceinkorea.epik_boot3_api.external.kopis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketOfficeScrapeResult {
  private String officeName;        // "interpark", "ticketlink" 등 (정규화된 이름)
//  private String displayName;       // "인터파크", "티켓링크" 등 (화면 표시용)
  private String ticketUrl;         // 실제 예매 URL
//  private String kopisId;           // 연관된 공연 KOPIS ID
//  private LocalDateTime scrapedAt;
}
