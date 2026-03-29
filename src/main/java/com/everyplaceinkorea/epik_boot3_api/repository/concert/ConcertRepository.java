package com.everyplaceinkorea.epik_boot3_api.repository.concert;

import com.everyplaceinkorea.epik_boot3_api.entity.common.DataSource;
import com.everyplaceinkorea.epik_boot3_api.entity.concert.Concert;
import com.everyplaceinkorea.epik_boot3_api.entity.musical.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConcertRepository extends JpaRepository<Concert, Long> {

    @Query("SELECT c FROM Concert c WHERE " +
            "(:searchType IS NULL OR " +
            "(:searchType = 'ALL' AND (c.title LIKE %:keyword% OR c.content LIKE %:keyword% OR c.member.nickname LIKE %:keyword%)) OR " +
            "(:searchType = 'TITLE' AND c.title LIKE %:keyword%) OR " +
            "(:searchType = 'CONTENT' AND c.content LIKE %:keyword%) OR " +
            "(:searchType = 'WRITER' AND c.member.nickname LIKE %:keyword%)) AND " +
            "(:keyword IS NULL OR " +
            "(:searchType = 'ALL' AND (c.title LIKE %:keyword% OR c.content LIKE %:keyword% OR c.member.nickname LIKE %:keyword%)) OR " +
            "(:searchType = 'TITLE' AND c.title LIKE %:keyword%) OR " +
            "(:searchType = 'CONTENT' AND c.content LIKE %:keyword%) OR " +
            "(:searchType = 'WRITER' AND c.member.nickname LIKE %:keyword%))")
    Page<Concert> searchConcert(@Param("keyword") String keyword,
                            @Param("searchType") String searchType,
                            Pageable pageable);

    @Query("SELECT c FROM Concert c WHERE (:regionId IS NULL OR c.region.id = :regionId) AND c.endDate >= :endDate AND c.status = 'ACTIVE' AND (c.kopisChild IS NULL OR c.kopisChild != 'Y')")
    Page<Concert> findConcertsByRegion(@Param("regionId") Long regionId, @Param("endDate") LocalDate endDate, Pageable pageable);

    // 랜덤이미지조회 - JPQL 버전으로 수정
    @Query(value = "SELECT * FROM concert ORDER BY RAND() LIMIT 10", nativeQuery = true)
    List<Concert> findConcertByRandom();

    @Query(value = "SELECT * FROM concert WHERE end_date >= :today AND (kopis_child IS NULL OR kopis_child != 'Y') ORDER BY RAND()", nativeQuery = true)
    Page<Concert> findActiveConcertByRandom(@Param("today") LocalDate today, Pageable pageable);

    @Query(value = "SELECT * FROM concert WHERE end_date > :today AND (kopis_child IS NULL OR kopis_child != 'Y') ORDER BY RAND()", nativeQuery = true)
    List<Concert> findAllActiveConcertByRandom(@Param("today") LocalDate today);

    Optional<Concert> findByKopisId(String kopisId);

    List<Concert> findByDataSource(DataSource dataSource);

    List<Concert> findByDataSourceAndLastSyncedAfter(DataSource dataSource, LocalDateTime dateTime);

    @Query("SELECT c FROM Concert c WHERE c.startDate >= :startDate AND c.endDate <= :endDate")
    List<Concert> findByDateRange(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Query("SELECT c FROM Concert c WHERE c.startDate >= :startDate AND c.startDate <= :endDate")
    List<Concert> findByStartDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(c) FROM Concert c WHERE c.startDate >= :startDate AND c.startDate <= :endDate")
    long countByStartDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<Concert> findByKopisIdIsNotNull();

    boolean existsByKopisId(String kopisId);

    List<Concert> findByFacilityIsNull();

    List<Concert> findByFacilityIsNotNullAndHallIsNull();

    @Query("SELECT c FROM Concert c WHERE c.kopisId IS NOT NULL " +
            "AND (c.kopisTicketOffices IS NULL OR c.kopisTicketOffices = '{}' OR c.kopisTicketOffices = '')" +
            "AND c.kopisTicketOfficesUpdatedAt IS NULL " +
            "AND (c.kopisTicketScrapeFailCount IS NULL OR c.kopisTicketScrapeFailCount < 3)")
    Page<Concert> findConcertsWithoutTicketOffices(Pageable pageable);

    @Query("SELECT c FROM Concert c WHERE c.kopisId IS NOT NULL " +
            "AND (c.kopisTicketOffices IS NULL OR c.kopisTicketOffices = '{}' OR c.kopisTicketOffices = '')"
            +
            "AND c.kopisTicketOfficesUpdatedAt IS NULL " +
            "AND (c.kopisTicketScrapeFailCount IS NULL OR c.kopisTicketScrapeFailCount < 3)")
    List<Concert> findConcertsWithoutTicketOffices();

    @Query("SELECT c FROM Concert c WHERE " +
        "(:genreName IS NULL OR c.kopisGenrenm LIKE %:genreName%) " +
        "AND c.endDate >= :today " +
        "AND c.status = 'ACTIVE' " +
        "AND (c.kopisChild IS NULL OR c.kopisChild != 'Y') " +
        "ORDER BY c.startDate ASC")
    Page<Concert> findConcertsByGenre(@Param("genreName") String genreName, @Param("today") LocalDate today, Pageable pageable);

    @Query("SELECT c FROM Concert c WHERE (:regionId IS NULL OR c.region.id = :regionId) AND c.endDate >= :endDate AND c.status = 'ACTIVE' AND (c.kopisChild IS NULL OR c.kopisChild != 'Y') ORDER BY c.id DESC")
    List<Concert> findAllConcertsByRegion(@Param("regionId") Long regionId, @Param("endDate") LocalDate endDate);

    @Query("SELECT c FROM Concert c WHERE " +
            "(:genreName IS NULL OR c.kopisGenrenm LIKE %:genreName%) " +
            "AND c.endDate >= :today " +
            "AND c.status = 'ACTIVE' " +
            "AND (c.kopisChild IS NULL OR c.kopisChild != 'Y') " +
            "ORDER BY c.startDate ASC")
    List<Concert> findAllConcertsByGenre(@Param("genreName") String genreName, @Param("today") LocalDate today);

    /**
     * 활성화된(ACTIVE) 콘서트 수를 카운트
     *
     * @param status 콘서트 상태 (ACTIVE, DELETE 등)
     * @return 해당 상태의 공연 컨텐츠 수
     *
     * totalConcerts 계산
     * DELETE 상태는 제외하고 실제 활설화된 콘서트만 세기 위함
     */
    long countByStatus(Status status);

    /**
     * 오늘 날짜 기준으로 진행 중인 공연 컨텐츠 수 카운트
     * 조건: status = ACTIVE AND startDate <= today AND endDate >= today
     *
     * @param status 공연 상태
     * @param startDate 비교할 시작 날짜 (today)
     * @param endDate 비교할 종료 날짜 (today)
     * @return 현재 진행 중인 공연 컨텐츠 수
     *
     * ongoingContents를 계산
     * 예: startDate=2024-11-01, endDate=2024-12-31이고 today=2024-11-15라면 진행 중
     */
    long countByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Status status, LocalDate startDate, LocalDate endDate);

    /**
     * 특정 시간 범위 내에 등록(wrtieDate)된 공연 수 카운트
     *
     * @param start 시작 시간
     * @param end 종료 시간
     * @return 오늘 등록된 공연 컨텐츠 수
     *
     * todayContents를 계산
     * writeDate가 오늘인 공연 컨텐츠 수를 세기 위함
     */
    long countByWriteDateBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 지역별로 그룹핑하여 각 지역의 공연 수를 조회
     * SQL: SELECT region.name, COUNT(*) FROM concert
     *      JOIN region ON concert.region_id = region.id
     *      WHERE concert.status = 'ACTIVE'
     *      GROUP BY region.name
     *
     * @param status 공연 상태
     * @return List<Object[]> 형태로 반환
     *          - Object[0]: 지역명 (String)
     *          - Object[1]: 공연 수 (Long)
     *
     * regionStats를 계산
     * 예: [["서울", 150], ["경기", 300]]
     */
    @Query("SELECT cr.region, COUNT(*) " +
            "FROM Concert c " +
            "JOIN c.region cr " +
            "WHERE c.status = :status " +
            "GROUP BY cr.region")
    List<Object[]> countByRegionGrouped(@Param("status") Status status);

    /**
     * 장르별로 그룹핑하여 각 장르의 공연 수를 조회
     * SQL: SELECT kopis_genrenm, COUNT(*) FROM concert
     *      WHERE status = 'ACTIVE' AND kopis_genrenm IS NOT NULL
     *      GROUP BY kopis_genrenm
     *
     * @param status 공연 상태
     * @return List<Object[]> 형태로 반환
     *          - Object[0]: 장르명 (String)
     *          - Object[1]: 공연 수 (Long)
     *
     * genreStats를 계산
     * NULL 체크 이유: KOPIS API에서 동기화된 콘서트만 장르 정보가 있음
     * 예: [["클래식", 300], ["대중음악", 500]]
     */
    @Query("SELECT c.kopisGenrenm, COUNT(c) " +
            "FROM Concert c " +
            "WHERE c.status = :status " +
            "AND c.kopisGenrenm IS NOT NULL " +
            "GROUP BY c.kopisGenrenm")
    List<Object[]> countByGenreGrouped(@Param("status") Status status);

    @Query("SELECT MAX(c.lastSynced) " +
            "FROM Concert c " +
            "WHERE c.dataSource = 'KOPIS_API'")
    Optional<LocalDateTime> findMaxLastSynced();

}
