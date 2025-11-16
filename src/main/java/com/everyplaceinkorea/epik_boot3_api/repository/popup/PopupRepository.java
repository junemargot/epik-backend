package com.everyplaceinkorea.epik_boot3_api.repository.popup;

import com.everyplaceinkorea.epik_boot3_api.admin.contents.popup.enums.Status;
import com.everyplaceinkorea.epik_boot3_api.entity.popup.Popup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface PopupRepository extends JpaRepository<Popup, Long>{
    @Query("SELECT c FROM Popup c WHERE " +
            "(:searchType IS NULL OR " +
            "(:searchType = 'ALL' AND (c.title LIKE %:keyword% OR c.content LIKE %:keyword%)) OR " +
            "(:searchType = 'TITLE' AND c.title LIKE %:keyword%) OR " +
            "(:searchType = 'CONTENT' AND c.content LIKE %:keyword%) OR " +
            "(:searchType = 'WRITER' AND c.member.nickname LIKE %:keyword%)) AND " +
            "(:keyword IS NULL OR " +
            "(:searchType = 'ALL' AND (c.title LIKE %:keyword% OR c.content LIKE %:keyword%)) OR " +
            "(:searchType = 'TITLE' AND c.title LIKE %:keyword%) OR " +
            "(:searchType = 'CONTENT' AND c.content LIKE %:keyword%) OR " +
            "(:searchType = 'WRITER' AND c.member.nickname LIKE %:keyword%))")
    Page<Popup> searchPopup(@Param("keyword") String keyword,
                            @Param("searchType") String searchType,
                            Pageable pageable); //페이징과 정렬기능을 쉽게 구현하기 위해(대규모 데이터 효율적

    // 이주의 신규 팝업 - 카테고리별
//    @Query("SELECT p FROM Popup p " + "JOIN p.popupCategory c " + "WHERE c.id = :categoryId " + "AND p.startDate = :startDate")
//    Page<Popup> findByCategoryAndStartDate(@Param("categoryId") Long categoryId,
//                                           @Param("startDate") LocalDate startDate,
//                                           Pageable pageable);

    @Query("SELECT p FROM Popup p " +
            "JOIN p.popupCategory c " +
            "WHERE c.id = :categoryId " +
            "AND :currentDate BETWEEN p.startDate AND p.endDate")
    Page<Popup> findByPopupCategoryAndStartDate(@Param("categoryId") Long categoryId,
                                            @Param("currentDate") LocalDate currentDate,
                                            Pageable pageable);

    // 이주의 신규 팝업 - 지역별
//    @Query("SELECT p FROM Popup p " + "JOIN p.popupRegion c " + "WHERE c.id = :regionId " + "AND p.startDate = :startDate")
//    Page<Popup> findByPopupRegionAndStartDate(@Param("regionId") Long regionId,
//                                        @Param("startDate") LocalDate startDate,
//                                        Pageable pageable);

    // 이주의 신규 팝업 - 지역별
    @Query("SELECT p FROM Popup p " +
            "LEFT JOIN p.popupRegion c " +
            "WHERE (:regionId IS NULL OR c.id = :regionId) " +
            "AND p.endDate >= :currentDate")
    Page<Popup> findByPopupRegionAndStartDate(@Param("regionId") Long regionId,
                                              @Param("currentDate") LocalDate currentDate,
                                              Pageable pageable);

    //카테고리 선택 후 지역 선택
//    @Query("SELECT p FROM Popup p " + "JOIN p.popupRegion r " + "WHERE r.id = :regionId " + "AND p.popupCategory.id = :categoryId " + "AND p.startDate = :startDate")
//    Page<Popup> findByCategoryAndRegionAndStartDate(@Param("categoryId") Long categoryId,
//                                                    @Param("regionId") Long regionId,
//                                                    @Param("startDate") LocalDate startDate,
//                                                    Pageable pageable);

    @Query("SELECT p FROM Popup p " +
            "WHERE p.popupCategory.id = :categoryId " +
            "AND p.popupRegion.id = :regionId " +
            "AND :currentDate BETWEEN p.startDate AND p.endDate")
    Page<Popup> findByPopupCategoryAndPopupRegionAndStartDate(@Param("categoryId") Long categoryId,
                                                    @Param("regionId") Long regionId,
                                                    @Param("currentDate") LocalDate currentDate,
                                                    Pageable pageable);


//    @Query("SELECT p FROM Popup p " +
//            "JOIN p.popupRegion r " +
//            "WHERE r.id = :regionId " +
//            "AND p.popupCategory.id = :categoryId " +
//            "AND p.startDate >= :startDate AND p.startDate < :endDate") // startDate 범위로 조건을 변경
//    Page<Popup> findByCategoryAndRegionAndStartDate(@Param("categoryId") Long categoryId,
//                                                    @Param("regionId") Long regionId,
//                                                    @Param("startDate") LocalDate startDate,
//                                                    @Param("endDate") LocalDate endDate, // endDate 파라미터 추가
//                                                    Pageable pageable);



    //epik pick 랜덤이미지조회
    @Query(value = "SELECT * FROM popup ORDER BY RAND() LIMIT 10", nativeQuery = true)
    List<Popup> getPopupsByRandom();

    @Query(value = "SELECT * FROM popup WHERE end_date >= :today ORDER BY RAND() LIMIT 10", nativeQuery = true)
    List<Popup> findActivePopupByRandom(@Param("today") LocalDate today);

    long countByStatus(Status status);

    long countByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Status status, LocalDate startDate, LocalDate endDate);

    long countByWriteDateBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT pr.region, COUNT(p) " +
            "FROM Popup p " +
            "JOIN p.popupRegion pr " +
            "WHERE p.status = :status " +
            "GROUP BY pr.region")
    List<Object[]> countByRegionGrouped(@Param("status") Status status);

}
