package com.everyplaceinkorea.epik_boot3_api.admin.feed.controller;

import com.everyplaceinkorea.epik_boot3_api.admin.feed.dto.FeedListDto;
import com.everyplaceinkorea.epik_boot3_api.admin.feed.dto.FeedResponseDto;
import com.everyplaceinkorea.epik_boot3_api.admin.feed.service.FeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

//    @GetMapping
//    public ResponseEntity<List<FeedResponseDto>> getAllFeeds() {
//        log.info("관리자 - 피드 목록 조회");
//        List<FeedResponseDto> feeds = feedService.getAllFeeds();
//        return ResponseEntity.ok(feeds);
//    }

    @GetMapping
    public ResponseEntity<FeedListDto> getAllFeedsWithPaging(
            @RequestParam(name = "p", defaultValue = "1") Integer page,
            @RequestParam(name = "k", required = false) String keyword,
            @RequestParam(name = "s", required = false) String searchType) {

        log.info("관리자 - 피드 목록 조회(페이징) - page: {}, keyword: {}, searchType: {}",
                page, keyword, searchType);

        FeedListDto feeds = feedService.getAllFeedsWithPageing(page, keyword, searchType);

        return ResponseEntity.ok(feeds);
    }
}
