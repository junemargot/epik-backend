package com.everyplaceinkorea.epik_boot3_api.entity.concert;

import com.everyplaceinkorea.epik_boot3_api.admin.contents.concert.dto.ConcertUploadResultDto;
import com.everyplaceinkorea.epik_boot3_api.entity.common.DataSource;
import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import com.everyplaceinkorea.epik_boot3_api.entity.musical.Status;
import com.everyplaceinkorea.epik_boot3_api.entity.Region;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.dto.KopisPerformanceDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CurrentTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
@Setter
@Entity
@Table(name = "concert")
public class Concert {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "address", nullable = false)
  private String address;

  @Column(name = "venue", nullable = false)
  private String venue;

  @Column(name = "start_date", nullable = false, columnDefinition = "DATE NOT NULL")
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false, columnDefinition = "DATE NOT NULL")
  private LocalDate endDate;

//  @Column(name = "img_src", nullable = false)
//  private String imgSrc;

  @Column(name = "running_time")
  private String runningTime;

  @Column(name = "age_restriction")
  private String ageRestriction;

  @Column(name = "view_count")
  private Integer viewCount;

  @Column(name = "write_date")
  @CurrentTimestamp
  private LocalDateTime writeDate;

  @Column(name = "file_saved_name")
  private String fileSavedName;

  @Column(name = "file_path")
  private String filePath;

  @Column(name = "youtube_url")
  private String youtubeUrl;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id")
  private Member member;

  @Enumerated(EnumType.STRING)
  private Status status = Status.ACTIVE;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "region_id")
  private Region region;

  // KOPIS API 관련 필드
  @Enumerated(EnumType.STRING)
  @Column(name = "data_source")
  private DataSource dataSource = DataSource.MANUAL;

  @Column(name = "kopis_id", unique = true)
  private String kopisId;

  @Column(name = "last_synced")
  private LocalDateTime lastSynced;  // 마지막 동기화 시간

  // KOPIS 원본 데이터 보존 필드들
  @Column(name = "kopis_prfnm")
  private String kopisPrfnm; // KOPIS 공연명

  @Column(name = "kopis_fcltynm")
  private String kopisFcltynm; // KOPIS 공연장명

  @Column(name = "kopis_prfstate")
  private String kopisPrfstate; // KOPIS 공연상태

  @Column(name = "kopis_genrenm")
  private String kopisGenrenm; // KOPIS 장르명

  @Column(name = "kopis_area")
  private String kopisArea; // KOPIS 지역 정보

  @Column(name = "kopis_poster")
  private String kopisPoster; // KOPIS 포스터 이미지 URL

  // KOPIS 상세 정보 필드들
  @Column(name = "ticket_price", columnDefinition = "TEXT")
  private String ticketPrice; // 티켓 가격 정보

  @Column(name = "detail_images", columnDefinition = "TEXT")
  private String detailImages; // 상세 이미지 URL들 (JSON 배열 형태)

  @Column(name = "age_limit")
  private String ageLimit; // 관람연령

  public void addImage(ConcertUploadResultDto uploadResult) {
    this.filePath = uploadResult.getFilePath();
    this.fileSavedName = uploadResult.getFileSavedName();
  }

  /**
   * KOPIS 데이터로부터 Concert 엔티티 생성
   */
  public static Concert fromKopisData(KopisPerformanceDto dto, Region region, Member member) {
    log.info("=== fromKopisData 호출됨 ===");
    log.info("DTO 제목: [{}]", dto.getPrfnm());
    
    Concert concert = new Concert();

    // KOPIS 원본 데이터 보존
    concert.setKopisId(dto.getMt20id());
    concert.setKopisPrfnm(dto.getPrfnm());
    log.info("kopisPrfnm 설정됨: [{}]", concert.getKopisPrfnm());
    concert.setKopisFcltynm(dto.getFcltynm());
    concert.setKopisGenrenm(dto.getGenrenm());
    concert.setKopisPrfstate(dto.getPrfstate());
    concert.setKopisArea(dto.getArea());
    concert.setKopisPoster(dto.getPoster());

    // 제목 매핑
    log.info("cleanKopisTitle 호출 전: [{}]", dto.getPrfnm());
    String cleanTitle = cleanKopisTitle(dto.getPrfnm());
    log.info("cleanKopisTitle 호출 후: [{}]", cleanTitle);

    // 조건 없이 무조건 설정 (HTML 엔티티 디코딩 보장)
    if (cleanTitle != null && !cleanTitle.trim().isEmpty()) {
        concert.setTitle(cleanTitle);
    } else if (dto.getPrfnm() != null) {
        // cleanTitle이 비어있으면 원본을 직접 디코딩해서 사용
        String decodedOriginal = dto.getPrfnm()
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'");
        concert.setTitle(decodedOriginal);
    } else {
        concert.setTitle("제목 없음");
    }
    log.info("최종 title 설정: [{}]", concert.getTitle());

    // 공연장명 매핑
    concert.setVenue(isValidString(dto.getFcltynm()) ? dto.getFcltynm() : "공연장 정보 없음");
    // 주소 매핑
    concert.setAddress(buildDetailedAddress(dto.getArea(), dto.getFcltynm()));
    // 내용 생성
    concert.setContent(generateContent(dto));
    // 날짜 매핑
    concert.setStartDate(parseKopisDateSafely(dto.getPrfpdfrom()));
    concert.setEndDate(parseKopisDateSafely(dto.getPrfpdto()));
    // 메타데이터 설정
    concert.setDataSource(DataSource.KOPIS_API);
    concert.setLastSynced(LocalDateTime.now());
    concert.setRegion(region);
    concert.setMember(member);
    concert.setStatus(Status.ACTIVE);
    concert.setViewCount(0);
    concert.setDetailImages(dto.getStyurls());
    // 이미지 매핑
    setupImageData(concert, dto);
    // 추가 필드 설정
    concert.setRunningTime(determineRunningTime(dto));
    concert.setAgeRestriction(determineAgeRestriction(dto));

    System.out.println("=== fromKopisData 상세 이미지 확인 ===");
    System.out.println("styurls: " + dto.getStyurls());
    System.out.println("===================");


    return concert;
  }

  /**
   * 기존 Concert를 KOPIS 데이터로 업데이트
   */
  public void updateFromKopisData(KopisPerformanceDto dto) {
    log.info("=== updateFromKopisData 호출됨 ===");
    log.info("Concert 업데이트 시작: ID={}, KOPIS_ID={}", this.id, dto.getMt20id());
    log.info("DTO 제목: [{}]", dto.getPrfnm());

    // KOPIS 원본 데이터 업데이트
    this.kopisPrfnm = dto.getPrfnm();
    log.info("kopisPrfnm 설정: [{}]", this.kopisPrfnm);

    this.kopisFcltynm = dto.getFcltynm();
    this.kopisGenrenm = dto.getGenrenm();
    this.kopisPrfstate = dto.getPrfstate();
    this.kopisArea = dto.getArea();
    this.kopisPoster = dto.getPoster();

    this.setRunningTime(determineRunningTime(dto));
    this.setAgeRestriction(determineAgeRestriction(dto));

    // 가공된 데이터 업데이트
    log.info("cleanKopisTitle 호출 전 - DTO.getPrfnm(): [{}]", dto.getPrfnm());
    String newTitle = cleanKopisTitle(dto.getPrfnm());
    log.info("cleanKopisTitle 호출 후 - newTitle: [{}]", newTitle);

    // 조건 없이 무조건 설정 (HTML 엔티티 디코딩 보장)
    if (newTitle != null && !newTitle.trim().isEmpty()) {
        this.title = newTitle;
    } else if (dto.getPrfnm() != null) {
        // cleanTitle이 비어있으면 원본을 직접 디코딩해서 사용
        String decodedOriginal = dto.getPrfnm()
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'");
        this.title = decodedOriginal;
    }
    log.info("title 설정 완료: [{}]", this.title);

    if(isValidString(dto.getFcltynm())) {
      this.venue = dto.getFcltynm();
//      this.address = buildDetailedAddress(dto.getArea(), dto.getFcltynm());
    }

    // 컨텐츠 업데이트
    if(this.dataSource == DataSource.KOPIS_API) {
      this.content = generateContent(dto);
    }

    // 날짜 업데이트
    LocalDate newStartDate = parseKopisDateSafely(dto.getPrfpdfrom());
    LocalDate newEndDate = parseKopisDateSafely(dto.getPrfpdto());
    if(newStartDate != null) this.startDate = newStartDate;
    if(newEndDate != null) this.endDate = newEndDate;

    // 동기화 시간 갱신
    this.lastSynced = LocalDateTime.now();

    // 이미지 업데이트
    this.setDetailImages(dto.getStyurls());
    setupImageData(this, dto);
    // Concert.fromKopisData 또는 updateFromKopisData 메소드에 추가
    System.out.println("=== updateFromKopisData 상세 이미지 확인 ===");
    System.out.println("styurls: " + dto.getStyurls());
    System.out.println("===================");

    log.debug("Concert 업데이트 완료: title={}, venue={}", this.title, this.venue);

    // KOPIS 포스터를 기존 이미지 시스템에 연결
    if (dto.getPoster() != null && !dto.getPoster().trim().isEmpty()) {
      this.filePath = dto.getPoster();
      this.fileSavedName = extractFileNameFromUrl(dto.getPoster());
    }

    // 추가 필드 업데이트
//    this.runningTime = dto.getOpenrun() != null && dto.getOpenrun().equals("Y") ? "오픈런" : this.runningTime;
//    if (this.ageRestriction == null) {
//      this.ageRestriction = "전체 관람가";
//    }

  }

  /**
   * KOPIS 상세 정보로 엔티티 업데이트
   */
  public void updateFromKopisDetailData(KopisPerformanceDto dto) {
    log.debug("상세 정보 업데이트 시작: {}", this.title);

    // 공연시간 정보 업데이트
    if (isValidString(dto.getPrftime())) {
      // 러닝타임 정보 추출 및 업데이트
      String extractedTime = extractRunningTime(dto.getPrftime());
      if (extractedTime != null) {
        this.runningTime = extractedTime;
      }
    }

    // 티켓가격 정보 업데이트
    if (isValidString(dto.getPcseguidance())) {
      this.ticketPrice = dto.getPcseguidance();
    }

    // 상세 이미지 목록 업데이트
    if (isValidString(dto.getStyurls())) {
      this.detailImages = dto.getStyurls();
    }

    // 관람연령 업데이트 (개선된 로직)
    if (isValidString(dto.getPrfage())) {
      this.ageLimit = dto.getPrfage();
      this.ageRestriction = normalizeAgeRestriction(dto.getPrfage());
    }

    // 러닝타임 정보 추가 업데이트
    if (isValidString(dto.getPrfruntime()) && !isValidString(this.runningTime)) {
      this.runningTime = dto.getPrfruntime();
    }
  }

  /**
   * 공연시간 텍스트에서 러닝타임 추출
   */
  private String extractRunningTime(String performanceTime) {
    if (!isValidString(performanceTime)) {
      return null;
    }

    try {
      // "총 120분", "120분" 패턴
      java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(총\\s*)?(\\d+)분");
      java.util.regex.Matcher matcher = pattern.matcher(performanceTime);

      if (matcher.find()) {
        return matcher.group(2) + "분";
      }

      // "2시간 30분", "2시간" 패턴
      pattern = java.util.regex.Pattern.compile("(\\d+)시간\\s*(\\d+)?분?");
      matcher = pattern.matcher(performanceTime);

      if (matcher.find()) {
        int hours = Integer.parseInt(matcher.group(1));
        int minutes = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
        int totalMinutes = hours * 60 + minutes;
        return totalMinutes + "분";
      }

      // "약 90분" 패턴
      pattern = java.util.regex.Pattern.compile("약\\s*(\\d+)분");
      matcher = pattern.matcher(performanceTime);

      if (matcher.find()) {
        return matcher.group(1) + "분";
      }

    } catch (Exception e) {
      log.warn("러닝타임 추출 실패: {} - {}", performanceTime, e.getMessage());
    }

    return null;
  }

  /**
   * URL에서 파일명 추출
   */
  private static String extractFileNameFromUrl(String url) {
    if (!isValidString(url)) {
      return "default_poster.jpg";
    }

    try {
      // URL에서 파일명 부분 추출
      String fileName = url.substring(url.lastIndexOf("/") + 1);

      // 쿼리 파라미터 제거
      if (fileName.contains("?")) {
        fileName = fileName.substring(0, fileName.indexOf("?"));
      }

      // 확장자가 없으면 기본 확장자 추가
      if (!fileName.contains(".")) {
        fileName += ".jpg";
      }

      return fileName;
    } catch (Exception e) {
      log.warn("URL에서 파일명 추출 실패: {} - {}", url, e.getMessage());
      return "kopis_poster_" + System.currentTimeMillis() + ".jpg";
    }
  }

  /**
   * KOPIS 공연명 정리
   */
  private static String cleanKopisTitle(String title) {
    if (!isValidString(title)) {
      return null;
    }

    log.info("=== cleanKopisTitle 실행 ===");
    log.info("원본 제목: [{}]", title);
    log.info("원본에 &amp; 포함? {}", title.contains("&amp;"));
    log.info("원본에 & 포함? {}", title.contains("&"));

    String cleaned = title.trim()
            // HTML 엔티티 디코딩 먼저 수행
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            // 그 다음 문자열 정리
            .replaceAll("\\[.*?\\]", "")  // [대학로], [앵콜] 등 제거
            .replaceAll("\\(.*?재공연.*?\\)", "")  // (재공연) 제거
            .replaceAll("\\(.*?앵콜.*?\\)", "")   // (앵콜) 제거
            .replaceAll("\\s+", " ")      // 연속된 공백을 하나로
            .trim();

    log.info("정리 후 제목: [{}]", cleaned);
    log.info("정리 후 &amp; 포함? {}", cleaned.contains("&amp;"));
    log.info("정리 후 & 포함? {}", cleaned.contains("&"));
    log.info("=======================");

    return cleaned.isEmpty() ? title : cleaned;
  }

  /*
  * 콘텐츠 생성
  */
  private static String generateContent(KopisPerformanceDto dto) {
    StringBuilder content = new StringBuilder();

    // 기본 정보 (이모지 제거)
    if (isValidString(dto.getGenrenm())) {
      content.append("장르: ").append(dto.getGenrenm()).append("\n");
    }

    if (isValidString(dto.getPrfstate())) {
      content.append("공연상태: ").append(dto.getPrfstate()).append("\n");
    }

    if (isValidString(dto.getArea())) {
      content.append("지역: ").append(dto.getArea()).append("\n");
    }

    if (isValidString(dto.getFcltynm())) {
      content.append("공연장: ").append(dto.getFcltynm()).append("\n");
    }

    // 기본 메시지
    content.append("\nKOPIS에서 제공하는 공연 정보입니다.");

    return content.toString();
  }

  /**
   * 문자열 유효성 검사
   */
  private static boolean isValidString(String str) {
    return str != null && !str.trim().isEmpty() && !"".equals(str.trim());
  }

  /**
   * 상세 주소 생성 (지역 + 공연장명)
   */
  private static String buildDetailedAddress(String area, String fcltynm) {
    StringBuilder address = new StringBuilder();

    if (isValidString(area)) {
      address.append(area);
    }

    if (isValidString(fcltynm)) {
      if (address.length() > 0) {
        address.append(" ");
      }
      address.append(fcltynm);
    }

    return address.length() > 0 ? address.toString() : "주소 정보 없음";
  }

  /**
   * 안전한 날짜 파싱
   */
  private static LocalDate parseKopisDateSafely(String kopisDate) {
    if (!isValidString(kopisDate)) {
      log.warn("날짜 정보가 비어있습니다: {}", kopisDate);
      return LocalDate.now(); // 기본값으로 오늘 날짜 사용
    }

    try {
      String cleanDate = kopisDate.trim().replaceAll("[^0-9]", "");
      if (cleanDate.length() == 8) {
        return LocalDate.parse(cleanDate, DateTimeFormatter.ofPattern("yyyyMMdd"));
      } else {
        log.warn("날짜 형식이 올바르지 않습니다: {} -> {}", kopisDate, cleanDate);
        return LocalDate.now();
      }
    } catch(Exception e) {
      log.error("날짜 파싱 실패: {} - {}", kopisDate, e.getMessage());
      return LocalDate.now();
    }
  }

  private static void setupImageData(Concert concert, KopisPerformanceDto dto) {
    // KOPIS 포스터를 기존 이미지 시스템에 연결
    if (dto.getPoster() != null && !dto.getPoster().trim().isEmpty()) {
      concert.setFilePath(dto.getPoster()); // KOPIS 포스터 URL을 file_path에 저장
      concert.setFileSavedName(extractFileNameFromUrl(dto.getPoster())); // URL에서 파일명 추출

//       파일명 추출 (URL에서)
      String fileName = extractFileNameFromUrl(dto.getPoster());
      concert.setFileSavedName(fileName);
    }
  }

  /**
   * 러닝타임 결정
   */
  private static String determineRunningTime(KopisPerformanceDto dto) {
    // 기타 러닝타임 정보가 있다면 사용
    if (isValidString(dto.getPrfruntime())) {
      String originalTime = dto.getPrfruntime();
      String convertedTime = convertToMinutes(originalTime);

      // 로깅 추가
      System.out.println("=== 러닝타임 변환 ===");
      System.out.println("원본: " + originalTime);
      System.out.println("변환: " + convertedTime);
      System.out.println("==================");

      return convertedTime;
    }
    return null;
  }

  /**
   * 시간 표현을 분 단위로 변환
   * 예: "2시간" -> "120분", "1시간 30분" -> "90분", "90분" -> "90분"
   */
  private static String convertToMinutes(String timeStr) {
    if (timeStr == null || timeStr.trim().isEmpty()) {
      return null;
    }

    String cleanTime = timeStr.trim().replaceAll("약\\s*", ""); // "약" 제거
    int totalMinutes = 0;

    try {
      System.out.println("변환 처리 중: " + cleanTime); // 디버그 로그

      // "2시간 30분" 형태 파싱
      if (cleanTime.contains("시간") && cleanTime.contains("분")) {
        String[] parts = cleanTime.split("시간");
        if (parts.length >= 2) {
          // 시간 부분
          String hourPart = parts[0].trim().replaceAll("[^0-9]", "");
          if (!hourPart.isEmpty()) {
            totalMinutes += Integer.parseInt(hourPart) * 60;
            System.out.println("시간 파트: " + hourPart + " -> " + (Integer.parseInt(hourPart) * 60) + "분");
          }

          // 분 부분
          String minutePart = parts[1].trim().replaceAll("[^0-9]", "");
          if (!minutePart.isEmpty()) {
            totalMinutes += Integer.parseInt(minutePart);
            System.out.println("분 파트: " + minutePart + "분");
          }
        }
      }
      // "2시간" 형태 파싱
      else if (cleanTime.contains("시간")) {
        String hourPart = cleanTime.replaceAll("[^0-9]", "");
        if (!hourPart.isEmpty()) {
          totalMinutes = Integer.parseInt(hourPart) * 60;
          System.out.println("시간만: " + hourPart + " -> " + totalMinutes + "분");
        }
      }
      // "90분" 형태 파싱
      else if (cleanTime.contains("분")) {
        String minutePart = cleanTime.replaceAll("[^0-9]", "");
        if (!minutePart.isEmpty()) {
          totalMinutes = Integer.parseInt(minutePart);
          System.out.println("분만: " + minutePart + "분");
        }
      }
      // 숫자만 있는 경우 (분으로 가정)
      else if (cleanTime.matches("\\d+")) {
        totalMinutes = Integer.parseInt(cleanTime);
        System.out.println("숫자만: " + cleanTime + "분");
      }

      String result = totalMinutes > 0 ? totalMinutes + "분" : null;
      System.out.println("최종 결과: " + result);
      return result;

    } catch (NumberFormatException e) {
      System.out.println("변환 실패, 원본 반환: " + timeStr);
      // 파싱 실패시 원본 반환
      return timeStr;
    }
  }

  /**
   * 연령 제한 결정
   */
  private static String determineAgeRestriction(KopisPerformanceDto dto) {
    if (isValidString(dto.getPrfage())) {
      return normalizeAgeRestriction(dto.getPrfage());
    }
    return "전체 관람가"; // 기본값
  }

  /**
   * 연령 제한 정규화
   */
  private static String normalizeAgeRestriction(String ageInfo) {
    if (!isValidString(ageInfo)) {
      return "전체 관람가";
    }

    String normalized = ageInfo.toLowerCase().trim();

    // 일반적인 패턴 매핑
    if (normalized.contains("전체") || normalized.contains("all")) {
      return "전체 관람가";
    } else if (normalized.contains("12")) {
      return "12세 이상 관람가";
    } else if (normalized.contains("15")) {
      return "15세 이상 관람가";
    } else if (normalized.contains("18") || normalized.contains("19")) {
      return "19세 이상 관람가";
    }

    // 원본 그대로 반환
    return ageInfo;
  }
}
