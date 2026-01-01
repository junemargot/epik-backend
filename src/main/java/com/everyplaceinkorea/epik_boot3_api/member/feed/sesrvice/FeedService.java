package com.everyplaceinkorea.epik_boot3_api.member.feed.sesrvice;

import com.everyplaceinkorea.epik_boot3_api.anonymous.feed.dto.FeedResponseDto;
import com.everyplaceinkorea.epik_boot3_api.member.feed.dto.FeedCreateDto;
import com.everyplaceinkorea.epik_boot3_api.member.feed.dto.FeedReportDto;
import com.everyplaceinkorea.epik_boot3_api.member.feed.dto.FeedUpdateDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FeedService {
    Long create(FeedCreateDto feedCreateDto, MultipartFile[] files);

    void delete(Long id);

    void update(Long id, FeedUpdateDto feedUpdateDto);

    void likeFeed(Long postId);

    List<FeedResponseDto> getMyFeeds(Long categoryId);

    List<FeedResponseDto> getLikedFeeds(String sort, Long categoryID);

    void reportFeed(Long id, FeedReportDto reportDto);
}
