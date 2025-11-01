package com.everyplaceinkorea.epik_boot3_api.admin.contents.popup.service;

import com.everyplaceinkorea.epik_boot3_api.EditorImage.UploadFolderType;
import com.everyplaceinkorea.epik_boot3_api.admin.contents.popup.dto.*;
import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import com.everyplaceinkorea.epik_boot3_api.entity.popup.*;
import com.everyplaceinkorea.epik_boot3_api.repository.Member.MemberRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.popup.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.print.attribute.standard.PagesPerMinute;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultPopupService implements PopupService {

    private final PopupRepository popupRepository;
    private final PopupCategoryRepository popupCategoryRepository;
    private final PopupRegionRepository popupRegionRepository;
    private final PopupTagRepository popupTagRepository;
    private final MemberRepository memberRepository;
    private final PopupImageRepository popupImageRepository;
    private final ModelMapper modelMapper;

    @Value("${file.tmp-dir}")
    private String tmpPath;

    @Value("${file.upload-dir}")
    private String uploadPath;

    // 팝업 등록
    @Transactional
    @Override
    public Long create(PopupRequestDto popupRequestDto, MultipartFile[] files) throws IOException {
        // 작성자 임시로 값 담아주기
        popupRequestDto.setWriter(1L);
        popupRequestDto.setType("팝업");

        // 멤버 외래키
        Member member = memberRepository.findById(popupRequestDto.getWriter()).orElseThrow();
        // 카테고리 외래키
        PopupCategory popupCategory = popupCategoryRepository.findById(popupRequestDto.getPopupCategory()).orElseThrow();
        // 지역 외래키
        PopupRegion popupRegion = popupRegionRepository.findById(popupRequestDto.getPopupRegion()).orElseThrow();

        // dto -> entity
        Popup popup = modelMapper.map(popupRequestDto, Popup.class);
        popup.setViewCount(0);
        popup.addMember(member); // entity 담아주기
        popup.addPopupCategory(popupCategory);
        popup.addPopupRegion(popupRegion);

        // 팝업 테이블 데이터 저장하기
        Popup savedPopup = popupRepository.save(popup);

        /////////////////////////////////////////////////////////////////////////////////

        // tags들을 뽑아서 tag테이블에 save
        String[] tags = popupRequestDto.getTags();
        for (String tag: tags) {
            PopupTag popupTag = PopupTag.builder()
                    .tag(tag)
                    .popup(savedPopup)
                    .build();
            popupTagRepository.save(popupTag);
        }

        //////////////////////////////////////////////////////////////////////////////////

        // files에 담긴 파일들 뽑아서 popup_image테이블에 save
        int orderIndex = 0;
        if (files.length > 0) {
            for (MultipartFile file : files) {
                String originalFilename = file.getOriginalFilename();
                String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                String savedFileName = UUID.randomUUID().toString().replace("-", "") + extension;

                File folder = new File(System.getProperty("user.dir") + "/" + uploadPath + "/" + UploadFolderType.POPUP.getFolderName());

                if (!folder.exists()) {
                    if(!folder.mkdirs()) {
                        throw new IllegalArgumentException("이미지 저장 폴더 생성에 실패 하였습니다.");
                    }
                }

                String fullPath = folder.getAbsolutePath() + File.separator + savedFileName;
                try {
                    file.transferTo(new File(fullPath));
                } catch (IOException e) {
                    log.info(e.getMessage());
                }

                PopupImageDto imageDto = PopupImageDto.builder()
                        .imgOrder(++orderIndex)
                        .imgSavedName(savedFileName)
                        .build();


                PopupImage popupImage = modelMapper.map(imageDto, PopupImage.class);
                popupImage.setPopup(savedPopup);
                popupImageRepository.save(popupImage);

                log.info("첨부파일 데이터베이스에 저장할 이름 savedFieName: {}", savedFileName);
                log.info("첨부파일 folder: {}", folder.getAbsolutePath());
                log.info("첨부파일 fullPath: {}", fullPath);
                log.info("첨부파일 imageDto: {}", imageDto);
                log.info("첨부파일 popupImage: {}", popupImage);
            }
        }

        return savedPopup.getId();
    }


    // 팝업 삭제
    @Override
    public void delete(Long id) {
        Popup popup = popupRepository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("해당하는 게시물을 찾을 수 없습니다."));

        // 팝업 이미지 삭제
        popupImageRepository.deleteAllByPopupId(id);

        // 팝업 태그 삭제
        popupTagRepository.deleteAllByPopupId(id);

        // 게시물 삭제
        popupRepository.deleteById(id);

    }

    // 팝업 비공개
    @Override
    @Transactional
    public void changeStatus(Long id) {
        Popup popup = popupRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("팝업 게시물을 찾을 수 없습니다."));

        popup.changeStatus();
        popupRepository.save(popup);
    }

