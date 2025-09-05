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
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    concert.setTitle(cleanKopisTitle(dto.getPrfnm()) != null && !cleanKopisTitle(dto.getPrfnm()).trim().isEmpty() 
                     ? cleanKopisTitle(dto.getPrfnm()) : "제목 없음");
    concert.setVenue(dto.getFcltynm() != null && !dto.getFcltynm().trim().isEmpty() 
                     ? dto.getFcltynm() : "공연장 정보 없음");
    concert.setContent(generateKopisContent(dto));
    concert.setAddress(dto.getArea() != null && !dto.getArea().trim().isEmpty() 
                       ? dto.getArea() : "주소 정보 없음");

    // 날짜 변환
    concert.setStartDate(parseKopisDate(dto.getPrfpdfrom()) != null 
                         ? parseKopisDate(dto.getPrfpdfrom()) 
                         : LocalDate.now());
    concert.setEndDate(parseKopisDate(dto.getPrfpdto()) != null 
                       ? parseKopisDate(dto.getPrfpdto()) 
                       : LocalDate.now().plusDays(1));

    // 메타데이터 설정
    concert.setDataSource(DataSource.KOPIS_API);
    concert.setLastSynced(LocalDateTime.now());
    concert.setRegion(region);
    concert.setMember(member);
    concert.setStatus(Status.ACTIVE);
    concert.setViewCount(0);
    
    // KOPIS 포스터를 기존 이미지 시스템에 연결
    if (dto.getPoster() != null && !dto.getPoster().trim().isEmpty()) {
      concert.setFilePath(dto.getPoster()); // KOPIS 포스터 URL을 file_path에 저장
      concert.setFileSavedName(extractFileNameFromUrl(dto.getPoster())); // URL에서 파일명 추출
    }
    
    // 추가 필드 매핑
    concert.setRunningTime(dto.getOpenrun() != null && dto.getOpenrun().equals("Y") ? "오픈런" : null);
    concert.setAgeRestriction("전체 관람가"); // KOPIS API에서 연령 제한 정보가 별도로 없어서 기본값

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
    this.startDate = parseKopisDate(dto.getPrfpdfrom()) != null 
                     ? parseKopisDate(dto.getPrfpdfrom()) 
                     : this.startDate; // 기존 값 유지
    this.endDate = parseKopisDate(dto.getPrfpdto()) != null 
                   ? parseKopisDate(dto.getPrfpdto()) 
                   : this.endDate; // 기존 값 유지

    // 동기화 시간 갱신
    this.lastSynced = LocalDateTime.now();
    
    // KOPIS 포스터를 기존 이미지 시스템에 연결
    if (dto.getPoster() != null && !dto.getPoster().trim().isEmpty()) {
      this.filePath = dto.getPoster();
      this.fileSavedName = extractFileNameFromUrl(dto.getPoster());
    }
    
    // 추가 필드 업데이트
    this.runningTime = dto.getOpenrun() != null && dto.getOpenrun().equals("Y") ? "오픈런" : this.runningTime;
    if (this.ageRestriction == null) {
      this.ageRestriction = "전체 관람가";
    }
  }

  /**
   * URL에서 파일명 추출
   */
  private static String extractFileNameFromUrl(String url) {
    if (url == null || url.trim().isEmpty()) {
      return null;
    }
    
    try {
      String[] parts = url.split("/");
      return parts[parts.length - 1]; // 마지막 부분이 파일명
    } catch (Exception e) {
      log.warn("URL에서 파일명 추출 실패: {}", url);
      return "kopis_poster.jpg"; // 기본 파일명
    }
  }

  /**
   * KOPIS 날짜 형식을 LocalDate로 변환
   */
  private static LocalDate parseKopisDate(String kopisDate) {
    if(kopisDate == null || kopisDate.trim().isEmpty()) {
      log.warn("KOPIS 날짜가 비어있습니다: {}", kopisDate);
      return null;
    }

    try {
      // YYYYMMDD 형식 파싱
      String cleanDate = kopisDate.trim().replaceAll("[^0-9]", ""); // 숫자만 추출
      if (cleanDate.length() == 8) {
        return LocalDate.parse(cleanDate, DateTimeFormatter.ofPattern("yyyyMMdd"));
      } else {
        log.warn("KOPIS 날짜 형식이 올바르지 않습니다: {} -> {}", kopisDate, cleanDate);
        return null;
      }
    } catch(Exception e) {
      log.error("KOPIS 날짜 파싱 실패: {} - {}", kopisDate, e.getMessage());
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
    
    if (dto.getGenrenm() != null && !dto.getGenrenm().trim().isEmpty()) {
      content.append("장르: ").append(dto.getGenrenm()).append("\n");
    }
    if (dto.getArea() != null && !dto.getArea().trim().isEmpty()) {
      content.append("지역: ").append(dto.getArea()).append("\n");
    }
    if (dto.getPrfstate() != null && !dto.getPrfstate().trim().isEmpty()) {
      content.append("공연상태: ").append(dto.getPrfstate()).append("\n");
    }
    if (dto.getFcltynm() != null && !dto.getFcltynm().trim().isEmpty()) {
      content.append("공연장: ").append(dto.getFcltynm()).append("\n");
    }
    
    String result = content.toString();
    return result.isEmpty() ? "KOPIS에서 가져온 공연 정보입니다." : result;
  }
}
