package com.everyplaceinkorea.epik_boot3_api.anonymous.feed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedImageDto {
    private String imageSaveName;
    private String imagePath;
}
