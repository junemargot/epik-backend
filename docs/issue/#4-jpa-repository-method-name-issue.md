# Issue: Spring Data JPA Repository 메소드 이름과 엔티티 속성 불일치로 인한 트랜잭션 롤백 오류

## 1. 문제 현상

애플리케이션 시작 시, 서버는 정상적으로 구동되는 것처럼 보이지만 아래와 같은 `DEBUG` 레벨의 오류 로그가 반복적으로 발생합니다.

```
JDBC transaction marked for rollback-only (exception provided for stack trace)
java.lang.Exception: exception just for purpose of providing stack trace
    at org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorImpl$TransactionDriverControlImpl.markRollbackOnly(JdbcResourceLocalTransactionCoordinatorImpl.java:309)
    at org.hibernate.internal.AbstractSharedSessionContract.buildNamedQuery(AbstractSharedSessionContract.java:1109)
    ...
```

이 오류는 데이터베이스 트랜잭션이 롤백되도록 표시되었음을 의미하며, 데이터 무결성에 문제를 일으킬 수 있는 잠재적 위험 요소입니다.

## 2. 원인 분석

오류 스택 트레이스의 `buildNamedQuery` 및 `createNamedQuery` 호출은 Spring Data JPA가 Named Query를 찾거나 생성하는 과정에서 문제가 발생했음을 시사합니다.

1.  프로젝트 내에서 `@NamedQuery` 어노테이션을 직접 사용하는 곳은 없는 것으로 확인되었습니다.
2.  따라서 Spring Data JPA가 Repository 인터페이스의 **메소드 이름을 기반으로 쿼리를 추론하고 생성**하는 과정에서 문제가 발생하는 것으로 추정됩니다.
3.  분석 결과, `PopupRepository.java` 인터페이스의 다음 메소드에서 원인을 찾았습니다.

    ```java
    // PopupRepository.java

    @Query("SELECT p FROM Popup p " + "JOIN p.popupRegion c " + "WHERE c.id = :regionId " + "AND p.startDate = :startDate")
    Page<Popup> findByRegionAndStartDate(@Param("regionId") Long regionId,
                                        @Param("startDate") LocalDate startDate,
                                        Pageable pageable);
    ```

4.  **문제의 핵심**은 메소드 이름인 `findByRegionAndStartDate`와 `@Query` 내부에서 실제 사용하는 엔티티 속성 이름인 `popupRegion`이 일치하지 않는다는 점입니다.
    -   **메소드 이름:** `findByRegion...` -> `Popup` 엔티티 내의 `region` 속성을 찾으려고 시도합니다.
    -   **JPQL 쿼리:** `JOIN p.popupRegion...` -> 실제 엔티티 속성은 `popupRegion`임을 나타냅니다.

    `@Query` 어노테이션이 명시적으로 쿼리를 지정하므로 런타임 시에는 문제가 없어 보일 수 있습니다. 하지만 애플리케이션 시작 단계에서 Spring Data JPA가 Repository 인터페이스를 파싱하고 검증하는 과정에서 이 불일치를 감지하고 내부적인 오류를 발생시키는 것으로 보입니다. 이 오류가 트랜잭션에 영향을 주어 "rollback-only" 로그가 남게 됩니다.

## 3. 해결 방안

메소드 이름과 실제 엔티티 속성 이름 사이의 불일치를 해결하여 Spring Data JPA의 쿼리 생성 메커니즘에서 혼란을 없애야 합니다.

`PopupRepository.java`의 `findByRegionAndStartDate` 메소드 이름을 `findByPopupRegionAndStartDate` 와 같이 실제 속성명을 반영하도록 수정하거나, 혼동의 여지가 없는 다른 이름으로 변경합니다.

**수정 제안:**

```java
// PopupRepository.java

// AS-IS
Page<Popup> findByRegionAndStartDate(...)

// TO-BE (Option 1: 속성명과 일치)
Page<Popup> findByPopupRegionAndStartDate(...)

// TO-BE (Option 2: 혼동을 피하는 새로운 이름)
Page<Popup> findPopupsByRegionAndDate(...)
```