//    @Override
//    public void updatePopupStatus(Long id) {
//        Popup popup = popupRepository.findById(id).orElseThrow();
////        popup.setStatus(popup.getStatus() == Status.HIDDEN ? Status.ACTIVE : Status.HIDDEN);
//        popup.changeStatus();
//        popupRepository.save(popup);
//    }

    // 팝업 상세조회
    @Transactional
    @Override
    public PopupResponseDto getPopup(Long id) {
        Popup popup = popupRepository.findById(id).orElseThrow();
        popup.setViewCount(popup.getViewCount() + 1);
        log.info("popup = {}", popup);
        //태그
        List<PopupTag> allByPopupTag = popupTagRepository.findAllByPopupId(popup.getId());
        String[] tags = new String[allByPopupTag.size()];
        for (int i = 0; i < allByPopupTag.size(); i++) {
            tags[i] = allByPopupTag.get(i).getTag();
        }

        List<PopupImage> allByPopupImage = popupImageRepository.findAllByPopupId(popup.getId());
        String[] saveImageNames = new String[allByPopupImage.size()];
        for (int i = 0; i < allByPopupImage.size(); i++) {
            saveImageNames[i] = allByPopupImage.get(i).getImgSavedName();
        }


        PopupResponseDto responseDto = modelMapper.map(popup, PopupResponseDto.class);
        responseDto.setWriter(popup.getMember().getNickname());
        responseDto.setTags(tags);
        responseDto.setSaveImageNames(saveImageNames);

        return responseDto;
    }

    // 팝업 전체 조회 및 검색 기능
    @Override
    public PopupListDto getList(int page, String keyword, String searchType) {

        int pageNumber = page - 1;
        int pageSize = 15;

        //정렬 기준 만들기
        Sort sort = Sort.by("id").descending();

        //페이징조건 만들기
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        // repository 전체조회 메서드 호출하기
        Page<Popup> popupPage = popupRepository.searchPopup(keyword, searchType, pageable);

        // 페이지네이션 정보 추가
        Long totalCount = popupPage.getTotalElements();
        int totalPages = popupPage.getTotalPages();
        boolean hasPrevPage = popupPage.hasPrevious();
        boolean hasNextPage = popupPage.hasNext();

        // 반환받은 entity -> dto 로 변환해 controller로 반환하기
        List<PopupDto> popupDtos = popupPage.getContent()
                .stream()
                .map(popup -> {
                    PopupDto popupDto = modelMapper.map(popup, PopupDto.class);
                    popupDto.setWriter(popup.getMember().getNickname());

                    return popupDto;
                }).toList();

        // 페이지 목록 생성 - 현재 페이지 계산
        int currentPage = popupPage.getNumber() + 1;
        List<Long> pages = LongStream.rangeClosed(
                Math.max(1, currentPage - 2),
                Math.min(totalPages, currentPage + 2)
        ).boxed().collect(Collectors.toList());

        // 반환
        return PopupListDto.builder()
                .popupList(popupDtos)
                .totalCount(totalCount)
                .totalPages(totalPages)
                .hasPrev(hasPrevPage)
                .hasNext(hasNextPage)
                .pages(pages)
                .build();
    }


    @Override
    public void update(Long id, PopupRequestDto popupRequestDto, MultipartFile file) {
//        Popup popup = popupRepository.findById(id).orElseThrow();
//        Member writer = memberRepository.findById(popupRequestDto.getWriter()).orElseThrow();
//        PopupCategory popupCategory = popupCategoryRepository.findById(popupRequestDto.getWriter()).orElseThrow();
//        PopupRegion popupRegion = popupRegionRepository.findById(popupRequestDto.getPopupRegion()).orElseThrow();
//
//        popup.updatePopup(popupRequestDto, writer, popupCategory, popupRegion);
//
//        List<PopupTag> popupTags = updatePopupTags(popupRequestDto.getTags());
//        popupTagRepository.saveAll(popupTags);

        // 기존 팝업 엔티티 조회
        Popup popup = popupRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("팝업게시물을 찾을 수 없습니다. ID: " + id));

        // 관련 엔티티 조회
        Member member = memberRepository.findById(popupRequestDto.getWriter())
                .orElseThrow(() -> new IllegalArgumentException("작성자를 찾을 수 없습니다."));
        PopupCategory popupCategory = popupCategoryRepository.findById(popupRequestDto.getPopupCategory())
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다."));
        PopupRegion popupRegion = popupRegionRepository.findById(popupRequestDto.getPopupRegion())
                .orElseThrow(() -> new IllegalArgumentException("지역을 찾을 수 없습니다."));

        // 팝업 엔티티 업데이트
        popup.setTitle(popupRequestDto.getTitle());
        popup.setContent(popupRequestDto.getContent());
        popup.setAddress(popupRequestDto.getAddress());
        popup.setAddressDetail(popupRequestDto.getAddressDetail());
        popup.setStartDate(popupRequestDto.getStartDate());
        popup.setEndDate(popupRequestDto.getEndDate());
        popup.setOperationTime(popupRequestDto.getOperationTime());
        popup.setSnsLink(popupRequestDto.getSnsLink());
        popup.setWebLink(popupRequestDto.getWebLink());
        popup.setType(popupRequestDto.getType());
        popup.addPopupCategory(popupCategory);
        popup.addPopupRegion(popupRegion);
        popup.addMember(member);

        // 팝업 저장
        popupRepository.save(popup);

        // 기존 태그 삭제
        popupTagRepository.deleteAllByPopupId(id);

        // 새 태그 추가
        String[] tags = popupRequestDto.getTags();
        if(tags != null && tags.length > 0) {
            for (String tag : tags) {
                PopupTag popupTag = PopupTag.builder()
                        .tag(tag)
                        .popup(popup)
                        .build();
                popupTagRepository.save(popupTag);
            }
        }

        // 이미지 처리
//        if(file != null && !file.isEmpty()) {
//            // 기존 이미지 삭제
//            popupImageRepository.deleteAllById(id);
//
//            // 새 이미지 저장
//            String originalFilename = file.getOriginalFilename();
//            String savedFileName = UUID.randomUUID().toString() + "_" + originalFilename;
//        }

    }

    private List<PopupTag> updatePopupTags(List<PopupTagDto> tagDtos) {
//        List<PopupTag> updatedTags = new ArrayList<>();
//        if (tagDtos != null) {
//            for (PopupTagDto dto : tagDtos) {
//                PopupTag tag = popupTagRepository.findById(dto.getId())
//                        .orElseThrow();
//                tag.updatePopupTag(dto);
//                updatedTags.add(tag);
//            }
//        }
//        return updatedTags;
        return null;
    }
}

