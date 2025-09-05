package com.everyplaceinkorea.epik_boot3_api.entity.concert;

import com.everyplaceinkorea.epik_boot3_api.admin.contents.concert.dto.ConcertUploadResultDto;
import com.everyplaceinkorea.epik_boot3_api.entity.common.DataSource;
import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import com.everyplaceinkorea.epik_boot3_api.entity.musical.Status;
import com.everyplaceinkorea.epik_boot3_api.entity.Region;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CurrentTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

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


}
