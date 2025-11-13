package com.everyplaceinkorea.epik_boot3_api.repository.exhibition;

import com.everyplaceinkorea.epik_boot3_api.entity.exhibition.Exhibition;
import com.everyplaceinkorea.epik_boot3_api.entity.musical.Musical;
import com.everyplaceinkorea.epik_boot3_api.entity.musical.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ExhibitionRepository extends JpaRepository<Exhibition, Long> {

  @Query("SELECT e FROM Exhibition e WHERE " +
          "(:searchType IS NULL OR " +
          "(:searchType = 'ALL' AND (e.title LIKE %:keyword% OR e.content LIKE %:keyword% OR e.member.nickname LIKE %:keyword%)) OR " +
          "(:searchType = 'TITLE' AND e.title LIKE %:keyword%) OR " +
          "(:searchType = 'CONTENT' AND e.content LIKE %:keyword%) OR " +
          "(:searchType = 'WRITER' AND e.member.nickname LIKE %:keyword%)) AND " +
          "(:keyword IS NULL OR " +
          "(:searchType = 'ALL' AND (e.title LIKE %:keyword% OR e.content LIKE %:keyword% OR e.member.nickname LIKE %:keyword%)) OR " +
          "(:searchType = 'TITLE' AND e.title LIKE %:keyword%) OR " +
          "(:searchType = 'CONTENT' AND e.content LIKE %:keyword%) OR " +
          "(:searchType = 'WRITER' AND e.member.nickname LIKE %:keyword%))")
  Page<Exhibition> searchExhibition(@Param("keyword") String keyword,
                              @Param("searchType") String searchType,
                              Pageable pageable);

  @Query("SELECT e FROM Exhibition e WHERE (:regionId IS NULL OR e.region.id = :regionId) AND e.endDate >= :endDate AND e.status = 'ACTIVE'")
  Page<Exhibition> findExhibitionsByRegion(@Param("regionId") Long regionId, @Param("endDate") LocalDate endDate, Pageable pageable);

    // 랜덤이미지조회
  @Query(value = "SELECT * FROM exhibition ORDER BY RAND() LIMIT 10", nativeQuery = true)
  List<Exhibition> findExhibitionByRandom();

  @Query(value = "SELECT * FROM exhibition WHERE end_date >= :today ORDER BY RAND() LIMIT 10", nativeQuery = true)
  List<Exhibition> findActiveExhibitionByRandom(@Param("today") LocalDate today);

  long countByStatus(Status status);

  long countByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
          Status status, LocalDate startDate, LocalDate endDate);

  long countByWriteDateBetween(LocalDateTime start, LocalDateTime end);

  @Query("SELECT er.region, COUNT(e) " +
          "FROM Exhibition e " +
          "JOIN e.region er " +
          "WHERE e.status = :status " +
          "GROUP BY er.region")
  List<Object[]> countByRegionGrouped(@Param("status") Status status);
}
