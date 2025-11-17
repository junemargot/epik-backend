package com.everyplaceinkorea.epik_boot3_api.entity.feed;


import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "feed")
public class Feed {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "write_date", nullable = false)
  @CreationTimestamp
  private LocalDateTime writeDate;

  @Column(name = "update_date", nullable = false)
  @UpdateTimestamp
  private LocalDateTime updateDate;

  @Column(name = "like_count")
  private Integer likeCount;

  @Column(name = "comment_count")
  private Integer commentCount;

  @Column(name = "is_visible")
  private Byte isVisible;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id")
  private Member member;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  private FeedCategory category;

  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  private FeedStatus status = FeedStatus.ACTIVE;

  public void delete() {
    this.status = FeedStatus.DELETED;
  }

  public void report() {
    this.status = FeedStatus.REPORTED;
  }

  public void hide() {
    this.status = FeedStatus.HIDDEN;
  }

  public void restore() {
    this.status = FeedStatus.ACTIVE;
  }

  // 피드 수정
  public void update(String content, FeedCategory feedCategory) {
    this.content = content;
    this.category = feedCategory;
  }

  // 좋아요 수 증가
  public void likeCountUp() {
    this.likeCount++;
  }

  public void likeCountDown() {
    this.likeCount--;
  }

  public void commentCountUp() {
    this.commentCount++;
  }

  public void commentCountDown() {
    this.commentCount--;
  }
}
