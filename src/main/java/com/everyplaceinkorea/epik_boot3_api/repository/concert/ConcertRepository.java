package com.everyplaceinkorea.epik_boot3_api.repository.concert;

import com.everyplaceinkorea.epik_boot3_api.entity.common.DataSource;
import com.everyplaceinkorea.epik_boot3_api.entity.concert.Concert;
import com.everyplaceinkorea.epik_boot3_api.entity.musical.Musical;
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

  @Query("SELECT c FROM Concert c WHERE (:regionId IS NULL OR c.region.id = :regionId) AND c.endDate >= :endDate AND c.status = 'ACTIVE'")
  Page<Concert> findConcertsByRegion(@Param("regionId") Long regionId, @Param("endDate") LocalDate endDate, Pageable pageable);

  // 랜덤이미지조회 - JPQL 버전으로 수정
  @Query(value = "SELECT * FROM concert ORDER BY RAND() LIMIT 10", nativeQuery = true)
  List<Concert> findConcertByRandom();

  @Query(value = "SELECT * FROM concert WHERE end_date >= :today ORDER BY RAND() LIMIT 10", nativeQuery = true)
  List<Concert> findActiveConcertByRandom(@Param("today") LocalDate today);

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
          "AND (c.kopisTicketOffices IS NULL OR c.kopisTicketOffices = '{}' OR c.kopisTicketOffices = '')")
  Page<Concert> findConcertsWithoutTicketOffices(Pageable pageable);

  @Query("SELECT c FROM Concert c WHERE c.kopisId IS NOT NULL " +
          "AND (c.kopisTicketOffices IS NULL OR c.kopisTicketOffices = '{}' OR c.kopisTicketOffices = '')")
  List<Concert> findConcertsWithoutTicketOffices();
}
