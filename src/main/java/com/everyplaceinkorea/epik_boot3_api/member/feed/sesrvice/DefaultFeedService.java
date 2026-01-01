package com.everyplaceinkorea.epik_boot3_api.member.feed.sesrvice;

import com.everyplaceinkorea.epik_boot3_api.EditorImage.UploadFolderType;
import com.everyplaceinkorea.epik_boot3_api.anonymous.feed.dto.FeedCommentDto;
import com.everyplaceinkorea.epik_boot3_api.anonymous.feed.dto.FeedImageDto;
import com.everyplaceinkorea.epik_boot3_api.anonymous.feed.dto.FeedResponseDto;
import com.everyplaceinkorea.epik_boot3_api.entity.comment.FeedComment;
import com.everyplaceinkorea.epik_boot3_api.entity.feed.*;
import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import com.everyplaceinkorea.epik_boot3_api.member.feed.dto.FeedCreateDto;
import com.everyplaceinkorea.epik_boot3_api.member.feed.dto.FeedReportDto;
import com.everyplaceinkorea.epik_boot3_api.member.feed.dto.FeedUpdateDto;
import com.everyplaceinkorea.epik_boot3_api.repository.Member.MemberRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.comment.FeedCommentRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.feed.*;
import com.everyplaceinkorea.epik_boot3_api.util.SecurityUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultFeedService implements FeedService {

    private final FeedRepository feedRepository;
    private final FeedImageRepository feedImageRepository;
    private final FeedCategoryRepository feedCategoryRepository;
    private final MemberRepository memberRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final ModelMapper modelMapper;
    private final FeedCommentRepository feedCommentRepository;
    private final FeedReportRepository feedReportRepository;

    @Value("${file.tmp-dir}")
    private String tmpPath;

    @Value("${file.upload-dir}")
    private String uploadPath;

    @Transactional
    @Override
    public Long create(FeedCreateDto feedCreateDto, MultipartFile[] files) {
        // 현재 로그인한 사용자 ID 가져오기
        Long currentMemberId = SecurityUtil.getCurrentMemberId();

        // 실제 로그인한 회원 조회
        Member member = memberRepository.findById(currentMemberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        FeedCategory feedCategory = feedCategoryRepository.findById(feedCreateDto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다."));

        // 피드 저장
        Feed feed = new Feed();
        feed.setContent(feedCreateDto.getContent());
        feed.setMember(member);
        feed.setCategory(feedCategory);

        Feed savedFeed = feedRepository.save(feed);

        // 이미지 저장
        if (files.length > 0) {
            for (MultipartFile file : files) {
                String originalFilename = file.getOriginalFilename();
                String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                String savedFileName = UUID.randomUUID().toString().replace("-", "") + extension;

                File folder = new File(System.getProperty("user.dir") + "/" + uploadPath + "/" + UploadFolderType.FEED.getFolderName());

                if (!folder.exists()) {
                    if (!folder.mkdirs()) {
                        throw new IllegalArgumentException("이미지 저장 폴더 생성에 실패 하였습니다.");
                    }
                }

                String fullPath = folder.getAbsolutePath() + "/" + savedFileName;
                try {
                    file.transferTo(new File(fullPath));
                } catch (IOException e) {
                    log.error("파일 저장 실패: {}", e.getMessage());
                    throw new RuntimeException("파일 저장에 실패했습니다.");
                }

                FeedImage feedImage = FeedImage.builder()
                        .imageSaveName(savedFileName)
                        .feed(savedFeed)
                        .build();

                feedImageRepository.save(feedImage);
            }
        }

        return savedFeed.getId();
    }

    @Transactional
    @Override
    public void delete(Long id) {
        Long currentMemberId = SecurityUtil.getCurrentMemberId();
        Feed feed = feedRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("피드를 찾을 수 없습니다."));

        if(!feed.getMember().getId().equals(currentMemberId)) {
            throw new IllegalStateException("본인이 작성한 피드만 삭제할 수 있습니다.");
        }

        feed.delete();
    }

    @Transactional
    @Override
    public void update(Long id, FeedUpdateDto feedUpdateDto) {
        Long currentMemberId = SecurityUtil.getCurrentMemberId();
        Feed feed = feedRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("피드를 찾을 수 없습니다."));

        if(!feed.getMember().getId().equals(currentMemberId)) {
            throw new IllegalStateException("본인이 작성한 피드만 수정할 수 있습니다.");
        }

        FeedCategory feedCategory = feedCategoryRepository.findById(feedUpdateDto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다."));

        feed.update(feedUpdateDto.getContent(), feedCategory);
    }

    @Transactional
    @Override
    public void likeFeed(Long postId) {
        Long currentMemberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(currentMemberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        Feed feed = feedRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("피드를 찾을 수 없습니다."));

        // 좋아요 여부 확인
        boolean isLiked = feedLikeRepository.existsByFeedIdAndMemberId(postId, currentMemberId);
        if (!isLiked) {
            // 처음 좋아요
            FeedLike feedLike = FeedLike.builder()
                    .feedId(feed.getId())
                    .memberId(member.getId())
                    .build();
            feedLikeRepository.save(feedLike);
            feedRepository.incrementLikeCount(postId);

        } else {
            // 이미 존재하는 경우
            FeedLike feedLike = feedLikeRepository.findByFeedIdAndMemberId(postId, currentMemberId);
            if(!feedLike.isActive()) {
                // 비활성화 -> 활성화
                feedLike.changeIsActive();
                feedRepository.incrementLikeCount(postId);
            } else {
                // 활성화 -> 비활성화 (좋아요 취소)
                feedLike.changeIsActive();
                feedRepository.decrementLikeCount(postId);
            }
        }
    }

    /**
     * 마이 피드 조회
     * @param categoryId 카테고리 ID
     * @return 피드 목록
     */
    @Override
    public List<FeedResponseDto> getMyFeeds(Long categoryId) {
        Long currentMemberId = SecurityUtil.getCurrentMemberId();

        List<Feed> feeds;
        if(categoryId == null) {
            feeds = feedRepository.findActiveMyFeeds(currentMemberId);
        } else {
            feeds = feedRepository.findActiveMyFeedsByCategory(currentMemberId, categoryId);
        }

        return feeds.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 좋아요한 피드 조회
     * @param sort 정렬 순서 (latest: 최신순, oldest: 오래된순)
     * @param categoryId 카테고리 ID
     * @return 피드 목록
     */
    @Override
    public List<FeedResponseDto> getLikedFeeds(String sort, Long categoryId) {
        Long currentMemberId = SecurityUtil.getCurrentMemberId();
        List<Long> likeFeedIds = feedLikeRepository.findFeedIdsByMemberId(currentMemberId);
        if(likeFeedIds.isEmpty()) {
            return List.of();
        }

        List<Feed> feeds = feedRepository.findAllById(likeFeedIds);
        feeds = feeds.stream()
                .filter(feed -> feed.getStatus() == FeedStatus.ACTIVE)
                .collect(Collectors.toList());

        if(categoryId != null) {
            feeds = feeds.stream()
                    .filter(feed -> feed.getCategory().getId().equals(categoryId))
                    .collect(Collectors.toList());
        }

        if("oldest".equalsIgnoreCase(sort)) {
            feeds.sort(Comparator.comparing(Feed::getWriteDate));
        } else {
            feeds.sort(Comparator.comparing(Feed::getWriteDate).reversed());
        }

        return feeds.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public void reportFeed(Long id, FeedReportDto reportDto) {
        Long currentMemberId = SecurityUtil.getCurrentMemberId();

        // 회원 조회
        Member member = memberRepository.findById(currentMemberId)
                .orElseThrow(() -> new EntityNotFoundException("회원을 찾을 수 없습니다."));

        // 피드 조회
        Feed feed = feedRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("피드를 찾을 수 없습니다."));

        // 신고 내용 조합
        String content = reportDto.getReason();
        if(reportDto.getDetail() != null && !reportDto.getDetail().isEmpty()) {
            content += " - " + reportDto.getDetail();
        }

        // 신고 내역 생성
        FeedReport report = new FeedReport();
        report.setFeed(feed);
        report.setMember(member);
        report.setContent(content);
        report.setStatus((byte) 0); // 0: 미처리

        feedReportRepository.save(report);
        log.info("피드 신고 완료 - feedId: {}, memberId: {}, reason: {}", id, currentMemberId, reportDto.getReason());
    }

    private FeedResponseDto convertToDto(Feed feed) {
        // 현재 로그인한 회원 ID 가져오기
        Long currentMemberId = SecurityUtil.getCurrentMemberId();

        // 댓글 조회
        List<FeedComment> comments = feedCommentRepository.findAllByFeedId(feed.getId());
        List<FeedCommentDto> commentDtos = comments.stream()
                .map(comment -> FeedCommentDto.builder()
                        .commentId(comment.getId())
                        .writer(comment.getMember().getNickname())
                        .content(comment.getContent())
                        .writeDate(comment.getWriteDate())
                        .build())
                .collect(Collectors.toList());

        // 이미지 경로 배열 생성
        List<FeedImageDto> images = feedImageRepository.findAllByFeedId(feed.getId())
                .stream()
                .map(image -> FeedImageDto.builder()
                        .imageSaveName(image.getImageSaveName())
                        .imagePath(image.getImagePath())
                        .build())
                .collect(Collectors.toList());

        // 현재 회원의 좋아요 여부 확인
        boolean isLiked = feedLikeRepository.existsByFeedIdAndMemberId(
                feed.getId(),
                currentMemberId
        );

        // 프로필 이미지 경로 생성
        String profileImagePath = null;
        if(feed.getMember().getProfileImg() != null) {
            String profileImg = feed.getMember().getProfileImg();

            // 외부 URL인 경우 (소셜 계정) - 그대로 반환
            if(profileImg.startsWith("http://") || profileImg.startsWith("https://")) {
                profileImagePath = profileImg;
                // 로컬 경로인 경우
            } else if(profileImg.startsWith("/")) {
                profileImagePath = profileImg;
            } else if(profileImg.startsWith("uploads/")) {
                profileImagePath = "/" + profileImg;
            } else {
                profileImagePath = "/" + profileImg;
            }
        }

        return FeedResponseDto.builder()
                .feedId(feed.getId())
                .writer(feed.getMember().getNickname())
                .writerProfileImage(profileImagePath)
                .writeDate(feed.getWriteDate())
                .likeCount(feed.getLikeCount() != null ? feed.getLikeCount() : 0)
                .commentCount(feed.getCommentCount() != null ? feed.getCommentCount() : 0)
                .content(feed.getContent())
                .comments(commentDtos)
                .images(images)
                .isLiked(isLiked)
                .categoryId(feed.getCategory().getId())
                .categoryName(feed.getCategory().getCategory())
                .build();
    }
}
