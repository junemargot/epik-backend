 false)
    private String venue;  // "예술의전당 콘서트홀"
    
    // === 헬퍼 메서드 ===
    
    public void setKopisMetadata(KopisMetadata metadata) {
        this.kopisMetadata = metadata;
    }
    
    public KopisMetadata getKopisMetadata() {
        if (this.kopisMetadata == null) {
            this.kopisMetadata = KopisMetadata.createDefault();
        }
        return this.kopisMetadata;
    }
}
```

#### 3-2. Concert 기존 메서드 수정

기존 KOPIS 관련 필드를 직접 접근하던 코드를 KopisMetadata를 통해 접근하도록 수정:

```java
// Before
concert.setKopisId("PF123456");
concert.setDataSource(DataSource.KOPIS_API);

// After
concert.getKopisMetadata().setKopisId("PF123456");
concert.getKopisMetadata().setDataSource(DataSource.KOPIS_API);
```

---

### Phase 4: 서비스 레이어 수정 (4-5시간)

#### 4-1. FacilityService 생성

```java
// src/main/java/com/everyplaceinkorea/epik_boot3_api/service/facility/FacilityService.java

package com.everyplaceinkorea.epik_boot3_api.service.facility;

import com.everyplaceinkorea.epik_boot3_api.external.kopis.KopisApiService;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.dto.KopisFacilityDto;
import com.everyplaceinkorea.epik_boot3_api.repository.facility.FacilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacilityService {

  private final FacilityRepository facilityRepository;
  private final KopisApiService kopisApiService;

  /**
   * Facility 조회 또는 생성
   * 이미 있으면 기존 것 반환, 없으면 KOPIS API 호출 후 생성
   */
  @Transactional
  public Facility getOrCreateFacility(String facilityId) {
    // 1. DB에 이미 있는지 확인
    Optional<Facility> existing = facilityRepository.findByFacilityId(facilityId);
    if (existing.isPresent()) {
      log.debug("기존 Facility 사용: {}", facilityId);
      return existing.get();
    }

    // 2. KOPIS API 호출
    log.info("새 Facility 생성 시작: {}", facilityId);
    try {
      String xml = kopisApiService.getFacilityDetail(facilityId);
      if (xml == null) {
        log.warn("Facility 정보 조회 실패: {}", facilityId);
        return null;
      }

      // 3. DTO 파싱
      KopisFacilityDto dto = kopisApiService.parseXmlToFacilityDto(xml);

      // 4. Facility 생성
      Facility facility = Facility.fromKopisData(dto);

      // 5. Hall 목록 생성
      if (dto.getHalls() != null && !dto.getHalls().isEmpty()) {
        dto.getHalls().forEach(hallDto -> {
          Hall hall = Hall.fromKopisData(hallDto, facility);
          facility.addHall(hall);
        });
      }

      // 6. 저장
      Facility saved = facilityRepository.save(facility);
      log.info("Facility 생성 완료: {} (Halls: {})",
              saved.getFacilityId(), saved.getHalls().size());

      return saved;

    } catch (Exception e) {
      log.error("Facility 생성 실패: {} - {}", facilityId, e.getMessage(), e);
      return null;
    }
  }

  /**
   * 공연장명 매칭
   * Facility의 Hall 목록에서 fcltynm과 가장 유사한 Hall 찾기
   */
  public Hall matchHall(Facility facility, String fcltynmFromPerformance) {
    if (facility == null || facility.getHalls().isEmpty()) {
      return null;
    }

    // fcltynmFromPerformance: "예술의전당(콘서트홀)"
    // 괄호 안의 내용 추출
    String hallNameToMatch = extractHallName(fcltynmFromPerformance);

    // Hall 목록에서 매칭
    return facility.getHalls().stream()
            .filter(hall -> hall.getName().contains(hallNameToMatch)
                    || hallNameToMatch.contains(hall.getName()))
            .findFirst()
            .orElse(null);
  }

  private String extractHallName(String fcltynm) {
    // "예술의전당(콘서트홀)" -> "콘서트홀"
    int start = fcltynm.lastIndexOf("(");
    int end = fcltynm.lastIndexOf(")");

    if (start > 0 && end > start) {
      return fcltynm.substring(start + 1, end);
    }

    return fcltynm;
  }
}
```

#### 4-2. KopisDataSyncService 수정

```java
// KopisDataSyncService.java - syncSingleConcert 메서드 수정

