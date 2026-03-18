# KOPIS 동기화 로직 개선

## 개요

KOPIS API 공식 문서 기반으로 동기화 로직을 검토한 결과, 데이터 누락 가능성과 필터링 미비 등 개선이 필요한 사항을 정리합니다.

**작성일**: 2025-03-16  
**브랜치**: `feature/#25-kopis-sync-improvements`  

---

## 수정 사항 목록

| 우선순위 | 항목 | 영향도 | 수정 범위 |
|---------|------|--------|----------|
| 🔴 P0 | 31일 날짜 범위 분할 미처리 | 데이터 누락 | KopisDataSyncService |
| 🟡 P1 | child/visit 필드 추가 | 기능 확장 | DTO, Entity, Repository, SyncService |
| 🟡 P1 | 아동 공연 조회 필터링 | UX 개선 | ConcertRepository, MusicalRepository |
| 🟠 P2 | URI 이중 빌드 버그 | 잠재적 오류 | KopisApiService |
| 🟠 P2 | 상세 조회 Rate Limiting | 서버 부하 | KopisDataSyncService |

---

## 🔴 P0. 31일 날짜 범위 분할 미처리

### 문제

KOPIS API 공식 문서에 따르면 `stdate`~`eddate` 간격은 **최대 31일**로 제한됩니다.  
현재 스케줄러에서 `now ~ now+6개월`(약 180일)을 한 번에 전달하고 있어 **앞쪽 31일분만 응답**되고 나머지 기간의 공연이 누락될 수 있습니다.

### 현재 코드

```java
// KopisDataSyncScheduler.java
String endDate = now.plusMonths(6).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
syncService.syncConcerts(startDate, endDate);  // 180일을 그대로 전달
```

### 해결 방안

`syncByGenreInChunks` 메서드를 추가하여 31일 단위로 분할 호출합니다.

### 수정 파일
- `KopisDataSyncService.java`
  - `syncByGenreInChunks()` 메서드 신규 추가
  - `syncConcerts()` 내부 호출을 `syncByGenrePaginated` → `syncByGenreInChunks`로 변경
  - `syncMusicals()` 동일하게 변경

---

## 🟡 P1. child/visit 필드 추가

### 목적

- `child`: 아동 공연을 일반 목록에서 제외하기 위한 필터링 필드
- `visit`: 내한 공연(해외 아티스트) 모아보기 기능 지원

### 수정 파일

**① KopisPerformanceDto.java** - 필드 추가
```java
private String child;   // 아동 공연 여부 (Y/N)
private String visit;   // 내한 공연 여부 (Y/N)
```

**② KopisDataSyncService.java** - 파싱 + 병합
- `parsePerformanceFromXml()`: child, visit 태그 파싱 추가
- `mergeAllDetailInfo()`: 상세 정보 병합에 child, visit 추가

**③ Concert.java** - 엔티티 컬럼 + 매핑
```java
@Column(name = "kopis_child")
private String kopisChild;

@Column(name = "kopis_visit")
private String kopisVisit;
```
- `setKopisOriginalData()`, `updateFromKopisData()`에 반영

**④ Musical.java** - Concert와 동일하게 적용

---

## 🟡 P1. 아동 공연 조회 필터링

### 방식

DB에 저장은 하되, 사용자 조회 API에서 `kopis_child != 'Y'` 조건으로 제외합니다.  
수동 등록 데이터(`kopis_child IS NULL`)는 정상 노출됩니다.

### 수정 파일

**ConcertRepository.java** - 5개 쿼리에 조건 추가
- `findConcertsByRegion`
- `findConcertsByGenre`
- `findAllConcertsByGenre`
- `findActiveConcertByRandom` (native query)
- `findAllActiveConcertByRandom` (native query)

**MusicalRepository.java** - 2개 쿼리에 조건 추가
- `findMusicalsByRegion`
- `findActiveMusicalByRandom` (native query)

### 추가 조건 (JPQL)
```sql
AND (c.kopisChild IS NULL OR c.kopisChild != 'Y')
```

### 추가 조건 (native query)
```sql
AND (kopis_child IS NULL OR kopis_child != 'Y')
```

---

## 🟠 P2. URI 이중 빌드 버그

### 문제

`KopisApiService.getPerformanceList()`에서 디버깅용 로깅 코드가 남아있어, 같은 `uriBuilder`에 `.path("/pblprfr")`를 두 번 호출합니다.  
실제 동기화는 `getPerformanceListByGenre()`를 사용하므로 현재 영향은 없지만, `getPerformanceList()`를 직접 호출하면 `/pblprfr/pblprfr` 경로로 요청됩니다.

### 수정 파일
- `KopisApiService.java`: `getPerformanceList()` 메서드의 `.uri()` 블록 정리

---

## 🟠 P2. 상세 조회 Rate Limiting

### 문제

페이지 간 500ms 딜레이는 있지만, 개별 공연의 상세 정보 조회(`fetchAndMergeDetailToDto`)에는 딜레이가 없습니다.  
한 페이지에 신규 공연이 많으면 상세 API를 연속 호출하여 KOPIS 서버에 부담을 줄 수 있습니다.

### 해결 방안

`syncByGenrePaginated` for 루프 내에 200ms 딜레이를 추가합니다.

### 수정 파일
- `KopisDataSyncService.java`: `syncByGenrePaginated()` 내 for 루프

---

## 체크리스트

- [ ] P0: `syncByGenreInChunks` 31일 분할 메서드 추가
- [ ] P1: `KopisPerformanceDto`에 child, visit 필드 추가
- [ ] P1: `KopisDataSyncService` 파싱/병합 로직 추가
- [ ] P1: `Concert`, `Musical` 엔티티에 kopisChild, kopisVisit 컬럼 추가
- [ ] P1: `ConcertRepository` 조회 쿼리 5개에 아동 필터 추가
- [ ] P1: `MusicalRepository` 조회 쿼리 2개에 아동 필터 추가
- [ ] P2: `KopisApiService.getPerformanceList()` URI 이중 빌드 수정
- [ ] P2: 상세 조회 Rate Limiting 200ms 딜레이 추가
- [ ] 전체 동기화 테스트 (콘서트 + 뮤지컬)
- [ ] 아동 공연 필터링 API 테스트

---

**문서 버전**: 1.0  
**작성자**: 황의정
