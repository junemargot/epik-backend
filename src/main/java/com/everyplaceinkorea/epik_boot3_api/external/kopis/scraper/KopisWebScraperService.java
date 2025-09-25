package com.everyplaceinkorea.epik_boot3_api.external.kopis.scraper;

import com.everyplaceinkorea.epik_boot3_api.external.kopis.dto.TicketOfficeScrapeResult;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.TimeoutException;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KopisWebScraperService {

  public KopisWebScraperService() {
    // Chrome WebDriver 자동 관리 설정
    WebDriverManager.chromedriver().setup();
  }

  /**
   * KOPIS ID로 예매처 정보를 스크래핑
   *
   * @param kopisId
   * @return 예매처 정보 리스트
   */
  public List<TicketOfficeScrapeResult> scrapeTicketOffices(String kopisId) {
    if(kopisId == null || kopisId.isBlank()) {
      log.warn("kopisId가 비어있어 스크래핑을 건너뜁니다.");
      return Collections.emptyList();
    }
    WebDriver driver = null;

    try {
      driver = createWebDriver();
      return performScraping(driver, kopisId);

    } catch (Exception e) {
      log.error("스크래핑 실패 - KOPIS ID: {}, 에러: {}", kopisId, e.getMessage(), e);
      return Collections.emptyList();

    } finally {
      if (driver != null) {
        try {
          driver.quit();
        } catch (Exception e) {
          log.warn("WebDriver 종료 실패: {}", e.getMessage());
        }
      }
    }
  }

  /**
   * WebDriver 인스턴스를 생성하고 옵션을 설정
   *
   * @return 설정된 Chrome WebDriver 인스턴스
   */
  private WebDriver createWebDriver() {
    ChromeOptions options = new ChromeOptions();

    // 헤드리스 모드 (서버 환경 필수)
    options.addArguments("--headless");
    // 성능 최적화 옵션들
    options.addArguments("--no-sandbox");
    options.addArguments("--disable-dev-shm-usage");
    options.addArguments("--disable-gpu");
    options.addArguments("--disable-extensions");
    options.addArguments("--disable-images");
    options.addArguments("--disable-javascript");
    // User Agent 설정 (봇 탐지 회피)
    options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
    // 메모리 사용량 제한
    options.addArguments("--memory-pressure-off");
    options.addArguments("--max_old_space_size=4096");

    return new ChromeDriver(options);
  }

  /**
   * 실제 스크래핑 수행
   *
   * @param driver 생성된 WebDriver
   * @param kopisId
   * @return 예매처 정보 리스트
   */
  private List<TicketOfficeScrapeResult> performScraping(WebDriver driver, String kopisId) {
    // KOPIS 상세페이지 URL 생성
    String url = String.format("https://kopis.or.kr/por/db/pblprfr/pblprfrView.do?mt20Id=%s", kopisId);
    log.info("상세페이지 스크래핑 시작 - URL: {}", url);

    // 페이지 로드
    driver.get(url); // 대상 페이지로 이동 (동기적 호출, 네비게이션 완료까지 대기)
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15)); // 명시적 대기 헬퍼 생성 (최대 15초)

    try {
      // 예매처 바로가기 버튼 찾기 및 클릭
      WebElement ticketButton = findTicketButton(driver, wait);

      if(ticketButton != null) {
        log.info("예매처 바로가기 버튼 발견, 클릭 시도");
        ticketButton.click();

        // 팝업 대기 후 링크 추출
        waitForPopupToLoad(driver, wait);
        return extractLinksFromPopup(driver, wait, kopisId);
      } else {
        log.warn("예매처 바로가기 버튼을 찾을 수 없음");
        // 대안: 페이지에서 직접 예매 링크 찾기
        return Collections.emptyList();
      }
    } catch(Exception e) {
      log.warn("팝업 처리 실패, 페이지 직접 검색으로 전환: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * 여러 XPath 셀렉터로 예매처 버튼 탐색
   *
   * @param driver WebDriver
   * @param wait WebDriverWait
   * @return 발견된 버튼 WebElement
   */
  private WebElement findTicketButton(WebDriver driver, WebDriverWait wait) {
    String[] buttonSelectors = {
            "//button[contains(@class, 'DBDetail_bt_st3__sg03__')]",
            "//button[contains(text(), '예매처바로가기')]",
            "//a[contains(text(), '예매처바로가기')]",
            "//button[contains(text(), '예매처 바로가기')]",
            "//a[contains(text(), '예매처 바로가기')]",
            "//button[contains(text(), '예매')]",
            "//a[contains(text(), '예매')]",
            "//button[contains(@class, 'ticket')]",
            "//a[contains(@class, 'ticket')]",
            "//*[@id='btnTicket']",
            "//*[@class*='btn-ticket']"
    };

    for(String selector : buttonSelectors) {
      try {
        List<WebElement> elements = driver.findElements(By.xpath(selector));
        if(!elements.isEmpty()) {
          WebElement button = elements.get(0);
          if(button.isDisplayed() & button.isEnabled()) {
            log.debug("버튼 탐색 - 셀렉터: {}, 텍스트: '{}'", selector, button.getText());
            return button;
          }
        }
      } catch (Exception e) {
        log.debug("셀렉터 '{} 추출 시도 실패: {}", selector, e.getMessage());
      }
    }

    return null;
  }

  /**
   * 팝업에서 예매처 링크 추출
   *
   * @param driver WebDriver
   * @param wait WebDriverWait
   * @param kopisId KOPIS 공연 ID
   * @return 예매처 링크 리스트
   */
  private List<TicketOfficeScrapeResult> extractLinksFromPopup(WebDriver driver, WebDriverWait wait, String kopisId) {
    try {
      // 예매처 팝업 요소 찾기
      WebElement popup = findPopupElement(driver, wait);

      if(popup != null) {
        log.info("예매처 팝업 발견, 예매처 링크 추출 시작");
        return extractTicketLinksInPage(popup, kopisId);
      } else {
        log.warn("예매처 팝업을 찾을 수 없음");
        return Collections.emptyList();
      }
    } catch(Exception e) {
      log.error("예매처 팝업에서 링크 추출 실패: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * 팝업 컨테이너 탐색
   *
   * @param driver WebDrvier
   * @param wait WebDrvierWait
   * @return 팝업 WebElement
   */
  private WebElement findPopupElement(WebDriver driver, WebDriverWait wait) {
    String[] popupSelectors = {
            "//div[contains(@class, 'DBDetail_layerPopCon__MFjyU')]", // 팝업 컨텐츠
            "//div[contains(@class, 'DBDetail_layerPop__CZOAF')]",    // 팝업 전체
            "//div[contains(@class, 'layerPop')]",                    // 일반적인 팝업
            "//div[contains(@class, 'popup')]"                        // 백업
    };

    for(String selector : popupSelectors) {
      try {
        WebElement popup = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.xpath(selector))
        );

        if(popup.isDisplayed()) {
          log.debug("예매처 팝업 발견 - 셀렉터: {}", selector);
          return popup;
        }
      } catch(Exception e) {
        log.debug("예매처 팝업 셀렉터 '{}' 시도 실패: {}", selector, e.getMessage());
      }
    }

    return null;
  }

  /**
   * 주어진 컨테이너에서 예매처 링크 추출
   *
   * @param container 팝업 또는 페이지 컨테이너
   * @param kopisId KOPIS 공연 ID
   * @return 예매처 정보 리스트
   */
  private List<TicketOfficeScrapeResult> extractTicketLinksInPage(WebElement container, String kopisId) {
    log.info("예매처 링크 추출 시작 - 컨테이너: {}", container.getTagName());

    List<WebElement> links = new ArrayList<>();

    // 직접 경로로 찾기
    try {
      links = container.findElements(By.xpath(".//div[contains(@class, 'DBDetail_btnType01_wrap__R1hJu')]//p//a"));
    } catch (Exception e) {
      log.debug("구체적 경로 실패, 일반 a 태그로 대체");
      links = container.findElements(By.tagName("a"));
    }

    log.info("찾은 링크 개수: {}", links.size());

    // 모든 링크 정보 로깅
    for (int i = 0; i < links.size(); i++) {
      try {
        WebElement link = links.get(i);
        String href = link.getAttribute("href");
        String text = link.getText();
        log.debug("링크 #{}: href='{}', text='{}'", i + 1, href, text);

      } catch (Exception e) {
        log.warn("링크 #{} 정보 추출 실패: {}", i + 1, e.getMessage());
      }
    }
    
    List<TicketOfficeScrapeResult> results = links.stream()
            .map(link -> toScrapeResult(link, kopisId))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
            
    log.info("최종 추출된 예매처 개수: {}", results.size());
    return results;
  }

  /**
   * 링크 요소를 도메인 객체로 변환 (WebElements로부터 TicketOfficeInfoDto 생성)
   *
   * @param linkElement WebElement 링크
   * @param kopisId KOPIS 공연 ID
   * @return TicketOfficeScrapeResult
   */
  private TicketOfficeScrapeResult toScrapeResult(WebElement linkElement, String kopisId) {
    try {
      String href = linkElement.getAttribute("href");
      String text = linkElement.getText().trim();
      
      log.debug("링크 처리 중 - href: '{}', text: '{}'", href, text);

      if(href == null || href.trim().isEmpty()) {
        log.debug("링크가 없어서 건너뜀 - text: '{}'", text);
        return null;
      }

      String[] names = normalizeOfficeName(text, href);
      String officeName = names[0];  // 정규화된 이름
      String displayName = names[1]; // 화면 표시용 이름

      log.info("예매처 정보 생성 성공 - 이름: '{}', URL: '{}'", displayName, href);

      return TicketOfficeScrapeResult.builder()
              .officeName(officeName)
//              .displayName(displayName)
              .ticketUrl(href)
//              .kopisId(kopisId)
//              .scrapedAt(LocalDateTime.now())
              .build();

    } catch(Exception e) {
      log.warn("링크 처리 중 오류: {}", e.getMessage());
      return null;
    }
  }

  /**
   * 링크 텍스트와 URL을 기준으로 예매처 이름 정규화
   *
   * @param text 링크 텍스트
   * @param href 링크 URL
   * @return [정규화명, 표시명]
   */
  private String[] normalizeOfficeName(String text, String href) {
    // 텍스트가 있으면 그대로 사용
    if(text != null && !text.trim().isEmpty() &&
      !text.equals("예매") && !text.equals("티켓") && !text.matches("^\\d+$")) {

      String displayName = text.trim();
      String normalizedName = text.toLowerCase().replaceAll("[^a-z0-9가-힣]", "");
      return new String[] { normalizedName, displayName };
    }

    // 텍스트가 없거나 의미 없는 경우에만 URL에서 추출
    if(href != null) {
      String displayName = extractDisplayNameFromUrl(href);
      String normalizedName = displayName.toLowerCase().replaceAll("[^a-z0-9가-힣]", "");
      return new String[] { normalizedName, displayName };
    }

    return new String[] {"other", "기타 예매처"};
  }

  /**
   * URL에서 예매처 표시명 추출
   *
   * @param href 링크 URL
   * @return 예매처 표시명
   */
  private String extractDisplayNameFromUrl(String href) {
    if (href.contains("interpark.com")) return "인터파크";
    if (href.contains("yes24.com")) return "YES24";
    if (href.contains("11st.co.kr")) return "11번가";
    if (href.contains("ticketlink.co.kr")) return "티켓링크";
    if (href.contains("booking.naver.com")) return "네이버예약";
    if (href.contains("melon.com")) return "멜론티켓";
    if (href.contains("timeticket.co.kr")) return "타임티켓";

    // 도메인에서 추출
    try {
      String domain = new URL(href).getHost();
      return domain.replace("www.", "");
    } catch (Exception e) {
      return "기타 예매처";
    }
  }

  private void waitForPopupToLoad(WebDriver driver, WebDriverWait wait) {
    try {
      // 팝업이 실제로 나타나고 안정화 대기
      wait.until(ExpectedConditions.and(
              ExpectedConditions.presenceOfElementLocated(
                      By.xpath("//div[contains(@class, 'DBDetail_layerPopCon__MFjyU')]")
              ),
              ExpectedConditions.elementToBeClickable(
                      By.xpath("//div[contains(@class, 'DBDetail_layerPopCon__MFjyU')]//a")
              )
      ));

      // 추가로 DOM이 완전히 렌더링될 때까지 대기
      wait.until(webDriver ->
              ((JavascriptExecutor) webDriver).executeScript(
                      "return document.readyState === 'complete' && " +
                              "document.querySelectorAll('.DBDetail_layerPopCon__MFjyU a').length > 0"
              ).equals(true)
      );

      log.debug("팝업 로딩 완료 확인");

    } catch (TimeoutException e) {
      log.warn("팝업 로딩 대기 시간 초과, 계속 진행: {}", e.getMessage());
    }
  }
}
