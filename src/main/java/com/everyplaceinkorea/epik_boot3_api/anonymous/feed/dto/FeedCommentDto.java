package com.everyplaceinkorea.epik_boot3_api.anonymous.feed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedCommentDto {
  private Long commentId;
  // 댓글 작성자
  private String writer;
  // 댓글 작성일
  private LocalDateTime writeDate;
  // 댓글 내용
  private String content;
}
