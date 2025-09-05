package com.everyplaceinkorea.epik_boot3_api.repository.musical;

import com.everyplaceinkorea.epik_boot3_api.entity.common.DataSource;
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
import java.util.Optional;

public interface MusicalRepository extends JpaRepository<Musical, Long> {

//    @Query("SELECT c FROM Musical c WHERE " +
//            "(:searchType = 'ALL' AND (c.title LIKE %:keyword% OR c.content LIKE %:keyword%)) OR " +
//            "(:searchType = 'TITLE' AND c.title LIKE %:keyword%) OR " +
//            "(:searchType = 'CONTENT' AND c.content LIKE %:keyword%) OR " +
//            "(:searchType = 'WRITER' AND c.member.nickname LIKE %:keyword%)")
//    Page<Musical> searchMusical(@Param("keyword") String keyword,
//                                @Param("searchType") String searchType,
//                                Pageable pageable);

    @Query("SELECT m FROM Musical m WHERE " +
            "(:searchType IS NULL OR " +
            "(:searchType = 'ALL' AND (m.title LIKE %:keyword% OR m.content LIKE %:keyword%)) OR " +
            "(:searchType = 'TITLE' AND m.title LIKE %:keyword%) OR " +
            "(:searchType = 'CONTENT' AND m.content LIKE %:keyword%) OR " +
            "(:searchType = 'WRITER' AND m.member.nickname LIKE %:keyword%)) AND " +
            "(:keyword IS NULL OR " +
            "(:searchType = 'ALL' AND (m.title LIKE %:keyword% OR m.content LIKE %:keyword%)) OR " +
            "(:searchType = 'TITLE' AND m.title LIKE %:keyword%) OR " +
            "(:searchType = 'CONTENT' AND m.content LIKE %:keyword%) OR " +
            "(:searchType = 'WRITER' AND m.member.nickname LIKE %:keyword%))")
    Page<Musical> searchMusical(@Param("keyword") String keyword,
                                @Param("searchType") String searchType,
                                Pageable pageable);

    // 상태 필터링 커스텀 쿼리
//    @Query("SELECT m FROM Musical m WHERE " +
//            "(:searchType IS NULL OR " +
//            "(:searchType = 'ALL' AND (m.title LIKE %:keyword% OR m.content LIKE %:keyword%)) OR " +
//            "(:searchType = 'TITLE' AND m.title LIKE %:keyword%) OR " +
//            "(:searchType = 'CONTENT' AND m.content LIKE %:keyword%) OR " +
//            "(:searchType = 'WRITER' AND m.member.nickname LIKE %:keyword%)) AND " +
//            "(:keyword IS NULL OR " +
//            "(:searchType = 'ALL' AND (m.title LIKE %:keyword% OR m.content LIKE %:keyword%)) OR " +
//            "(:searchType = 'TITLE' AND m.title LIKE %:keyword%) OR " +
//            "(:searchType = 'CONTENT' AND m.content LIKE %:keyword%) OR " +
//            "(:searchType = 'WRITER' AND m.member.nickname LIKE %:keyword%)) AND " +
//            "(m.status = :status)")
//    Page<Musical> searchMusicalWithStatus(
//            @Param("keyword") String keyword,
//            @Param("searchType") String searchType,
//            @Param("status") Status status,
//            Pageable pageable);

    @Query("SELECT m FROM Musical m WHERE (:regionId IS NULL OR m.region.id = :regionId) AND m.endDate >= :endDate AND m.status = 'ACTIVE'")
    Page<Musical> findMusicalsByRegion(@Param("regionId") Long regionId, @Param("endDate") LocalDate endDate, Pageable pageable);

    // 랜덤이미지조회 - 모두 nativeQuery로 통일
    @Query(value = "SELECT * FROM musical ORDER BY RAND() LIMIT 10", nativeQuery = true)
    List<Musical> findMusicalByRandom();

    @Query(value = "SELECT * FROM musical WHERE end_date >= :today ORDER BY RAND() LIMIT 10", nativeQuery = true)
    List<Musical> findActiveMusicalByRandom(@Param("today") LocalDate today);

    // KOPIS 관련 메서드 추가
    Optional<Musical> findByKopisId(String kopisId);
    List<Musical> findByDataSource(DataSource dataSource);
    List<Musical> findByDataSourceAndLastSyncedAfter(DataSource dataSource, LocalDateTime dateTime);

}
