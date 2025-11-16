package com.everyplaceinkorea.epik_boot3_api.anonymous.feed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedResponseDto {
  // 피드 ID
  private Long feedId;
  // 작성자
  private String writer;
  // 등록일
  private LocalDateTime writeDate;
  // 좋아요 수
  private Integer likeCount;
  // 댓글 수
  private Integer commentCount;
  // 내용
  private String content;
  // 댓글
  private List<FeedCommentDto> comments;
  // 이미지
  private String[] imageSaveName;
  // 좋아요 여부
  private Boolean isLiked;
  // 카테고리 정보
  private Long categoryId;
  private String categoryName;
}

