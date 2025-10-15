# [REFACTOR] Concert/Musical 엔티티의 KOPIS 관련 필드 분리

## 이슈 개요

### 문제 정의
Concert와 Musical 엔티티에 KOPIS API 관련 필드가 과도하게 많아져 엔티티가 비대해지고, 유지보수성이 저하되는 문제

### 발생 배경
- 초기: 수기 입력 방식
- 변경: 팝업을 제외한 콘텐츠를 KOPIS API 기반으로 전환
- 결과: KOPIS 관련 필드 10개 이상 추가
- 추가 문제: Issue #1로 인해 시설 정보 필드(latitude, longitude, tel, url 등)도 추가 예정

### 우선순위
**Medium** - 당장 서비스에 영향은 없지만, 장기적으로 반드시 해결해야 할 기술 부채

---

## 현재 상태 (AS-IS)

### Concert 엔티티 구조
```java
@Entity
public class Concert {
    // 기본 필드
    private Long id;
    private String title;
    private String content;
    private LocalDate startDate;
    private LocalDate endDate;
    // ... (10개 필드)
    
    // KOPIS 원본 데이터 (10+ 필드)
    private String kopisId;
    private String kopisPrfnm;
    private String kopisPrfstate;
    private String kopisGenrenm;
    private String kopisFcltynm;
    private String kopisArea;
    private String kopisPoster;
    private String ticketPrice;
    private String detailImages;
    private String kopisTicketOffices;
    private LocalDateTime kopisTicketOfficesUpdatedAt;
    private TicketOfficeSource kopisTicketOfficesSource;
    
    // Issue #1로 추가될 시설 정보 (6개 필드)
    private Double latitude;
    private Double longitude;
    private String detailedAddress;
    private String facilityId;
    private String facilityTel;
    private String facilityUrl;
    
    // 기타 메타데이터
    private DataSource dataSource;
    private LocalDateTime lastSynced;
}
```

**총 필드 수: 30개 이상**

### 문제점

