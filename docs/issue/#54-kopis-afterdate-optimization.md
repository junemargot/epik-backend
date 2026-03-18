# KOPIS afterdate 파라미터 활용 증분 동기화 최적화

## 개요

현재 동기화 시 `shouldSkipSync()`로 매건 DB 조회하여 기존 데이터를 필터링하고 있으나, KOPIS API의 `afterdate` 파라미터를 활용하면 API 단계에서 변경분만 가져올 수 있어 불필요한 API 호출과 DB 조회를 대폭 줄일 수 있습니다.

**작성일**: 2025-03-17  
**브랜치**: `refactor/#26-kopis-afterdate-optimization`  
**관련 이슈**: #26

---

## 현재 방식 vs 개선 방식

### 현재 (shouldSkipSync)
```
API: 6개월치 전체 공연 조회 (예: 1000건)
→ 1000번 DB 조회 (existsByKopisId)
→ 950건 스킵, 50건만 실제 처리
```

### 개선 (afterdate)
```
API: afterdate=마지막동기화일 → 변경분만 응답 (예: 50건)
→ 50건만 처리 (DB 스킵 체크 최소화)
```

---

## 활용 가능한 기존 코드

- `ConcertRepository.findMaxLastSynced()` — 이미 구현되어 있음
- `MusicalRepository.findMaxLastSynced()` — 이미 구현되어 있음
- `KopisApiService.getPerformanceListByGenre()` — `afterdate` 파라미터 추가 필요

## 수정 대상

- `KopisApiService`: `afterdate` 쿼리 파라미터 추가
- `KopisDataSyncService`: 마지막 동기화 시각 조회 → `afterdate`로 전달
- `shouldSkipSync()`: 제거 또는 fallback 용도로 유지

---

**문서 버전**: 1.0  
**작성자**: 황의정
