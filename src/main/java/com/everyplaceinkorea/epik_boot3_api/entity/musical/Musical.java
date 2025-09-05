package com.everyplaceinkorea.epik_boot3_api.entity.musical;

import com.everyplaceinkorea.epik_boot3_api.admin.contents.musical.enums.Status;
import com.everyplaceinkorea.epik_boot3_api.entity.common.DataSource;
import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import com.everyplaceinkorea.epik_boot3_api.entity.Region;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
public class Musical {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String address;
    private String venue;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "file_saved_name")
    private String fileSavedName;

    @Column(name = "running_time")
    private String runningTime;

    @Column(name = "age_restriction")
    private String ageRestriction;

    @Column(name = "view_count")
    private Integer viewCount;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    @Column(name = "write_date")
    @CreationTimestamp
    private LocalDateTime writeDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member; // 작성자 테이블 외래키(fk)

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "region_id")
    private Region region; // 지역 테이블 외래키(fk)

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
    private String kopisPrfnm;

    @Column(name = "kopis_fcltynm")
    private String kopisFcltynm;

    @Column(name = "kopisPrfstate")
    private String kopisPrfstate;

    @Column(name = "kopis_genrenm")
    private String kopisGenrenm;

    @Column(name = "kopis_area")
    private String kopisArea;

    @Column(name = "kopis_poster")
    private String kopisPoster;


    public void addMember(Member member) {
        this.member = member;
    }

    public void addRegion(Region region) {
        this.region = region;
    }

    public void addFileSavedName(String fileSavedName) {
        this.fileSavedName = fileSavedName;
    }

    // 상태 변경
    public void changeStatus() {
        this.status = (this.status == Status.ACTIVE) ? Status.HIDDEN : Status.ACTIVE;
    }

    // 삭제
    public void delete() {
        this.status = Status.DELETE;
    }


}

