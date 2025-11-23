package com.everyplaceinkorea.epik_boot3_api.entity.comment;

import com.everyplaceinkorea.epik_boot3_api.entity.feed.Feed;
import com.everyplaceinkorea.epik_boot3_api.entity.feed.FeedStatus;
import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import com.everyplaceinkorea.epik_boot3_api.member.comment.dto.CommentUpdateDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class FeedComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;

    @Column(name = "write_date")
    @CreationTimestamp
    private LocalDateTime writeDate;

    @Column(name = "update_date", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updateDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id")
    private Feed feed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    private FeedStatus status = FeedStatus.ACTIVE;

    @Builder
    public FeedComment(String content, Feed feed, Member member) {
        this.content = content;
        this.feed = feed;
        this.member = member;
    }

    public void update(CommentUpdateDto commentUpdateDto) {
        this.content = commentUpdateDto.getContent();
    }

    public void delete() {
        this.status = FeedStatus.DELETED;
    }

    public void report() {
        this.status = FeedStatus.REPORTED;
    }

    public void hide() {
        this.status = FeedStatus.HIDDEN;
    }
}
