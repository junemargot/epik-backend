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
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CurrentTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

  @Column(name = "kopisPrfstate")
  private String kopisPrfstate; // KOPIS 공연상태

  @Column(name = "kopis_genrenm")
  private String kopisGenrenm; // KOPIS 장르명

  @Column(name = "kopis_area")
  private String kopisArea; // KOPIS 지역 정보

  @Column(name = "kopis_poster")
  private String kopisPoster; // KOPIS 포스터 이미지 URL

  public void addImage(ConcertUploadResultDto uploadResult) {
    this.filePath = uploadResult.getFilePath();
    this.fileSavedName = uploadResult.getFileSavedName();
  }

  /**
   * KOPIS 데이터로부터 Concert 엔티티 생성
   */
  public static Concert fromKopisData(KopisPerformanceDto dto, Region region, Member member) {
    Concert concert = new Concert();

    // KOPIS 원본 데이터 보존
    concert.setKopisId(dto.getMt20id());
    concert.setKopisPrfnm(dto.getPrfnm());
    concert.setKopisFcltynm(dto.getFcltynm());
    concert.setKopisGenrenm(dto.getGenrenm());
    concert.setKopisPrfstate(dto.getPrfstate());
    concert.setKopisArea(dto.getArea());
    concert.setKopisPoster(dto.getPoster());

    // 가공된 데이터 설정
    concert.setTitle(cleanKopisTitle(dto.getPrfnm()));
    concert.setVenue(dto.getFcltynm());
    concert.setContent(generateKopisContent(dto));
    concert.setAddress("");

    // 날짜 변환
    concert.setStartDate(parseKopisDate(dto.getPrfpdfrom()));
    concert.setEndDate(parseKopisDate(dto.getPrfpdto()));

    // 메타데이터 설정
    concert.setDataSource(DataSource.KOPIS_API);
    concert.setLastSynced(LocalDateTime.now());
    concert.setRegion(region);
    concert.setMember(member);
    concert.setStatus(Status.ACTIVE);
    concert.setViewCount(0);

    return concert;
  }

  /**
   * 기존 Concert를 KOPIS 데이터로 업데이트
   */
  public void updateFromKopisData(KopisPerformanceDto dto) {
    // KOPIS 원본 데이터 업데이트
    this.kopisPrfnm = dto.getPrfnm();
    this.kopisFcltynm = dto.getFcltynm();
    this.kopisGenrenm = dto.getGenrenm();
    this.kopisPrfstate = dto.getPrfstate();
    this.kopisArea = dto.getArea();
    this.kopisPoster = dto.getPoster();

    // 가공된 데이터 업데이트
    this.title = cleanKopisTitle(dto.getPrfnm());
    this.venue = dto.getFcltynm();
    this.content = generateKopisContent(dto);

    // 날짜 업데이트
    this.startDate = parseKopisDate(dto.getPrfpdfrom());
    this.endDate = parseKopisDate(dto.getPrfpdto());

    // 동기화 시간 갱신
    this.lastSynced = LocalDateTime.now();
  }

  /**
   * KOPIS 날짜 형식을 LocalDate로 변환
   */
  private static LocalDate parseKopisDate(String kopisDate) {
    if(kopisDate == null || kopisDate.isEmpty()) {
      return null;
    }

    try {
      return LocalDate.parse(kopisDate, DateTimeFormatter.ofPattern("yyyyMMdd"));
    } catch(Exception e) {
      return null;
    }
  }

  /**
   * KOPIS 공연명 정리
   */
  private static String cleanKopisTitle(String title) {
    if(title == null) return "";
    return title.trim()
            .replaceAll("\\[.*?\\]", "")  // [대학로] 제거
            .replaceAll("\\(.*?\\)", "")  // (재공연) 제거
            .trim();
  }

  /**
   * KOPIS 정보로 기본 설명 생성
   */
  private static String generateKopisContent(KopisPerformanceDto dto) {
    StringBuilder content = new StringBuilder();
    if (dto.getGenrenm() != null) content.append("장르: ").append(dto.getGenrenm()).append("\n");
    if (dto.getArea() != null) content.append("지역: ").append(dto.getArea()).append("\n");
    if (dto.getPrfstate() != null) content.append("공연상태: ").append(dto.getPrfstate()).append("\n");
    return content.toString();
  }
}
