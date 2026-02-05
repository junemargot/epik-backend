package com.everyplaceinkorea.epik_boot3_api.entity.inquiry;

import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "inquiry")
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member writer;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private InquiryCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InquiryStatus status = InquiryStatus.PENDING;

    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;

    @Column(name = "receive_email_answer", nullable = false)
    private boolean receiveEmailAnswer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answered_by")
    private Member answeredBy; // 답변한 관리자

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @LastModifiedDate
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Builder
    private Inquiry(Member writer, String title, String content, InquiryCategory category, boolean receiveEmailAnswer) {
        this.writer = writer;
        this.title = title;
        this.content = content;
        this.category = category;
        this.receiveEmailAnswer = receiveEmailAnswer;
    }

    public static Inquiry createInquiry(Member writer, String title, String content, InquiryCategory category, boolean receiveEmailAnswer) {
        return Inquiry.builder()
                .writer(writer)
                .title(title)
                .content(content)
                .category(category)
                .receiveEmailAnswer(receiveEmailAnswer)
                .build();
    }

    // 비즈니스 메서드 1: 답변 등록
    public void addAnswer(String answer, Member admin) {
        if(this.status == InquiryStatus.ANSWERED) {
            throw new IllegalStateException("이미 답변이 완료된 문의입니다.");
        }

        this.answer = answer;
        this.answeredAt = LocalDateTime.now();
        this.answeredBy = admin;
        this.status = InquiryStatus.ANSWERED;
    }

    // 비즈니스 메서드 2: 내용 수정
    public void updateContent(String title, String content) {
        if(!canModify()) {
            throw new IllegalStateException("답변이 완료된 문의는 수정할 수 없습니다.");
        }

        this.title = title;
        this.content = content;
    }

    // 비즈니스 메서드 3: 상태 체크 (읽기 전용)
    public boolean isAnswered() {
        return this.status == InquiryStatus.ANSWERED;
    }

    public boolean canModify() {
        return this.status == InquiryStatus.PENDING;
    }
}
