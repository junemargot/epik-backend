package com.everyplaceinkorea.epik_boot3_api.repository.inquiry;

import com.everyplaceinkorea.epik_boot3_api.entity.inquiry.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    // 내 문의 목록 조회 (작성일 최신순)
    Page<Inquiry> findByWriterIdOrderByCreatedAtDesc(Long writerId, Pageable pageable);

    // 문의 상세 조회 with 이미지 (N+1 방지)
    @Query("SELECT i FROM Inquiry i " +
            "LEFT JOIN FETCH i.images " +
            "LEFT JOIN FETCH i.answeredBy " +
            "WHERE i.id = :inquiryId")
    Optional<Inquiry> findByIdWithImages(@Param("inquiryId") Long inquiryId);


}
