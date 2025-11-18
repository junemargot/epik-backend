package com.everyplaceinkorea.epik_boot3_api.entity.feed;

import jakarta.persistence.*;
import lombok.*;


@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
public class FeedImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "image_save_name")
    private String imageSaveName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id")
    private Feed feed;

    @Builder
    public FeedImage(String imageSaveName, Feed feed) {
        this.imageSaveName = imageSaveName;
        this.feed = feed;
    }

    public String getImagePath() {
        return "/uploads/images/feed/" + this.imageSaveName;
    }
}
