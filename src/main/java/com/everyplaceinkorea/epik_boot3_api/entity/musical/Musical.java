package com.everyplaceinkorea.epik_boot3_api.entity.musical;

import com.everyplaceinkorea.epik_boot3_api.admin.contents.musical.enums.Status;
import com.everyplaceinkorea.epik_boot3_api.entity.common.DataSource;
import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import com.everyplaceinkorea.epik_boot3_api.entity.Region;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.dto.KopisPerformanceDto;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.utils.KopisDataUtils;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
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

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "venue", nullable = false)
    private String venue;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "running_time")
    private String runningTime;

    @Column(name = "age_restriction")
    private String ageRestriction;

    @Column(name = "view_count")
    private Integer viewCount;

    @Column(name = "write_date")
    @CreationTimestamp
    private LocalDateTime writeDate;

    @Column(name = "file_saved_name")
    private String fileSavedName;

    @Column(name = "file_path")
    private String filePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member; // 작성자 테이블 외래키(fk)

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "region_id")
    private Region region; // 지역 테이블 외래키(fk)

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    // KOPIS 관련 필드
    @Enumerated(EnumType.STRING)
    @Column(name = "data_source")
    private DataSource dataSource = DataSource.MANUAL;

    @Column(name = "last_synced")
    private LocalDateTime lastSynced;  // 마지막 동기화 시간

    @Column(name = "kopis_id", unique = true)
    private String kopisId;

    @Column(name = "kopis_prfnm")
    private String kopisPrfnm;

    @Column(name = "kopis_prfstate")
    private String kopisPrfstate;

    @Column(name = "kopis_genrenm")
    private String kopisGenrenm;

    @Column(name = "kopis_fcltynm")
    private String kopisFcltynm;

    @Column(name = "kopis_area")
    private String kopisArea;

    @Column(name = "kopis_poster")
    private String kopisPoster;

    @Column(name = "kopis_pcseguidance", columnDefinition = "TEXT")
    private String kopisPcseguidance; // 티켓가격

    @Column(name = "kopis_styurls", columnDefinition = "TEXT")
    private String kopisStyurls;

    @Column(name = "kopis_prfage") // 관람연령
    private String kopisPrfage;


    public void addMember(Member member) {
        this.member = member;
    }

    public void addRegion(Region region) {
        this.region = region;
    }

    public void addFileSavedName(String fileSavedName) {
        this.fileSavedName = fileSavedName;
    }

    public void changeStatus() {
        this.status = (this.status == Status.ACTIVE) ? Status.HIDDEN : Status.ACTIVE;
    }

    public void delete() {
        this.status = Status.DELETE;
    }

    /**
     * KOPIS 데이터로부터 Musical 엔티티 생성
     *
     * @param dto
     * @param region
     * @param member
     * @return
     */
    public static Musical fromKopisData(KopisPerformanceDto dto, Region region, Member member) {
        log.info("=== Musical.fromKopisData 호출됨 - 제목: [{}] ===", dto.getPrfnm());

        Musical musical = new Musical();

        // === 1. KOPIS 원본 데이터 저장 ===
        setKopisOriginalData(musical, dto);

        // === 2. 핵심 정보 데이터 매핑 ===
        setBasicInformation(musical, dto);

        // === 3. 날짜 정보 설정 ===
        setDateInformation(musical, dto);

        // === 4. 메타데이터 및 관계 설정 ===
        setMetadataAndRelations(musical, region, member);

        // === 5. 이미지 및 부가 정보 ===
        setAdditionalInformation(musical, dto);

        log.info("=== Musical 생성 완료: title=[{}] ===", dto.getPrfnm());

        return musical;
    }

    /**
     * 기존 Musical을 KOPIS 데이터로 업데이트
     */
    public void updateFromKopisData(KopisPerformanceDto dto) {
        log.info("=== updateFromKopisData 호출됨 ===");
        log.info("Musical 업데이트 시작: ID={}, KOPIS_ID={}", this.id, dto.getMt20id());
        log.info("DTO 제목: [{}]", dto.getPrfnm());

        // KOPIS 원본 데이터 업데이트
        this.kopisPrfnm = dto.getPrfnm();
        this.kopisFcltynm = dto.getFcltynm();
        this.kopisGenrenm = dto.getGenrenm();
        this.kopisPrfstate = dto.getPrfstate();
        this.kopisArea = dto.getArea();
        this.kopisPoster = dto.getPoster();

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

        // KOPIS 포스터를 기존 이미지 시스템에 연결 (Concert와 동일한 방식)
        if (dto.getPoster() != null && !dto.getPoster().trim().isEmpty()) {
            this.filePath = dto.getPoster(); // KOPIS 포스터 URL을 file_path에 저장
            this.fileSavedName = extractFileNameFromUrl(dto.getPoster()); // URL에서 파일명 추출
        }

        log.debug("Musical 업데이트 완료: title={}, venue={}", this.title, this.venue);
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
            this.kopisPcseguidance = dto.getPcseguidance();
        }

        // 상세 이미지 목록 업데이트
        if (isValidString(dto.getStyurls())) {
            this.kopisStyurls = dto.getStyurls();
        }

        // 러닝타임 정보 추가 업데이트
        if (isValidString(dto.getPrfruntime()) && !isValidString(this.runningTime)) {
            this.runningTime = dto.getPrfruntime();
        }
    }

    private static void setupImageData(Musical musical, KopisPerformanceDto dto) {
        // KOPIS 포스터를 기존 이미지 시스템에 연결
        if (dto.getPoster() != null && !dto.getPoster().trim().isEmpty()) {
            musical.setFilePath(dto.getPoster()); // KOPIS 포스터 URL을 file_path에 저장
            musical.setFileSavedName(extractFileNameFromUrl(dto.getPoster())); // URL에서 파일명 추출

//       파일명 추출 (URL에서)
            String fileName = extractFileNameFromUrl(dto.getPoster());
            musical.setFileSavedName(fileName);
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
    private static void setKopisOriginalData(Musical musical, KopisPerformanceDto dto) {
        musical.setKopisId(dto.getMt20id());
        musical.setKopisPrfnm(dto.getPrfnm());
        musical.setKopisFcltynm(dto.getFcltynm());
        musical.setKopisGenrenm(dto.getGenrenm());
        musical.setKopisPrfstate(dto.getPrfstate());
        musical.setAgeRestriction(dto.getPrfage());
        musical.setKopisArea(dto.getArea());
        musical.setKopisPoster(dto.getPoster());

        log.debug("KOPIS 원본 데이터 설정 완료: ID=[{}]", dto.getMt20id());
    }

    /**
     * 2. 핵심 정보 매핑 (제목, 공연장, 주소, 내용)
     */
    private static void setBasicInformation(Musical musical, KopisPerformanceDto dto) {
        // 제목 처리
        String cleanTitle = cleanKopisTitle(dto.getPrfnm());
        musical.setTitle(KopisDataUtils.normalizeTitle(cleanTitle));

        // 공연장 및 주소
        musical.setVenue(isValidString(dto.getFcltynm()) ? dto.getFcltynm() : "공연장 정보 없음");
        musical.setAddress(buildDetailedAddress(dto.getArea(), dto.getFcltynm()));

        // 설명 내용 생성
        musical.setContent(generateContent(dto));

        log.debug("기본 정보 설정 완료: title=[{}], venue=[{}]", musical.getTitle(), musical.getVenue());
    }

    /**
     * 3. 날짜 정보 설정
     */
    private static void setDateInformation(Musical musical, KopisPerformanceDto dto) {
        musical.setStartDate(parseKopisDateSafely(dto.getPrfpdfrom()));
        musical.setEndDate(parseKopisDateSafely(dto.getPrfpdto()));

        log.debug("날짜 정보 설정 완료: {} ~ {}", musical.getStartDate(), musical.getEndDate());
    }

    /**
     * 4. 메타데이터 및 관계 설정
     */
    private static void setMetadataAndRelations(Musical musical, Region region, Member member) {
        // 데이터 소스 및 동기화 정보
        musical.setDataSource(DataSource.KOPIS_API);
        musical.setLastSynced(LocalDateTime.now());

        // 엔티티 관계 설정
        musical.setRegion(region);
        musical.setMember(member);

        // 상태 및 초기값
        musical.setStatus(Status.ACTIVE);
        musical.setViewCount(0);

        log.debug("메타데이터 설정 완료: region=[{}], member=[{}]",
                region != null ? region.getId() : null,
                member != null ? member.getId() : null);
    }

    /**
     * 5. 이미지 및 부가 정보 설정
     */
    private static void setAdditionalInformation(Musical musical, KopisPerformanceDto dto) {
        // 상세 이미지
        musical.setKopisStyurls(dto.getStyurls());

        // 가격정보 설정
        musical.setKopisPcseguidance(dto.getPcseguidance());

        // 포스터 이미지 처리
        setupImageData(musical, dto);

        // 러닝타임 처리
        musical.setRunningTime(determineRunningTime(dto));

        log.debug("부가 정보 설정 완료: runningTime=[{}]", musical.getRunningTime());
    }
}