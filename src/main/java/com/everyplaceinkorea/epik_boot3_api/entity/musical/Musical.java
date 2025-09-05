package com.everyplaceinkorea.epik_boot3_api.entity.musical;

import com.everyplaceinkorea.epik_boot3_api.admin.contents.musical.enums.Status;
import com.everyplaceinkorea.epik_boot3_api.entity.common.DataSource;
import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import com.everyplaceinkorea.epik_boot3_api.entity.Region;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.dto.KopisPerformanceDto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    @Column(name = "kopis_prfstate")
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

    /**
     * KOPIS 데이터로부터 musical 엔티티 생성
     */
    public static Musical fromKopisData(KopisPerformanceDto dto, Region region, Member member) {
        Musical musical = new Musical();

        // KOPIS 원본 데이터 보존
        musical.setKopisId(dto.getMt20id());
        musical.setKopisPrfnm(dto.getPrfnm());
        musical.setKopisFcltynm(dto.getFcltynm());
        musical.setKopisGenrenm(dto.getGenrenm());
        musical.setKopisPrfstate(dto.getPrfstate());
        musical.setKopisArea(dto.getArea());
        musical.setKopisPoster(dto.getPoster());

        // 가공된 데이터 설정
        musical.setTitle(cleanKopisTitle(dto.getPrfnm()) != null && !cleanKopisTitle(dto.getPrfnm()).trim().isEmpty() 
                         ? cleanKopisTitle(dto.getPrfnm()) : "제목 없음");
        musical.setVenue(dto.getFcltynm() != null && !dto.getFcltynm().trim().isEmpty() 
                         ? dto.getFcltynm() : "공연장 정보 없음");
        musical.setContent(generateKopisContent(dto));
        musical.setAddress(dto.getArea() != null && !dto.getArea().trim().isEmpty() 
                           ? dto.getArea() : "주소 정보 없음");

        // 날짜 변환
        musical.setStartDate(parseKopisDate(dto.getPrfpdfrom()) != null 
                             ? parseKopisDate(dto.getPrfpdfrom()) 
                             : LocalDate.now());
        musical.setEndDate(parseKopisDate(dto.getPrfpdto()) != null 
                           ? parseKopisDate(dto.getPrfpdto()) 
                           : LocalDate.now().plusDays(1));

        // 메타데이터 설정
        musical.setDataSource(DataSource.KOPIS_API);
        musical.setLastSynced(LocalDateTime.now());
        musical.setRegion(region);
        musical.setMember(member);
        musical.setStatus(Status.ACTIVE);
        musical.setViewCount(0);
        
        // KOPIS 포스터를 기존 이미지 시스템에 연결
        if (dto.getPoster() != null && !dto.getPoster().trim().isEmpty()) {
            musical.setFileSavedName(extractFileNameFromUrl(dto.getPoster()));
        }
        
        // 추가 필드 매핑
        musical.setRunningTime(dto.getOpenrun() != null && dto.getOpenrun().equals("Y") ? "오픈런" : null);
        musical.setAgeRestriction("전체 관람가");

        return musical;
    }

    /**
     * 기존 musical를 KOPIS 데이터로 업데이트
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
            return "kopis_poster.jpg"; // 기본 파일명
        }
    }

    /**
     * KOPIS 날짜 형식을 LocalDate로 변환
     */
    private static LocalDate parseKopisDate(String kopisDate) {
        if(kopisDate == null || kopisDate.trim().isEmpty()) {
            return null;
        }

        try {
            // YYYYMMDD 형식 파싱
            String cleanDate = kopisDate.trim().replaceAll("[^0-9]", ""); // 숫자만 추출
            if (cleanDate.length() == 8) {
                return LocalDate.parse(cleanDate, DateTimeFormatter.ofPattern("yyyyMMdd"));
            } else {
                return null;
            }
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
                .replaceAll("\\[.*?\\]", "")  // [대학로] 제거 (대괄호)
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

