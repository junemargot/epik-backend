package com.everyplaceinkorea.epik_boot3_api.member.inquiry.service;

import com.everyplaceinkorea.epik_boot3_api.entity.inquiry.Inquiry;
import com.everyplaceinkorea.epik_boot3_api.entity.inquiry.InquiryImage;
import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import com.everyplaceinkorea.epik_boot3_api.member.inquiry.dto.InquiryCreateRequestDto;
import com.everyplaceinkorea.epik_boot3_api.member.inquiry.dto.InquiryDetailResponseDto;
import com.everyplaceinkorea.epik_boot3_api.member.inquiry.dto.InquiryListResponseDto;
import com.everyplaceinkorea.epik_boot3_api.member.inquiry.dto.InquiryUpdateRequestDto;
import com.everyplaceinkorea.epik_boot3_api.repository.Member.MemberRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.inquiry.InquiryImageRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.inquiry.InquiryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquireServiceImpl implements InquiryService {

    private final InquiryRepository inquiryRepository;
    private final InquiryImageRepository inquiryImageRepository;
    private final MemberRepository memberRepository;

    private static final String UPLOAD_DIR = "uploads/images/inquiry/";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif", "webp");

    @Override
    @Transactional
    public Long createInquiry(InquiryCreateRequestDto request, List<MultipartFile> images, Long memberId) {
        // 1. 회원 조회
        Member writer = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 2. 문의 엔티티 생성
        Inquiry inquiry = Inquiry.createInquiry(
                writer,
                request.getTitle(),
                request.getContent(),
                request.getCategory(),
                request.isReceiveEmailAnswer()
        );

        // 3. 이미지 처리
        if(images != null && !images.isEmpty()) {
            validateImages(images);
            for(MultipartFile image : images) {
                String savedFileName = saveImage(image);

                InquiryImage inquiryImage = InquiryImage.createImage(
                        savedFileName,
                        image.getOriginalFilename(),
                        image.getSize(),
                        inquiry
                );

                inquiry.addImage(inquiryImage);
            }
        }

        // 4. 저장
        Inquiry savedInquiry = inquiryRepository.save(inquiry);
        log.info("문의 등록 완료 - ID: {}, 작성자: {}", savedInquiry.getId(), writer.getEmail());

        return savedInquiry.getId();
    }

    @Override
    public Page<InquiryListResponseDto> getMyInquiries(Long memberId, Pageable pageable) {
        Page<Inquiry> inquiryPage = inquiryRepository.findByWriterIdOrderByCreatedAtDesc(memberId, pageable);
        return inquiryPage.map(InquiryListResponseDto::from);
    }


    @Override
    public InquiryDetailResponseDto getInquiryDetail(Long inquiryId, Long memberId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문의입니다."));

        // 본인 문의 확인
        if(!inquiry.getWriter().getId().equals(memberId)) {
            throw new IllegalArgumentException("본인 문의만 조회할 수 있습니다.");
        }

        return InquiryDetailResponseDto.from(inquiry);
    }

    @Override
    @Transactional
    public void updateInquiry(Long inquiryId, InquiryUpdateRequestDto request, List<MultipartFile> newImages, Long memberId) {

        // 1. 문의 조회 및 권한 확인
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문의입니다."));

        if (!inquiry.getWriter().getId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 문의만 수정할 수 있습니다.");
        }

        // 2. 내용 수정 (답변 완료 시 예외 발생)
        inquiry.updateContent(request.getTitle(), request.getContent());

        // 3. 이미지 처리 (선택사항)
        if (newImages != null && !newImages.isEmpty()) {
            // 기존 이미지 삭제
            deleteExistingImages(inquiry);

            // 새 이미지 업로드
            validateImages(newImages);
            for (MultipartFile image : newImages) {
                String savedFileName = saveImage(image);

                InquiryImage inquiryImage = InquiryImage.createImage(
                        savedFileName,
                        image.getOriginalFilename(),
                        image.getSize(),
                        inquiry
                );

                inquiry.addImage(inquiryImage);
            }
        }

        log.info("문의 수정 완료 - ID: {}", inquiryId);
    }

    @Override
    @Transactional
    public void deleteInquiry(Long inquiryId, Long memberId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문의입니다."));

        if (!inquiry.getWriter().getId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 문의만 삭제할 수 있습니다.");
        }

        // 이미지 파일 삭제
        deleteExistingImages(inquiry);

        inquiryRepository.delete(inquiry);

        log.info("문의 삭제 완료 - ID: {}", inquiryId);
    }

    private void validateImages(List<MultipartFile> images) {
        if(images.size() > 8) {
            throw new IllegalStateException("이미지는 최대 8개까지 업로드 가능합니다.");
        }

        for(MultipartFile image : images) {
            if(image.getSize() > MAX_FILE_SIZE) {
                throw new IllegalStateException("이미지 파일 크기는 5MB를 초과할 수 없습니다.");
            }

            // 확장자 검증
            String originalFilename = image.getOriginalFilename();
            if(originalFilename == null || originalFilename.isEmpty()) {
                throw new IllegalStateException("파일명이 유효하지 않습니다.");
            }

            String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if(!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new IllegalStateException("허용되지 않는 이미지 형식입니다. (jpg, jpeg, png, gif, webp만 가능)");
            }
        }
    }

    private String saveImage(MultipartFile image) {
        try {
            // 업로드 디렉토리 생성
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 고유한 파일명 생성 (UUID + 확장자)
            String originalFilename = image.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String savedFileName = UUID.randomUUID().toString() + extension;

            // 파일 저장
            Path filePath = Paths.get(UPLOAD_DIR + savedFileName);
            Files.write(filePath, image.getBytes());

            log.info("이미지 저장 완료 - 파일명: {}", savedFileName);

            return savedFileName;

        } catch (IOException e) {
            log.error("이미지 저장 실패", e);
            throw new RuntimeException("이미지 저장에 실패했습니다.", e);
        }
    }

    private void deleteExistingImages(Inquiry inquiry) {
        List<InquiryImage> images = inquiry.getImages();

        for (InquiryImage image : images) {
            try {
                Path filePath = Paths.get(UPLOAD_DIR + image.getImageSavedName());
                Files.deleteIfExists(filePath);
                log.info("이미지 파일 삭제 완료 - {}", image.getImageSavedName());
            } catch (IOException e) {
                log.error("이미지 파일 삭제 실패 - {}", image.getImageSavedName(), e);
            }
        }

        inquiry.clearImages();
    }
}
