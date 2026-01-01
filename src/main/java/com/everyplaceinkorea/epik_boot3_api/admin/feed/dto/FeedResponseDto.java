package com.everyplaceinkorea.epik_boot3_api.admin.feed.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedResponseDto {
    private Long id;
    private String content;
    private String writer;
    private String writerProfileImage;
    private LocalDateTime writeDate;
    private Integer commentCount;
    private Integer likedCount;
    private String category;
    private String status;
    private List<String> images;
}
