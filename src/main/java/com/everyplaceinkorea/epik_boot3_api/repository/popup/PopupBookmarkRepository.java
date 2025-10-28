package com.everyplaceinkorea.epik_boot3_api.repository.popup;

import com.everyplaceinkorea.epik_boot3_api.entity.concert.ConcertBookmark;
import com.everyplaceinkorea.epik_boot3_api.entity.concert.ConcertBookmarkId;
import com.everyplaceinkorea.epik_boot3_api.entity.popup.PopupBookmark;
import com.everyplaceinkorea.epik_boot3_api.entity.popup.PopupBookmarkId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PopupBookmarkRepository extends JpaRepository<PopupBookmark, PopupBookmarkId> {
    @Query("SELECT pb FROM PopupBookmark pb WHERE pb.member.id = :memberId AND pb.isActive = true")
    List<PopupBookmark> findPopupBookmarksByMemberId(@Param("memberId") Long memberId);

    // 특정 팝업의 특정 회원 북마크 조회
    @Query("SELECT pb FROM PopupBookmark pb WHERE pb.popup.id = :popupId AND pb.member.id = :memberId")
    Optional<PopupBookmark> findByPopupIdAndMemberId(
            @Param("popupId") Long popupId,
            @Param("memberId") Long memberId
    );
}