#### 1. 단일 책임 원칙(SRP) 위반
Concert 엔티티가 담당하는 책임:
- 공연 정보 관리
- KOPIS 동기화 메타데이터 관리
- 시설 정보 관리 (Issue #1 이후)

#### 2. 데이터 중복
```sql
-- 같은 시설의 공연들이 시설 정보를 중복 저장
Concert (id=1, title="공연A", latitude=37.5, facilityTel="02-123-4567")
Concert (id=2, title="공연B", latitude=37.5, facilityTel="02-123-4567")  -- 중복!
Concert (id=3, title="공연C", latitude=37.5, facilityTel="02-123-4567")  -- 중복!
```

#### 3. 유지보수성 저하
- 필드가 너무 많아 코드 가독성 감소
- 시설 정보 변경 시 모든 관련 Concert 업데이트 필요
- 새로운 KOPIS 필드 추가 시 엔티티 계속 비대화

#### 4. 확장성 제한
- 다른 데이터 소스(인터파크, 예스24) 추가 시 필드 더 증가
- 테이블 크기 증가로 인한 성능 저하 가능성

---

## 목표 상태 (TO-BE)

### 설계 원칙
1. **도메인 중심 설계**: 데이터 출처가 아닌 도메인 개념으로 엔티티 분리
2. **단일 책임 원칙**: 각 엔티티는 하나의 명확한 책임만 가짐
3. **데이터 정규화**: 중복 제거 및 참조 관계 활용

### 제안하는 엔티티 구조

```
Concert (공연 정보)
├── KopisMetadata (KOPIS 동기화 메타데이터) @Embedded
├── Facility (시설 정보) @ManyToOne FK
└── Hall (공연장 정보) @ManyToOne FK
```

---

## 상세 설계안

### 1. Concert 엔티티 (슬림화)
```java
@Entity
@Table(name = "concert")
public class Concert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 공연 고유 정보만
    private String title;
    private String content;
    private LocalDate startDate;
    private LocalDate endDate;
    private String runningTime;
    private String ageRestriction;
    
    // KOPIS 메타데이터 (Embedded)
    @Embedded
    private KopisMetadata kopisMetadata;
    
    // 시설/공연장 참조 (FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id")
    private Facility facility;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hall_id")
    private Hall hall;
    
    // 조회 편의를 위한 비정규화 필드
    @Column(name = "venue")
    private String venue;  // "예술의전당 콘서트홀"
    
    // 기타
    @ManyToOne(fetch = FetchType.LAZY)
    private Region region;
    
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;
}
```

### 2. KopisMetadata (Embeddable)
```java
@Embeddable
public class KopisMetadata {
    @Column(name = "kopis_id", unique = true)
    private String kopisId;  // mt20id
    
    @Column(name = "kopis_prfnm")
    private String kopisPrfnm;  // 원본 공연명
    
    @Column(name = "kopis_area")
    private String kopisArea;  // 원본 지역
    
    @Column(name = "kopis_poster")
    private String kopisPoster;  // 원본 포스터 URL
    
    @Column(name = "ticket_price", columnDefinition = "TEXT")
    private String ticketPrice;
    
    @Column(name = "detail_images", columnDefinition = "TEXT")
    private String detailImages;
    
    @Column(name = "kopis_ticket_offices", columnDefinition = "JSON")
    private String kopisTicketOffices;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "data_source")
    private DataSource dataSource;
    
    @Column(name = "last_synced")
    private LocalDateTime lastSynced;
}
```

### 3. Facility 엔티티 (새로 생성)
```java
@Entity
@Table(name = "facility")
public class Facility {
    @Id
    @Column(name = "facility_id")
    private String facilityId;  // KOPIS mt10id
    
    @Column(name = "name", nullable = false)
    private String name;  // 시설명
    
    @Column(name = "address")
    private String address;  // 상세 주소
    
    @Column(name = "latitude")
    private Double latitude;  // 위도
    
    @Column(name = "longitude")
    private Double longitude;  // 경도
    
    @Column(name = "tel")
    private String tel;  // 전화번호
    
    @Column(name = "url")
    private String url;  // 홈페이지
    
    @Enumerated(EnumType.STRING)
    @Column(name = "data_source")
    private DataSource dataSource;
    
    @Column(name = "last_synced")
    private LocalDateTime lastSynced;
    
    // 시설 내 공연장 목록
    @OneToMany(mappedBy = "facility", cascade = CascadeType.ALL)
    private List<Hall> halls = new ArrayList<>();
}
```

### 4. Hall 엔티티 (새로 생성)
```java
@Entity
@Table(name = "hall")
public class Hall {
    @Id
    @Column(name = "hall_id")
    private String hallId;  // KOPIS mt13id
    
    @Column(name = "name", nullable = false)
    private String name;  // 공연장명
    
    @Column(name = "seat_count")
    private Integer seatCount;  // 좌석수
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;
    
    @Column(name = "last_synced")
    private LocalDateTime lastSynced;
}
```

---

## 비교 분석

### 필드 수 비교
| 엔티티 | AS-IS | TO-BE | 감소율 |
|--------|-------|-------|--------|
| Concert | 30+ | 15 | 50% |
| Facility | - | 9 | 신규 |
| Hall | - | 4 | 신규 |

### 데이터 중복 비교
```
AS-IS: 
- 공연 1000개, 시설 100개 가정
- 시설 정보 저장: 1000회 (9배 중복)

TO-BE:
- 공연 1000개, 시설 100개
- 시설 정보 저장: 100회 (중복 없음)
```

---

## 마이그레이션 전략

### 1단계: 새 테이블 생성
```sql
-- Facility 테이블
CREATE TABLE facility (
    facility_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    address VARCHAR(500),
    latitude DOUBLE,
    longitude DOUBLE,
    tel VARCHAR(50),
    url VARCHAR(500),
    data_source VARCHAR(20),
    last_synced DATETIME
);

-- Hall 테이블
CREATE TABLE hall (
    hall_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    seat_count INT,
    facility_id VARCHAR(50) NOT NULL,
    last_synced DATETIME,
    FOREIGN KEY (facility_id) REFERENCES facility(facility_id)
);
```

### 2단계: Concert 테이블에 FK 추가
```sql
ALTER TABLE concert ADD COLUMN facility_id VARCHAR(50);
ALTER TABLE concert ADD COLUMN hall_id VARCHAR(50);

ALTER TABLE concert ADD FOREIGN KEY (facility_id) REFERENCES facility(facility_id);
ALTER TABLE concert ADD FOREIGN KEY (hall_id) REFERENCES hall(hall_id);
```

### 3단계: 데이터 마이그레이션
```sql
-- 1. Facility 데이터 추출 및 삽입
INSERT INTO facility (facility_id, latitude, longitude, address, tel, url, data_source, last_synced)
SELECT DISTINCT 
    facility_id,
    latitude,
    longitude,
    detailed_address,
    facility_tel,
    facility_url,
    data_source,
    last_synced
FROM concert
WHERE facility_id IS NOT NULL;

-- 2. Concert의 FK 업데이트
UPDATE concert c
SET c.facility_id = c.facility_id  -- 이미 있는 경우
WHERE c.facility_id IS NOT NULL;
```

### 4단계: Concert에서 중복 컬럼 제거
```sql
ALTER TABLE concert DROP COLUMN latitude;
ALTER TABLE concert DROP COLUMN longitude;
ALTER TABLE concert DROP COLUMN detailed_address;
ALTER TABLE concert DROP COLUMN facility_tel;
ALTER TABLE concert DROP COLUMN facility_url;
```

---

## 기대 효과

### 1. 코드 품질 향상
- Concert 엔티티 책임 명확화
- 코드 가독성 개선
- 유지보수성 향상

### 2. 데이터 무결성 보장
- 시설 정보 일관성 유지
- 중복 데이터 제거
- 데이터 정합성 향상

### 3. 성능 개선
- 테이블 크기 감소
- 인덱스 효율성 증가
- 조회 성능 최적화 가능

### 4. 확장성 확보
- 새로운 데이터 소스 추가 용이
- 시설별 기능 추가 가능 (시설 검색, 시설별 통계 등)
- Hall 별 상세 정보 관리 가능

---

## 리스크 및 고려사항

### 기술적 리스크
1. **데이터 마이그레이션 실패 가능성**
   - 대응: 백업 필수, 롤백 계획 수립
   
2. **JOIN 쿼리 증가로 인한 성능 저하**
   - 대응: 적절한 인덱스 설정, 필요시 fetch join 사용

3. **기존 코드와의 호환성 문제**
   - 대응: DTO 레이어에서 변환 로직 처리

### 비즈니스 리스크
1. **개발 일정 지연**
   - 예상 작업 기간: 2-3일
   - 대응: 기능 개발과 병행하지 않고 별도 스프린트로 진행

2. **배포 후 버그 발생 가능성**
   - 대응: 철저한 테스트, 단계적 배포

---

## 실행 시점

### 우선순위: Medium
다음 상황 중 하나에 해당하면 즉시 실행:

1. **데이터 양 급증**: Concert가 10,000건 이상
2. **시설 정보 변경 빈번**: 전화번호, URL 등 자주 업데이트
3. **새 데이터 소스 추가**: 인터파크, 예스24 등 API 연동 시
4. **시설 기반 기능 추가**: 주변 공연장 검색, 시설별 통계 등
5. **성능 문제 발생**: 조회 속도 저하 등

### 권장 실행 시점
- Issue #1 (시설 정보 추가) 완료 직후
- 또는 다음 스프린트 시작 전

---

## 참고 자료

### 관련 Issue
- Issue #1: 카카오맵 표시를 위한 시설 정보 추가

### 참고 패턴
- Domain-Driven Design (DDD)
- Database Normalization
- Embedded Value Objects
