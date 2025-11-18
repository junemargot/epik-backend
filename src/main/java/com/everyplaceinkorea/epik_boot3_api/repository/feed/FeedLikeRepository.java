package com.everyplaceinkorea.epik_boot3_api.repository.feed;

import com.everyplaceinkorea.epik_boot3_api.entity.feed.FeedLike;
import com.everyplaceinkorea.epik_boot3_api.entity.feed.FeedLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedLikeRepository extends JpaRepository<FeedLike, FeedLikeId> {
    boolean existsByFeedIdAndMemberId(Long feedId, Long memberId);
    // feedId와 memberId로 레코드 한 개 조회
    FeedLike findByFeedIdAndMemberId(Long feedId, Long memberId);

//    회원이 좋아요 한 피드 ID 목록 조회

    @Query("SELECT fl.feedId FROM FeedLike fl WHERE fl.memberId = :memberId AND fl.isActive = true")
    List<Long> findFeedIdsByMemberId(@Param("memberId") Long memberId);

}
