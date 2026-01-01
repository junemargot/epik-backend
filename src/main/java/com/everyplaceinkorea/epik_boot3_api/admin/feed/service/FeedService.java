package com.everyplaceinkorea.epik_boot3_api.admin.feed.service;


import com.everyplaceinkorea.epik_boot3_api.admin.feed.dto.FeedListDto;
import com.everyplaceinkorea.epik_boot3_api.admin.feed.dto.FeedResponseDto;

import java.util.List;

public interface FeedService {

    List<FeedResponseDto> getAllFeeds();

    FeedListDto getAllFeedsWithPageing(int page, String keyword, String searchType);

}
