package com.everyplaceinkorea.epik_boot3_api.admin.feed.service;

import com.everyplaceinkorea.epik_boot3_api.admin.feed.dto.FeedListDto;
import com.everyplaceinkorea.epik_boot3_api.admin.feed.dto.FeedResponseDto;
import com.everyplaceinkorea.epik_boot3_api.entity.feed.Feed;
import com.everyplaceinkorea.epik_boot3_api.entity.feed.FeedImage;
import com.everyplaceinkorea.epik_boot3_api.repository.feed.FeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultFeedService implements FeedService{

    private final FeedRepository feedRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FeedResponseDto> getAllFeeds() {
        List<Feed> feeds = feedRepository.findAll();

        return feeds.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public FeedListDto getAllFeedsWithPageing(int page, String keyword, String searchType) {
        int pageNumber = page - 1;
        int pageSize = 10;
        Sort sort = Sort.by("id").descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        Page<Feed> feedPage = feedRepository.searchFeed(keyword, searchType, pageable);

        long totalCount = feedPage.getTotalElements();
        int totalPages = feedPage.getTotalPages();
        boolean hasPrev = feedPage.hasPrevious();
        boolean hasNext = feedPage.hasNext();

        List<FeedResponseDto> feedDtos = feedPage.getContent()
                .stream()
                .map(this::convertToDto)
                .toList();

        int currentPage = feedPage.getNumber() + 1;
        List<Long> pages = LongStream.rangeClosed(
                Math.max(1, currentPage - 2),
                Math.min(totalPages, currentPage + 2)
        ).boxed().collect(Collectors.toList());

        return FeedListDto.builder()
                .feedList(feedDtos)
                .totalCount(totalCount)
                .totalPages(totalPages)
                .hasPrev(hasPrev)
                .hasNext(hasNext)
                .pages(pages)
                .build();
    }

    private FeedResponseDto convertToDto(Feed feed) {
        FeedResponseDto dto = new FeedResponseDto();
        dto.setId(feed.getId());
        dto.setContent(feed.getContent());
        dto.setWriter(feed.getMember().getNickname());
        if(feed.getMember().getProfileImg() != null) {
            dto.setWriterProfileImage(feed.getMember().getProfileImg());
        }
        dto.setWriteDate(feed.getWriteDate());
        dto.setCommentCount(feed.getCommentCount());
        dto.setLikedCount(feed.getLikeCount());
        dto.setCategory(feed.getCategory().getCategory());
        dto.setStatus(feed.getStatus().name());
        if(feed.getImages() != null && !feed.getImages().isEmpty()) {
            List<String> imagePaths = feed.getImages().stream()
                    .map(FeedImage::getImagePath)
                    .collect(Collectors.toList());
            dto.setImages(imagePaths);
        } else {
            dto.setImages(new ArrayList<>());
        }

        return dto;
    }

}
