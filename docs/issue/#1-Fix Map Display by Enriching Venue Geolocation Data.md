### 이슈 개요
팝업 컨텐츠를 제외한 컨텐츠(콘서트, 뮤지컬 등) 상세 페이지에서 카카오맵이 공연장의 위치를 정상적으로 표시하지 못하는 문제

- 발생 위치: 예) /pages/concert/[id]/index.vue
- 영향 범위: 콘서트 및 뮤지컬 상세 페이지의 공연장 위치 정보 표시
- 우선순위: High (사용자 경험에 직접적 영향)
  현재 상태 (AS-IS)
  문제점
  부정확한 주소 데이터

```json
// 백엔드에서 제공하는 주소 형태
address: "서울 홍익대 대학로 아트센터"  // 공연장명만 포함
```
KOPIS API 연동 후 상세 주소 매핑이 제대로 이루어지지 않음.
받은 데이터: area + fcltynm (예: "서울" + "홍익대 대학로 아트센터")
카카오맵 API가 인식하기 어려운 형태의 주소 정보
좌표 정보 부재
```json
{
  "address": "서울 홍익대 대학로 아트센터",
  "latitude": null,    //  위도 정보 없음
  "longitude": null    //  경도 정보 없음
}
```

### 목표 상태 (TO-BE)
1. 해결 후 기대 효과
    - 정확한 지도 표시
    - KOPIS 공연시설 API를 통한 정확한 좌표 정보 획득
    - 카카오맵에서 정확한 공연장 위치 표시
2. 개선된 사용자 경험
    - 콘서트 상세 페이지에서 공연장 위치를 한눈에 확인
    - 공연장 클릭 시 공연장 상세 정보 팝업 표시
    - 길찾기 연동 가능

3. 데이터 품질 향상
```json
{
  "address": "서울특별시 종로구 대명길 74",  // 정확한 도로명 주소
  "latitude": 37.52112,                    // 정확한 위도
  "longitude": 127.128363,                 // 정확한 경도
  "venue": "홍익대 대학로 아트센터",
  "hasMapData": true
}
```

### 근본 원인 분석

| 원인 | 현재 상태 | 해결 방안 |
|------|-----------|-----------|
| **주소 데이터 부정확** | `area + fcltynm` 조합 (예: 서울특별시 예술의전당) | KOPIS 시설상세조회 API 활용 |
| **좌표 정보 없음** | latitude/longitude가 null | 시설 API에서 위도/경도 추출 |
| **프론트엔드 미구현** | 정확한 주소가 추출되지 않아 맵이 출력되지 않음 | 카카오맵 초기화 로직 구현 |
| **API 연동 부족** | 공연 정보만 조회 | 시설 정보 추가 조회 |

### 구현 계획

#### 1단계: 백엔드 - 좌표 데이터 수집

```java
// KopisApiService.java에 추가
public String getFacilityDetail(String facilityId) {
    // KOPIS 공연시설상세조회 API 호출
    // URL: /prfplc/{facilityId}
}

// Concert 엔티티에 필드 추가  
@Column(name = "latitude") private Double latitude;
@Column(name = "longitude") private Double longitude;
@Column(name = "detailed_address") private String detailedAddress;
```

2단계: 백엔드 - 동기화 로직 개선

```java
// KopisDataSyncService.java 확장
private void syncSingleConcertEnhanced() {
    // 1. 공연 정보 조회 (기존)
    // 2. 시설명으로 시설ID 조회 (신규)
    // 3. 시설ID로 좌표 정보 조회 (신규)  
    // 4. Concert 엔티티에 좌표 정보 저장 (신규)
}
```

#### 3단계: 백엔드 - 기존 데이터 보강

```java
// 기존 콘서트들에 좌표 정보 추가
@Transactional
public void enrichExistingConcertsWithCoordinates() {
    // KOPIS 콘서트들을 대상으로 좌표 정보 보강
}
```

#### 4단계: 프론트엔드 확인
```javascript
<script setup>
// 카카오맵 초기화 로직 구현
// 좌표 우선 → 주소 지오코딩 → 공연장명 검색 순서로 대체
</script>
```

#### 5단계: 프론트엔드 - 콘서트 상세 페이지 연동

```javscript
<!-- pages/concert/[id]/index.vue -->
<EventLocation 
  v-if="concert.hasMapData || concert.address"
  :address="concert.detailedAddress || concert.address"
  :venue-name="concert.venue"
  :latitude="concert.latitude"
  :longitude="concert.longitude"
/>
```

---

## 완료 조건

### 백엔드 요구사항
- [ ] KOPIS 시설상세조회 API 연동 완료
- [ ] Concert 엔티티에 latitude, longitude, detailedAddress 필드 추가
- [ ] 신규 동기화 시 좌표 정보 자동 수집
- [ ] 기존 KOPIS 콘서트 데이터에 좌표 정보 보강
- [ ] API 응답에 hasMapData 플래그 포함

### 프론트엔드 요구사항
- [ ] EventLocation.vue 컴포넌트 카카오맵 초기화 로직 구현
- [ ] 좌표 우선 → 주소 대안 → 공연장명 검색 순서로 지도 표시
- [ ] 콘서트 상세 페이지에서 EventLocation 컴포넌트 활성화
- [ ] 지도 표시 실패 시 적절한 오류 메시지 표시
- [ ] 반응형 디자인 적용 (모바일/데스크톱)

### 품질 검증
- [ ] 주요 공연장 10곳 이상에서 정확한 위치 표시 확인
- [ ] 지도 로딩 성능 최적화 (3초 이내 표시)
- [ ] 다양한 브라우저에서 정상 동작 확인
- [ ] 모바일 환경에서 지도 조작 가능

---

## 관련 파일

### 백엔드
- `/src/main/java/com/everyplaceinkorea/epik_boot3_api/external/kopis/KopisApiService.java`
- `/src/main/java/com/everyplaceinkorea/epik_boot3_api/external/kopis/service/KopisDataSyncService.java`
- `/src/main/java/com/everyplaceinkorea/epik_boot3_api/entity/concert/Concert.java`

### 프론트엔드
- `/pages/concert/[id]/index.vue`
- `/components/event/EventLocation.vue`
- `/nuxt.config.ts` (카카오맵 API 키 설정)

### 데이터베이스
```sql
-- 추가할 컬럼
ALTER TABLE concert ADD COLUMN latitude DOUBLE;
ALTER TABLE concert ADD COLUMN longitude DOUBLE;  
ALTER TABLE concert ADD COLUMN detailed_address VARCHAR(500);
```

---

## 추가 고려사항

1. **API 사용량 최적화**
    - KOPIS 시설 정보 캐싱으로 중복 호출 방지
    - 카카오맵 API 키 사용량 모니터링

2. **오류 처리**
    - KOPIS API 호출 실패 시 대안 로직
    - 카카오맵 로드 실패 시 텍스트 주소 표시

3. **확장성**
    - 뮤지컬, 전시회 등 다른 공연 타입으로 확장 가능한 구조
    - 좌표 정보를 활용한 주변 정보 제공 기능

4. **성능**
    - 시설 정보 캐싱 전략
    - 지도 컴포넌트 지연 로딩 적용