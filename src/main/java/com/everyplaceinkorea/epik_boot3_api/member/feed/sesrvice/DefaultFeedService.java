package com.everyplaceinkorea.epik_boot3_api.member.feed.sesrvice;

import com.everyplaceinkorea.epik_boot3_api.EditorImage.UploadFolderType;
import com.everyplaceinkorea.epik_boot3_api.entity.feed.*;
import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import com.everyplaceinkorea.epik_boot3_api.member.feed.dto.FeedCreateDto;
import com.everyplaceinkorea.epik_boot3_api.member.feed.dto.FeedUpdateDto;
import com.everyplaceinkorea.epik_boot3_api.repository.Member.MemberRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.feed.FeedCategoryRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.feed.FeedImageRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.feed.FeedLikeRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.feed.FeedRepository;
import com.everyplaceinkorea.epik_boot3_api.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

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
        Feed feed = modelMapper.map(feedCreateDto, Feed.class);
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

        // 실제 로그인한 회원으로 좋아요 확인
        boolean isLiked = feedLikeRepository.existsByFeedIdAndMemberId(postId, currentMemberId);
        if (!isLiked) {
            FeedLike feedLike = FeedLike.builder()
                    .feedId(feed.getId())
                    .memberId(member.getId())
                    .build();
            feedLikeRepository.save(feedLike);
            feed.likeCountUp();
        } else {
            FeedLike feedLike = feedLikeRepository.findByFeedIdAndMemberId(postId, 1L);
            feedLike.changeIsActive();
            feed.likeCountUp();
        }
    }

    @Transactional
    @Override
    public void unLikeFeed(Long postId) {
        Long currentMemberId = SecurityUtil.getCurrentMemberId();
        Feed feed = feedRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("피드를 찾을 수 없습니다."));

        FeedLike feedLike = feedLikeRepository.findByFeedIdAndMemberId(postId, currentMemberId);
        feedLike.changeIsActive();
        feed.likeCountDown();
    }
}
