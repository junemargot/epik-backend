package com.everyplaceinkorea.epik_boot3_api.repository.feed;

import com.everyplaceinkorea.epik_boot3_api.entity.feed.Feed;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface FeedRepository extends JpaRepository<Feed, Long> {
  List<Feed> findAllByMemberId(Long memberId);

  // 전제조회
  @Query("SELECT f FROM Feed f WHERE (:lastId IS NULL OR f.id < :lastId) ORDER BY f.id DESC")
  List<Feed> findFeedsByLastId(@Param("lastId") Long lastId, Pageable pageable);

  List<Feed> findAllByCategoryId(Long categoryId);

  @Query("SELECT f from Feed f WHERE f.category.id = :categoryId " +
        "AND (:lastId IS NULL OR f.id > :lastId) " +
        "ORDER BY f.id DESC")
  List<Feed> findFeedsByCategoryIdAndLastId(
          @Param("categoryId") Long categoryId,
          @Param("lastId") Long lastId,
          Pageable pageable
  );

  // 활성 피드만 조회
  @Query("SELECT f FROM Feed f WHERE f.status = 'ACTIVE' " +
          "AND (:lastId IS NULL OR f.id < :lastId) " +
          "ORDER BY f.id DESC")
  List<Feed> findActiveFeedsByLastId(@Param("lastId") Long lastId, Pageable pageable);

  // 카테고리별 활성 피드만 조회
  @Query("SELECT f FROM Feed f WHERE f.category.id = :categoryId " +
          "AND f.status = 'ACTIVE' " +
          "AND (:lastId IS NULL OR f.id < :lastId) " +
          "ORDER BY f.id DESC")
  List<Feed> findActiveFeedsByCategoryIdAndLastId(
          @Param("categoryId") Long categoryId,
          @Param("lastId") Long lastId,
          Pageable pageable
  );

  // 회원의 피드 조회
  @Query("SELECT f FROM Feed f " +
          "JOIN FETCH f.member " +
          "WHERE f.member.id = :memberId " +
          "AND f.status = 'ACTIVE' " +
          "ORDER BY f.writeDate DESC")
  List<Feed> findActiveMyFeeds(@Param("memberId") Long memberId);

  @Query("SELECT f FROM Feed f " +
          "JOIN FETCH f.member " +
          "WHERE f.member.id = :memberId " +
          "AND f.category.id = :categoryId " +
          "AND f.status = 'ACTIVE' " +
          "ORDER BY f.writeDate DESC")
  List<Feed> findActiveMyFeedsByCategory(
          @Param("memberId") Long memberId,
          @Param("categoryId") Long categoryId
  );

  /**
   * 좋아요 카운트 원자적 증가
   * FeedLike 테이블과 별개로 Feed 테이블의 likeCount만 증가
   */
  @Modifying
  @Transactional
  @Query("UPDATE Feed f SET f.likeCount = f.likeCount + 1 WHERE f.id = :feedId AND f.likeCount >= 0")
  int incrementLikeCount(@Param("feedId") Long feedId);

  @Modifying
  @Transactional
  @Query("UPDATE Feed f SET f.likeCount = f.likeCount - 1 WHERE f.id = :feedId AND f.likeCount > 0")
  int decrementLikeCount(@Param("feedId") Long feedId);

  /**
   * 댓글 카운트 원자적 증가
   */
  @Modifying
  @Transactional
  @Query("UPDATE Feed f SET f.commentCount = f.commentCount + 1 WHERE f.id = :feedId")
  int incrementCommentCount(@Param("feedId") Long feedId);

  @Modifying
  @Transactional
  @Query("UPDATE Feed f SET f.commentCount = f.commentCount - 1 WHERE f.id = :feedId AND f.commentCount > 0")
  int decrementCommentCount(@Param("feedId") Long feedId);

  @Query("SELECT f FROM Feed f " +
          "WHERE (:keyword IS NULL OR :searchType IS NULL) " +
          "OR (:searchType = 'content' AND f.content LIKE %:keyword%) " +
          "OR (:searchType = 'writer' AND f.member.nickname LIKE %:keyword%) " +
          "OR (:searchType = 'all' AND (f.content LIKE %:keyword% OR f.member.nickname LIKE %:keyword%))")
  Page<Feed> searchFeed(@Param("keyword") String keyword,
                        @Param("searchType") String searchType,
                        Pageable pageable);
}