private void syncSingleConcert(KopisPerformanceDto dto, 
                                Member systemMember, 
                                Region defaultRegion, 
                                SyncResult result) {
    try {
        Optional<Concert> existingConcert = concertRepository
            .findByKopisMetadata_KopisId(dto.getMt20id());
        
        Concert concert;
        
        if (existingConcert.isPresent()) {
            concert = existingConcert.get();
            concert.updateFromKopisData(dto);
        } else {
            concert = Concert.fromKopisData(dto, defaultRegion, systemMember);
        }
        
        // === 시설 정보 조회 및 설정 (새로 추가) ===
        if (dto.getMt10id() != null && !dto.getMt10id().isEmpty()) {
            try {
                Facility facility = facilityService.getOrCreateFacility(dto.getMt10id());
                
                if (facility != null) {
                    concert.setFacility(facility);
                    
                    // Hall 매칭
                    Hall hall = facilityService.matchHall(facility, dto.getFcltynm());
                    if (hall != null) {
                        concert.setHall(hall);
                    }
                    
                    // venue 필드 업데이트
                    String venueName = buildVenueName(facility, hall);
                    concert.setVenue(venueName);
                    
                    log.debug("시설 정보 설정 완료: {}", concert.getTitle());
                }
            } catch (Exception e) {
                log.warn("시설 정보 조회 실패: {} - {}", 
                    concert.getTitle(), e.getMessage());
                // 실패해도 Concert 저장은 계속 진행
            }
        }
        
        concertRepository.save(concert);
        result.incrementSuccess();
        
    } catch (Exception e) {
        log.error("Concert 동기화 실패: {}", dto.getMt20id(), e);
        result.addFailure(dto.getPrfnm() + ": " + e.getMessage());
    }
}

private String buildVenueName(Facility facility, Hall hall) {
    if (facility == null) {
        return "정보 없음";
    }
    
    if (hall != null) {
        return facility.getName() + " " + hall.getName();
    }
    
    return facility.getName();
}
```

---

### Phase 5: 데이터베이스 마이그레이션 (3-4시간)

#### 5-1. 마이그레이션 SQL 작성

```sql
-- migration_v1_create_facility_tables.sql

