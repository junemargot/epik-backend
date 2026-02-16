package com.everyplaceinkorea.epik_boot3_api.entity.inquiry;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "inquiry_image")
public class InquiryImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "image_saved_name", nullable = false, length =255)
    private String imageSavedName; // 서버에 저장된 파일명 (UUID + 확장자)

    @Column(name = "original_filename", length = 255)
    private String originalFilename; // 원본 파일명

    @Column(name = "file_size")
    private Long fileSize; // 파일 크기 (바이트)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id", nullable = false,
                foreignKey = @ForeignKey(name = "FK_INQUIRY_IMAGE_INQUIRY"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Inquiry inquiry;

    @Builder
    private InquiryImage(String imageSavedName, String originalFilename, Long fileSize, Inquiry inquiry) {
        this.imageSavedName = imageSavedName;
        this.originalFilename = originalFilename;
        this.fileSize = fileSize;
        this.inquiry = inquiry;
    }

    public static InquiryImage createImage(String imageSavedName, String originalFilename, Long fileSize, Inquiry inquiry) {
        return InquiryImage.builder()
                .imageSavedName(imageSavedName)
                .originalFilename(originalFilename)
                .fileSize(fileSize)
                .inquiry(inquiry)
                .build();
    }

    public String getImagePath() {
        return "/uploads/images/inquiry/" + this.imageSavedName;
    }

    public void assignToInquiry(Inquiry inquiry) {
        this.inquiry = inquiry;
    }
}
