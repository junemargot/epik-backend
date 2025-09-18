package com.everyplaceinkorea.epik_boot3_api.entity.musical;

import com.everyplaceinkorea.epik_boot3_api.admin.contents.musical.enums.Status;
import com.everyplaceinkorea.epik_boot3_api.entity.common.DataSource;
import com.everyplaceinkorea.epik_boot3_api.entity.concert.Concert;
import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import com.everyplaceinkorea.epik_boot3_api.entity.Region;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.dto.KopisPerformanceDto;
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

    private String address;
    private String venue;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "file_saved_name")
    private String fileSavedName;

    @Column(name = "file_path")
    private String filePath;

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

    // 상태 변경
    public void changeStatus() {
        this.status = (this.status == Status.ACTIVE) ? Status.HIDDEN : Status.ACTIVE;
    }

    // 삭제
    public void delete() {
        this.status = Status.DELETE;
    }

    /**
     * KOPIS 데이터로부터 Musical 엔티티 생성
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

        log.info("=== Musical 생성 완료: title=[{}]", dto.getPrfnm());

        return musical;
    }

    /**
     * 1. KOPIS 원본 데이터 보존
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
        musical.setTitle(determineTitle(cleanTitle, dto.getPrfnm()));

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

        this.setRunningTime(determineRunningTime(dto));

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

        // 추가 필드 업데이트
//        this.runningTime = dto.getOpenrun() != null && dto.getOpenrun().equals("Y") ? "오픈런" : this.runningTime;
        if (this.ageRestriction == null) {
            this.ageRestriction = "전체 관람가";
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
    private static LocalDate parseKopisDateSafely(String kopisDate) {
        if(!isValidString(kopisDate)) {
            log.warn("날짜 정보가 비어있습니다: {}", kopisDate);
            return LocalDate.now();
        }

        try {
            String cleanDate = kopisDate.trim().replaceAll("[^0-9]", "");
            if(cleanDate.length() == 8) {
                return LocalDate.parse(cleanDate, DateTimeFormatter.ofPattern("yyyyMMdd"));
            } else {
                log.warn("날짜 형식이 올바르지 않습니다: {} -> {}", kopisDate, cleanDate);
                return LocalDate.now();
            }
        } catch(Exception e) {
            log.error("날짜 파싱 실패: {} -> {}", kopisDate, e.getMessage());
            return LocalDate.now();
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
        return cleaned.isEmpty() ? title : cleaned;
    }

    /**
     * 제목 결정 로직
     */
    private static String determineTitle(String cleanTitle, String originalTitle) {
        if(cleanTitle != null && !cleanTitle.trim().isEmpty()) {
            return cleanTitle;
        }

        if(originalTitle != null) {
            // HTML 엔티티 디코딩
            return originalTitle
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&#39;", "'");
        }

        return "제목 없음";
    }

    /**
     * KOPIS 정보로 기본 설명 생성
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
            this.kopisPcseguidance = dto.getPcseguidance();
        }

        // 상세 이미지 목록 업데이트
        if (isValidString(dto.getStyurls())) {
            this.kopisStyurls = dto.getStyurls();
        }

        // 관람연령 업데이트 (개선된 로직)
        if (isValidString(dto.getPrfage())) {
            this.kopisPrfage = dto.getPrfage();
        }

        // 러닝타임 정보 추가 업데이트
        if (isValidString(dto.getPrfruntime()) && !isValidString(this.runningTime)) {
            this.runningTime = dto.getPrfruntime();
        }
    }

}

