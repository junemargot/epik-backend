package com.everyplaceinkorea.epik_boot3_api.anonymous.feed.service;

import com.everyplaceinkorea.epik_boot3_api.anonymous.feed.dto.FeedCommentDto;
import com.everyplaceinkorea.epik_boot3_api.anonymous.feed.dto.FeedImageDto;
import com.everyplaceinkorea.epik_boot3_api.anonymous.feed.dto.FeedResponseDto;
import com.everyplaceinkorea.epik_boot3_api.entity.comment.FeedComment;
import com.everyplaceinkorea.epik_boot3_api.entity.feed.Feed;
import com.everyplaceinkorea.epik_boot3_api.entity.feed.FeedImage;
import com.everyplaceinkorea.epik_boot3_api.repository.comment.FeedCommentRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.feed.FeedImageRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.feed.FeedLikeRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.feed.FeedRepository;
import com.everyplaceinkorea.epik_boot3_api.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefaultFeedService implements FeedService {

    private final FeedRepository feedRepository;
    private final FeedImageRepository feedImageRepository;
    private final FeedCommentRepository feedCommentRepository;
    private final FeedLikeRepository feedLikeRepository;

    @Override
    public List<FeedResponseDto> getFeeds(Long lastId) {
        Pageable pageable = PageRequest.of(0, 15, Sort.by("id").ascending());

        // 전체 피드 조회
        List<Feed> feeds = feedRepository.findFeedsByLastId(lastId, pageable);

        // 공통 변환 로직 호출
        return convertFeedToDto(feeds);
    }

    @Override
    public List<FeedResponseDto> getByCategories(Long categoryId, Long lastId) {
        Pageable pageable = PageRequest.of(0, 15, Sort.by("id").ascending());

        // 카테고리별 피드 조회
        List<Feed> feeds = feedRepository.findFeedsByCategoryIdAndLastId(categoryId, lastId, pageable);

        // 공통 변환 로직 호출
        return convertFeedToDto(feeds);
    }

    /**
     * Feed 엔티티 리스트를 FeedResponseDto 리스트로 변환하는 공통 메서드
     *
     * @param feeds 변환할 피드 엔티티 리스트
     * @return 변환된 DTO 리스트
     */
    private List<FeedResponseDto> convertFeedToDto(List<Feed> feeds) {
        // 현재 로그인한 사용자 ID (비회원이면 null)
        Long currentMemberId = getCurrentMemberIdOrNull();
        return feeds.stream()
                .map(feed -> {
                    // 댓글 변환
                    List<FeedCommentDto> comments = feedCommentRepository.findAllByFeedId(feed.getId())
                            .stream()
                            .map(comment -> FeedCommentDto.builder()
                                    .writer(comment.getMember().getNickname())
                                    .writeDate(comment.getWriteDate())
                                    .content(comment.getContent())
                                    .build())
                            .toList();

                    // 이미지 변환
                    List<FeedImageDto> images = feedImageRepository.findAllByFeedId(feed.getId())
                            .stream()
                            .map(image -> FeedImageDto.builder()
                                    .imageSaveName(image.getImageSaveName())
                                    .imagePath(image.getImagePath())
                                    .build())
                            .collect(Collectors.toList());

                    // 좋아요 여부 확인
                    Boolean isLiked = false;
                    if (currentMemberId != null) {
                        isLiked = feedLikeRepository.existsByFeedIdAndMemberId(
                                feed.getId(), currentMemberId
                        );
                    }

                    String profileImagePath = null;
                    if(feed.getMember().getProfileImg() != null) {
                        profileImagePath = "/uploads/images/user/" + feed.getMember().getProfileImg();
                    }

                    // DTO 생성
                    return FeedResponseDto.builder()
                            .feedId(feed.getId())
                            .writer(feed.getMember().getNickname())
                            .writerProfileImage(profileImagePath)
                            .writeDate(feed.getWriteDate())
                            .likeCount(feed.getLikeCount())
                            .commentCount(feed.getCommentCount())
                            .content(feed.getContent())
                            .comments(comments)
                            .images(images)
                            .isLiked(isLiked)
                            .categoryId(feed.getCategory().getId())
                            .categoryName(feed.getCategory().getCategory())
                            .build();
                })
                .toList();
    }

    /**
     * 현재 로그인한 사용자 ID를 반환, 비회면이면 null 반환
     */
    private Long getCurrentMemberIdOrNull() {
        try {
            return SecurityUtil.getCurrentMemberId();
        } catch (IllegalStateException e) {
            return null;
        }
    }
}
