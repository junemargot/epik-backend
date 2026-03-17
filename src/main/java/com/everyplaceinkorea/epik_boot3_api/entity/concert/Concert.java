package com.everyplaceinkorea.epik_boot3_api.entity.concert;

import com.everyplaceinkorea.epik_boot3_api.admin.contents.concert.dto.ConcertUploadResultDto;
import com.everyplaceinkorea.epik_boot3_api.entity.Facility;
import com.everyplaceinkorea.epik_boot3_api.entity.Hall;
import com.everyplaceinkorea.epik_boot3_api.entity.common.DataSource;
import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import com.everyplaceinkorea.epik_boot3_api.entity.musical.Status;
import com.everyplaceinkorea.epik_boot3_api.entity.Region;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.dto.KopisPerformanceDto;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.enums.TicketOfficeSource;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.utils.JsonUtils;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.utils.KopisDataUtils;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CurrentTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id")
  private Member member;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "region_id")
  private Region region;

  @Enumerated(EnumType.STRING)
  private Status status = Status.ACTIVE;

  @Enumerated(EnumType.STRING)
  @Column(name = "data_source")
  private DataSource dataSource = DataSource.MANUAL;

  @Column(name = "last_synced")
  private LocalDateTime lastSynced;

  // === KOPIS 관련 필드 ===
  @Column(name = "kopis_id", unique = true)
  private String kopisId;

  @Column(name = "kopis_prfnm")
  private String kopisPrfnm; // KOPIS 공연명

  @Column(name = "kopis_prfstate")
  private String kopisPrfstate; // KOPIS 공연상태

  @Column(name = "kopis_genrenm")
  private String kopisGenrenm; // KOPIS 장르명

  @Column(name = "kopis_fcltynm")
  private String kopisFcltynm; // KOPIS 공연장명

  @Column(name = "kopis_area")
  private String kopisArea; // KOPIS 지역 정보

  @Column(name = "kopis_poster")
  private String kopisPoster; // KOPIS 포스터 이미지 URL

  @Column(name = "kopis_child")
  private String kopisChild;

  @Column(name = "kopis_visit")
  private String kopisVisit;

  @Column(name = "ticket_price", columnDefinition = "TEXT")
  private String ticketPrice; // 티켓 가격 정보

  @Column(name = "detail_images", columnDefinition = "TEXT")
  private String detailImages; // 상세 이미지 URL들 (JSON 배열 형태)

  @Column(name = "kopis_ticket_offices", columnDefinition = "JSON")
  private String kopisTicketOffices;

  @Column(name = "kopis_ticket_offices_updated_at")
  private LocalDateTime kopisTicketOfficesUpdatedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "kopis_ticket_offices_source")
  private TicketOfficeSource kopisTicketOfficesSource = TicketOfficeSource.MANUAL;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "facility_id")
  private Facility facility;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hall_id")
  private Hall hall;

  public void addImage(ConcertUploadResultDto uploadResult) {
    this.filePath = uploadResult.getFilePath();
    this.fileSavedName = uploadResult.getFileSavedName();
  }

  /**
   * KOPIS 데이터로부터 Concert 엔티티 생성
   */
  public static Concert fromKopisData(KopisPerformanceDto dto, Region region, Member member) {
    log.info("=== Concert.fromKopisData 호출됨 - 제목: [{}] ===", dto.getPrfnm());

    Concert concert = new Concert();

    setKopisOriginalData(concert, dto);
    setBasicInformation(concert, dto);
    setDateInformation(concert, dto);
    setMetadataAndRelations(concert, region, member);
    setAdditionalInformation(concert, dto);

    log.info("=== Concert 생성 완료: title=[{}] ===", dto.getPrfnm());

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
    this.kopisFcltynm = dto.getFcltynm();
    this.kopisGenrenm = dto.getGenrenm();
    this.kopisPrfstate = dto.getPrfstate();
    this.kopisArea = dto.getArea();
    this.kopisPoster = dto.getPoster();
    this.kopisChild = dto.getChild();
    this.kopisVisit = dto.getVisit();

    this.setRunningTime(determineRunningTime(dto));
    this.setAgeRestriction(dto.getPrfage());

    // 가공된 데이터 업데이트
    this.title = cleanKopisTitle(dto.getPrfnm());
    this.venue = dto.getFcltynm();
    this.content = generateContent(dto);


    // 날짜 업데이트
    this.startDate = parseKopisDateSafely(dto.getPrfpdfrom()) != null
            ? parseKopisDateSafely(dto.getPrfpdfrom())
            : this.startDate; // 기존 값 유지
    this.endDate = parseKopisDateSafely(dto.getPrfpdto()) != null
            ? parseKopisDateSafely(dto.getPrfpdto())
            : this.endDate; // 기존 값 유지

    // 동기화 시간 갱신
    this.lastSynced = LocalDateTime.now();

    // KOPIS 포스터를 기존 이미지 시스템에 연결
    if (dto.getPoster() != null && !dto.getPoster().trim().isEmpty()) {
      this.filePath = dto.getPoster();
      this.fileSavedName = extractFileNameFromUrl(dto.getPoster());
    }

    log.debug("Concert 업데이트 완료: title={}, venue={}", this.title, this.venue);
  }

  /**
   * KOPIS 상세 정보로 엔티티 업데이트
   */
  public void updateFromKopisDetailData(KopisPerformanceDto dto) {
    log.debug("상세 정보 업데이트 시작: {}", this.title);

    // 공연시간 정보 업데이트
    if (isValidString(dto.getPrftime())) {
      // 러닝타임 정보 추출 및 업데이트
      String extractedTime = KopisDataUtils.extractRunningTime(dto.getPrftime());
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

    // 러닝타임 정보 추가 업데이트
    if (isValidString(dto.getPrfruntime()) && !isValidString(this.runningTime)) {
      this.runningTime = dto.getPrfruntime();
    }
  }

  private static void setupImageData(Concert concert, KopisPerformanceDto dto) {
    // KOPIS 포스터를 기존 이미지 시스템에 연결
    if (dto.getPoster() != null && !dto.getPoster().trim().isEmpty()) {
      concert.setFilePath(dto.getPoster()); // KOPIS 포스터 URL을 file_path에 저장
      concert.setFileSavedName(extractFileNameFromUrl(dto.getPoster())); // URL에서 파일명 추출

      String fileName = extractFileNameFromUrl(dto.getPoster());
      concert.setFileSavedName(fileName);
    }
  }

  // 파일명 생성
  private static String extractFileNameFromUrl(String url) {
    return KopisDataUtils.generateFileName(url);
  }

  // 공연명 생성
  private static String cleanKopisTitle(String title) {
    return KopisDataUtils.normalizeTitle(title);
  }

  // 상세정보 내용 생성
  private static String generateContent(KopisPerformanceDto dto) {
    return KopisDataUtils.generateContent(dto);
  }

  // 주소 생성
  private static String buildDetailedAddress(String area, String fcltynm) {
    return KopisDataUtils.generateAddress(area, fcltynm);
  }

  // 날짜 변환
  private static LocalDate parseKopisDateSafely(String kopisDate) {
    return KopisDataUtils.parseDate(kopisDate);
  }

  // 러닝타임 생성
  private static String determineRunningTime(KopisPerformanceDto dto) {
    return KopisDataUtils.determineRunningTime(dto);
  }

  // 문자열 유효성 검사
  private static boolean isValidString(String str) {
    return KopisDataUtils.isValidString(str);
  }

  /**
   * 1. KOPIS 원본 데이터 저장
   */
  private static void setKopisOriginalData(Concert concert, KopisPerformanceDto dto) {
    concert.setKopisId(dto.getMt20id());
    concert.setKopisPrfnm(dto.getPrfnm());
    concert.setKopisFcltynm(dto.getFcltynm());
    concert.setKopisGenrenm(dto.getGenrenm());
    concert.setKopisPrfstate(dto.getPrfstate());
    concert.setAgeRestriction(dto.getPrfage());
    concert.setKopisArea(dto.getArea());
    concert.setKopisPoster(dto.getPoster());
    concert.setKopisChild(dto.getChild());
    concert.setKopisVisit(dto.getVisit());

    log.debug("KOPIS 원본 데이터 설정 완료: ID=[{}]", dto.getMt20id());
  }

  /**
   * 2. 핵심 정보 매핑 (제목, 공연장, 주소, 내용)
   */
  private static void setBasicInformation(Concert concert, KopisPerformanceDto dto) {
    String cleanTitle = cleanKopisTitle(dto.getPrfnm());
    concert.setTitle(KopisDataUtils.normalizeTitle(cleanTitle));

    concert.setVenue(isValidString(dto.getFcltynm()) ? dto.getFcltynm() : "공연장 정보 없음");
    concert.setAddress(buildDetailedAddress(dto.getArea(), dto.getFcltynm()));
    concert.setContent(generateContent(dto));
    log.debug("기본 정보 설정 완료: title=[{}], venue=[{}]", concert.getTitle(), concert.getVenue());
  }

  /**
   * 3. 날짜 정보 설정
   */
  private static void setDateInformation(Concert concert, KopisPerformanceDto dto) {
    concert.setStartDate(KopisDataUtils.parseDate(dto.getPrfpdfrom()));
    concert.setEndDate(KopisDataUtils.parseDate(dto.getPrfpdto()));

    log.debug("날짜 정보 설정 완료: {} ~ {}", concert.getStartDate(), concert.getEndDate());
  }

  /**
   * 4. 메타데이터 및 관계 설정
   */
  private static void setMetadataAndRelations(Concert concert, Region region, Member member) {
    concert.setDataSource(DataSource.KOPIS_API);
    concert.setLastSynced(LocalDateTime.now());
    concert.setRegion(region);
    concert.setMember(member);
    concert.setStatus(Status.ACTIVE);
    concert.setViewCount(0);

    log.debug("메타데이터 설정 완료: region=[{}], member=[{}]",
            region != null ? region.getId() : null,
            member != null ? member.getId() : null);
  }

  /**
   * 5. 이미지 및 부가 정보 설정
   */
  private static void setAdditionalInformation(Concert concert, KopisPerformanceDto dto) {
    concert.setDetailImages(dto.getStyurls());
    concert.setTicketPrice(dto.getPcseguidance());
    setupImageData(concert, dto);

    concert.setRunningTime(determineRunningTime(dto));

    log.debug("부가 정보 설정 완료: runningTime=[{}]", concert.getRunningTime());
  }

  // === JSON 헬퍼 메서드들 ===

  /**
   * 예매처 정보를 Map으로 변환
   */
  @Transient
  public Map<String, String> getTicketOffices() {
    return JsonUtils.fromJsonToMap(kopisTicketOffices);
  }

  /**
   * 예매처 정보 설정
   */
  public void setTicketOffices(Map<String, String> offices) {
    this.kopisTicketOffices = JsonUtils.toJson(offices);
    this.kopisTicketOfficesUpdatedAt = LocalDateTime.now();
  }
}