-- 1. Facility 테이블 생성
CREATE TABLE facility (
    facility_id VARCHAR(50) PRIMARY KEY COMMENT 'KOPIS mt10id',
    name VARCHAR(200) NOT NULL COMMENT '시설명',
    address VARCHAR(500) COMMENT '상세 주소',
    latitude DOUBLE COMMENT '위도',
    longitude DOUBLE COMMENT '경도',
    tel VARCHAR(50) COMMENT '전화번호',
    url VARCHAR(500) COMMENT '홈페이지',
    data_source VARCHAR(20) COMMENT 'KOPIS_API or MANUAL',
    last_synced DATETIME COMMENT '마지막 동기화 시간',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='공연 시설 정보';

-- 2. Hall 테이블 생성
CREATE TABLE hall (
    hall_id VARCHAR(50) PRIMARY KEY COMMENT 'KOPIS mt13id',
    name VARCHAR(200) NOT NULL COMMENT '공연장명',
    seat_count INT COMMENT '좌석수',
    facility_id VARCHAR(50) NOT NULL COMMENT '시설 ID',
    last_synced DATETIME COMMENT '마지막 동기화 시간',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (facility_id) REFERENCES facility(facility_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='공연장 정보';

-- 3. Concert 테이블에 FK 컬럼 추가
ALTER TABLE concert 
    ADD COLUMN facility_id VARCHAR(50) COMMENT '시설 ID',
    ADD COLUMN hall_id VARCHAR(50) COMMENT '공연장 ID';

-- 4. 외래키 제약조건 추가
ALTER TABLE concert
    ADD CONSTRAINT fk_concert_facility 
    FOREIGN KEY (facility_id) REFERENCES facility(facility_id) ON DELETE SET NULL;

ALTER TABLE concert
    ADD CONSTRAINT fk_concert_hall 
    FOREIGN KEY (hall_id) REFERENCES hall(hall_id) ON DELETE SET NULL;

-- 5. 인덱스 생성
CREATE INDEX idx_facility_name ON facility(name);
CREATE INDEX idx_facility_coordinates ON facility(latitude, longitude);
CREATE INDEX idx_hall_facility ON hall(facility_id);
CREATE INDEX idx_concert_facility ON concert(facility_id);
CREATE INDEX idx_concert_hall ON concert(hall_id);
```

#### 5-2. 데이터 마이그레이션 SQL

```sql
-- migration_v2_data_migration.sql

-- 1. Facility 데이터 추출 및 삽입
-- (중복 제거하여 unique한 facility만 삽입)
INSERT INTO facility (
    facility_id, 
    latitude, 
    longitude, 
    address, 
    tel, 
    url, 
    data_source, 
    last_synced
)
SELECT DISTINCT 
    c.facility_id,
    c.latitude,
    c.longitude,
    c.detailed_address,
    c.facility_tel,
    c.facility_url,
    c.data_source,
    c.last_synced
FROM concert c
WHERE c.facility_id IS NOT NULL
    AND c.facility_id != ''
    AND c.latitude IS NOT NULL
ON DUPLICATE KEY UPDATE
    latitude = VALUES(latitude),
    longitude = VALUES(longitude),
    address = VALUES(address),
    tel = VALUES(tel),
    url = VALUES(url),
    last_synced = VALUES(last_synced);

-- 2. Musical 테이블도 동일하게 처리 (필요시)
INSERT INTO facility (
    facility_id, 
    latitude, 
    longitude, 
    address, 
    tel, 
    url, 
    data_source, 
    last_synced
)
SELECT DISTINCT 
    m.facility_id,
    m.latitude,
    m.longitude,
    m.detailed_address,
    m.facility_tel,
    m.facility_url,
    m.data_source,
    m.last_synced
FROM musical m
WHERE m.facility_id IS NOT NULL
    AND m.facility_id != ''
    AND m.latitude IS NOT NULL
    AND NOT EXISTS (
        SELECT 1 FROM facility f 
        WHERE f.facility_id = m.facility_id
    );

-- 3. 마이그레이션 검증 쿼리
SELECT 
    COUNT(DISTINCT facility_id) as unique_facilities_in_concert,
    COUNT(*) as total_concerts_with_facility
FROM concert
WHERE facility_id IS NOT NULL;

SELECT COUNT(*) as total_facilities FROM facility;
```

#### 5-3. 기존 컬럼 제거 SQL (마이그레이션 검증 후 실행)

```sql
-- migration_v3_cleanup.sql
-- ⚠️ 주의: 마이그레이션 검증 완료 후에만 실행!

-- Concert 테이블에서 중복 컬럼 제거
ALTER TABLE concert 
    DROP COLUMN latitude,
    DROP COLUMN longitude,
    DROP COLUMN detailed_address,
    DROP COLUMN facility_tel,
    DROP COLUMN facility_url;

-- Musical 테이블도 동일하게 처리
ALTER TABLE musical 
    DROP COLUMN latitude,
    DROP COLUMN longitude,
    DROP COLUMN detailed_address,
    DROP COLUMN facility_tel,
    DROP COLUMN facility_url;
```

---

### Phase 6: DTO 및 API 응답 수정 (2-3시간)

#### 6-1. FacilityDto 생성

```java
// src/main/java/com/everyplaceinkorea/epik_boot3_api/dto/facility/FacilityDto.java

package com.everyplaceinkorea.epik_boot3_api.dto.facility;

import lombok.Data;

@Data
public class FacilityDto {
  private String facilityId;
  private String name;
  private String address;
  private Double latitude;
  private Double longitude;
  private String tel;
  private String url;

  public static FacilityDto from(Facility facility) {
    if (facility == null) {
      return null;
    }

    FacilityDto dto = new FacilityDto();
    dto.setFacilityId(facility.getFacilityId());
    dto.setName(facility.getName());
    dto.setAddress(facility.getAddress());
    dto.setLatitude(facility.getLatitude());
    dto.setLongitude(facility.getLongitude());
    dto.setTel(facility.getTel());
    dto.setUrl(facility.getUrl());
    return dto;
  }
}
```

#### 6-2. ConcertResponseDto 수정

```java
// ConcertResponseDto.java 수정

@Data
public class ConcertResponseDto {
    private Long id;
    private String title;
    private String content;
    private LocalDate startDate;
    private LocalDate endDate;
    private String venue;
    
    // 시설 정보는 중첩 객체로
    private FacilityDto facility;
    
    // KOPIS 정보
    private String kopisId;
    private String posterUrl;
    
    public static ConcertResponseDto from(Concert concert) {
        ConcertResponseDto dto = new ConcertResponseDto();
        dto.setId(concert.getId());
        dto.setTitle(concert.getTitle());
        dto.setContent(concert.getContent());
        dto.setStartDate(concert.getStartDate());
        dto.setEndDate(concert.getEndDate());
        dto.setVenue(concert.getVenue());
        
        // Facility 매핑
        dto.setFacility(FacilityDto.from(concert.getFacility()));
        
        // KOPIS 메타데이터
        if (concert.getKopisMetadata() != null) {
            dto.setKopisId(concert.getKopisMetadata().getKopisId());
            dto.setPosterUrl(concert.getKopisMetadata().getKopisPoster());
        }
        
        return dto;
    }
}
```

---

### Phase 7: 테스트 (3-4시간)

#### 7-1. 단위 테스트 작성

```java
// FacilityServiceTest.java

@SpringBootTest
@Transactional
class FacilityServiceTest {
    
    @Autowired
    private FacilityService facilityService;
    
    @Autowired
    private FacilityRepository facilityRepository;
    
    @Test
    @DisplayName("Facility 생성 테스트")
    void testCreateFacility() {
        // given
        String facilityId = "FC001247";
        
        // when
        Facility facility = facilityService.getOrCreateFacility(facilityId);
        
        // then
        assertThat(facility).isNotNull();
        assertThat(facility.getFacilityId()).isEqualTo(facilityId);
        assertThat(facility.getLatitude()).isNotNull();
        assertThat(facility.getLongitude()).isNotNull();
    }
    
    @Test
    @DisplayName("Facility 중복 생성 방지 테스트")
    void testGetExistingFacility() {
        // given
        String facilityId = "FC001247";
        Facility first = facilityService.getOrCreateFacility(facilityId);
        
        // when
        Facility second = facilityService.getOrCreateFacility(facilityId);
        
        // then
        assertThat(first.getFacilityId()).isEqualTo(second.getFacilityId());
        assertThat(facilityRepository.count()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("Hall 매칭 테스트")
    void testMatchHall() {
        // given
        Facility facility = createTestFacility();
        String fcltynm = "예술의전당(콘서트홀)";
        
        // when
        Hall hall = facilityService.matchHall(facility, fcltynm);
        
        // then
        assertThat(hall).isNotNull();
        assertThat(hall.getName()).contains("콘서트홀");
    }
}
```

#### 7-2. 통합 테스트

```java
// ConcertSyncIntegrationTest.java

@SpringBootTest
@Transactional
class ConcertSyncIntegrationTest {
    
    @Autowired
    private KopisDataSyncService syncService;
    
    @Autowired
    private ConcertRepository concertRepository;
    
    @Autowired
    private FacilityRepository facilityRepository;
    
    @Test
    @DisplayName("Concert 동기화 시 Facility 자동 생성 테스트")
    void testConcertSyncWithFacility() {
        // given
        LocalDate now = LocalDate.now();
        String startDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String endDate = now.plusDays(7).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // when
        SyncResult result = syncService.syncConcerts(startDate, endDate);
        
        // then
        assertThat(result.getSuccessCount()).isGreaterThan(0);
        
        List<Concert> concerts = concertRepository.findAll();
        assertThat(concerts).isNotEmpty();
        
        Concert concert = concerts.get(0);
        assertThat(concert.getFacility()).isNotNull();
        assertThat(concert.getFacility().getLatitude()).isNotNull();
        assertThat(concert.getFacility().getLongitude()).isNotNull();
    }
}
```

#### 7-3. 마이그레이션 검증 쿼리

```sql
-- 마이그레이션 검증 쿼리들

-- 1. Facility 생성 확인
SELECT COUNT(*) FROM facility;

-- 2. Concert-Facility 매핑 확인
SELECT 
    COUNT(*) as total_concerts,
    COUNT(facility_id) as concerts_with_facility,
    COUNT(facility_id) * 100.0 / COUNT(*) as percentage
FROM concert;

-- 3. 중복 제거 확인
SELECT 
    facility_id,
    COUNT(*) as concert_count
FROM concert
WHERE facility_id IS NOT NULL
GROUP BY facility_id
ORDER BY concert_count DESC
LIMIT 10;

-- 4. 데이터 일관성 확인
SELECT c.id, c.title, f.name, f.latitude, f.longitude
FROM concert c
LEFT JOIN facility f ON c.facility_id = f.facility_id
WHERE c.facility_id IS NOT NULL
LIMIT 10;
```

---

### Phase 8: 배포 및 모니터링 (1일)

#### 8-1. 배포 전 체크리스트

```markdown
### 배포 전 확인사항
- [ ] 모든 단위 테스트 통과
- [ ] 통합 테스트 통과
- [ ] 마이그레이션 SQL 검증 완료
- [ ] 롤백 계획 수립
- [ ] 데이터베이스 백업 완료
- [ ] 개발 환경에서 마이그레이션 테스트 완료
- [ ] 스테이징 환경 배포 및 검증
```

#### 8-2. 배포 순서

```bash
# 1. 데이터베이스 백업
mysqldump -u username -p epik_db > backup_before_migration.sql

# 2. 마이그레이션 실행
mysql -u username -p epik_db < migration_v1_create_facility_tables.sql
mysql -u username -p epik_db < migration_v2_data_migration.sql

# 3. 마이그레이션 검증
mysql -u username -p epik_db < validation_queries.sql

# 4. 애플리케이션 배포
./mvnw clean package
java -jar target/epik-api.jar

# 5. 기존 컬럼 제거 (검증 완료 후)
# mysql -u username -p epik_db < migration_v3_cleanup.sql
```

#### 8-3. 모니터링 항목

```markdown
### 배포 후 모니터링
- [ ] API 응답 시간 확인
- [ ] Concert 조회 시 Facility 정보 포함 확인
- [ ] 에러 로그 모니터링
- [ ] 데이터베이스 쿼리 성능 확인
- [ ] 신규 Concert 동기화 시 Facility 자동 생성 확인
```

---

## 📋 체크리스트

### Phase 1: 준비
- [ ] 데이터베이스 백업 완료
- [ ] feature 브랜치 생성
- [ ] 필드 분류 완료

### Phase 2: 엔티티 생성
- [ ] KopisMetadata.java 생성
- [ ] Facility.java 생성
- [ ] Hall.java 생성
- [ ] FacilityRepository.java 생성
- [ ] HallRepository.java 생성

### Phase 3: Concert 리팩토링
- [ ] Concert.java @Embedded 적용
- [ ] Concert.java FK 추가
- [ ] 기존 메서드 수정

### Phase 4: 서비스 레이어
- [ ] FacilityService.java 생성
- [ ] KopisDataSyncService.java 수정
- [ ] buildVenueName() 구현

### Phase 5: 데이터베이스
- [ ] migration_v1 실행 (테이블 생성)
- [ ] migration_v2 실행 (데이터 이관)
- [ ] 검증 쿼리 실행
- [ ] migration_v3 실행 (컬럼 제거)

### Phase 6: DTO
- [ ] FacilityDto.java 생성
- [ ] ConcertResponseDto.java 수정
- [ ] API 응답 확인

### Phase 7: 테스트
- [ ] FacilityServiceTest 작성 및 통과
- [ ] 통합 테스트 작성 및 통과
- [ ] 수동 테스트 완료

### Phase 8: 배포
- [ ] 스테이징 배포
- [ ] 프로덕션 배포
- [ ] 모니터링 확인

---

## 🚨 롤백 계획

### 롤백 시나리오
1. 마이그레이션 실패
2. 애플리케이션 에러 다발
3. 성능 저하

### 롤백 절차

```bash
# 1. 애플리케이션 이전 버전으로 롤백
git checkout main
./mvnw clean package
java -jar target/epik-api.jar

# 2. 데이터베이스 복구
mysql -u username -p epik_db < backup_before_migration.sql

# 3. 검증
# - API 정상 동작 확인
# - 데이터 일관성 확인
```

---

## 📊 예상 리소스

### 개발 시간
- Phase 1-2: 6-8시간
- Phase 3-4: 7-9시간
- Phase 5-6: 5-6시간  
- Phase 7-8: 4-5시간
- **총 예상 시간: 22-28시간 (3-4일)**

### 인프라
- 데이터베이스 백업 용량: 약 100-500MB
- 마이그레이션 실행 시간: 약 5-10분
- 배포 다운타임: 약 5분

---

## ⚠️ 주의사항

1. **데이터 백업 필수**: 모든 단계 전에 백업
2. **스테이징 먼저**: 프로덕션 배포 전 반드시 스테이징에서 검증
3. **점진적 진행**: 한 Phase씩 완료 후 다음 진행
4. **롤백 준비**: 언제든 롤백 가능하도록 준비
5. **모니터링**: 배포 후 24시간 집중 모니터링

---

## 📚 참고 문서

- Issue #2: KOPIS 필드 분리 리팩토링
- Issue #1: 카카오맵 표시를 위한 시설 정보 추가
- KOPIS API 가이드