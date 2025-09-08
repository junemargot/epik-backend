package com.everyplaceinkorea.epik_boot3_api.admin.contents.musical.service;

import com.everyplaceinkorea.epik_boot3_api.admin.contents.musical.dto.*;
import com.everyplaceinkorea.epik_boot3_api.entity.common.DataSource;
import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import com.everyplaceinkorea.epik_boot3_api.entity.Region;
import com.everyplaceinkorea.epik_boot3_api.entity.musical.*;
import com.everyplaceinkorea.epik_boot3_api.EditorImage.UploadFolderType;
import com.everyplaceinkorea.epik_boot3_api.repository.Member.MemberRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.RegionRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.musical.MusicalImageRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.musical.MusicalRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.musical.MusicalTicketOfficeRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.musical.MusicalTicketPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.LongStream;


@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultMusicalService implements MusicalService {

    private final MusicalRepository musicalRepository;
    private final MusicalTicketOfficeRepository musicalTicketOfficeRepository;
    private final MusicalTicketPriceRepository musicalTicketPriceRepository;
    private final MemberRepository memberRepository;
    private final RegionRepository regionRepository;
    private final ModelMapper modelMapper;
    private final MusicalImageRepository musicalImageRepository;

    @Value("${file.tmp-dir}")
    private String tmpPath;

    @Value("${file.upload-dir}")
    private String uploadPath;

    // 뮤지컬 등록 기능
    @Transactional
    @Override
    public MusicalResponseDto create(MusicalCreateDto requestDto, MultipartFile file) throws IOException {
        requestDto.setWriter(1L);
        requestDto.setRegion(1L);

        String saveFileName = uploadFile(file);
        Member member = memberRepository.findById(requestDto.getWriter()).orElseThrow();
        Region region = regionRepository.findById(requestDto.getRegion()).orElseThrow();

        Musical musical = modelMapper.map(requestDto, Musical.class);

        musical.setViewCount(0);
        musical.addMember(member);
        musical.addRegion(region);
        musical.addFileSavedName(saveFileName);

        Musical savedMusical = musicalRepository.save(musical);

        List<MusicalTicketPrice> musicalTicketPrices = saveTicketPrices(requestDto.getTicketPrices(), savedMusical);
        List<MusicalTicketOffice> musicalTicketOffices = saveTicketOffices(requestDto.getTicketOffices(), savedMusical);

        List<MusicalTicketPrice> savedMusicalTicketPrices = musicalTicketPriceRepository.saveAll(musicalTicketPrices);
        List<MusicalTicketOffice> savedMusicalTicketOffices = musicalTicketOfficeRepository.saveAll(musicalTicketOffices);

        imageSave(requestDto, savedMusical);

        MusicalResponseDto responseDto = modelMapper.map(savedMusical, MusicalResponseDto.class);
        responseDto.setWriter(member.getUsername());
        responseDto.setSaveImageName(saveFileName);

        List<MusicalTicketPriceDto> ticketPriceDtos = savedMusicalTicketPrices
                .stream()
                .map(ticketPrice -> modelMapper.map(ticketPrice, MusicalTicketPriceDto.class))
                .toList();
        responseDto.setTicketPrices(ticketPriceDtos);

        List<MusicalTicketOfficeDto> ticketOfficeDtos = savedMusicalTicketOffices
                .stream()
                .map(ticketOffice -> modelMapper.map(ticketOffice, MusicalTicketOfficeDto.class))
                .toList();
        responseDto.setTicketOffices(ticketOfficeDtos);

        return responseDto;
    }

    private void imageSave(MusicalCreateDto requestDto, Musical musical) throws IOException {
        String[] fileNames = requestDto.getFileNames();
        // fullPath 경로를 만들어준다.
        for (String fileName : fileNames) {

            // ### 현재 임시폴더에 있는 이미지 의 경로생성하기 ###
            // C:\Users\yunkk\Desktop\epik-full\epik\backend\src\main\webapp\image\tmp\musical
            Path folderPath = Paths.get(System.getProperty("user.dir") + File.separator + tmpPath + File.separator + UploadFolderType.MUSICAL);
            // C:\Users\yunkk\Desktop\epik-full\epik\backend\src\main\webapp\image\tmp\musical\fe0e5a9fc11a45f4aa3b73ae2a9f0816.jpg
            String fullPath = System.getProperty("user.dir") + File.separator + tmpPath + File.separator + UploadFolderType.MUSICAL.getFolderName() + fileName;
            Path sourcePath = Paths.get(fullPath);

            log.info("fullPath = {}", fullPath);

            // ## 임시폴더에서 저장폴더로 옮기기
            Path targetpath = Paths.get(System.getProperty("user.dir") + File.separator + uploadPath + UploadFolderType.MUSICAL.getFolderName() + File.separator + fileName);

            if (Files.exists(sourcePath)) {
                log.info("파일명 : {}이 임시폴더에 존재합니다.", fileName);
                // 존재한다면? webapp/image/uplods/musical 로 옮기기
                // 일단 폴더가 존재하는지 확인하고
                if (!Files.exists(folderPath)) {
                    Files.createDirectories(folderPath); // 상위 폴더까지 모두 생성
                }
                // 이동할 파일의 현재 경로(sourceDir), 이동 후의 파일 경로 설정(targetDir)
                Files.move(sourcePath, targetpath);
            }

            MusicalImage musicalImage = MusicalImage.builder()
                    .fileSaveName(fileName)
                    .musical(musical)
                    .build();

            musicalImageRepository.save(musicalImage);
        }
    }

    private List<MusicalTicketPrice> saveTicketPrices(List<MusicalTicketPriceDto> ticketPrices, Musical musical) {
        List<MusicalTicketPrice> ticketPriceEntities = new ArrayList<>();
        for (MusicalTicketPriceDto ticketPriceDto : ticketPrices) {
            MusicalTicketPrice ticketPrice = modelMapper.map(ticketPriceDto, MusicalTicketPrice.class);
            ticketPrice.setMusical(musical);

            ticketPriceEntities.add(ticketPrice);
        }
        return ticketPriceEntities;
    }

    // 위의 메소드와 동일한 기능
    private List<MusicalTicketOffice> saveTicketOffices(List<MusicalTicketOfficeDto> ticketOffices, Musical musical) {
        List<MusicalTicketOffice> ticketOfficesEntities = new ArrayList<>();
        for (MusicalTicketOfficeDto ticketOfficeDto : ticketOffices) {
            MusicalTicketOffice ticketOffice = modelMapper.map(ticketOfficeDto, MusicalTicketOffice.class);
            ticketOffice.setMusical(musical);
            ticketOfficesEntities.add(ticketOffice);
        }
        return ticketOfficesEntities;
    }

    private String uploadFile(MultipartFile file) throws IOException {
        if (file != null && !file.isEmpty()) {
            String originalFilename = file.getOriginalFilename(); // 실제 파일명
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String savedFileName = UUID.randomUUID().toString().replace("-", "") + extension; // uuid.확장자

            File folder = new File(System.getProperty("user.dir") + File.separator + uploadPath + File.separator + UploadFolderType.MUSICAL.getFolderName());
            if (!folder.exists()) {
                if(!folder.mkdirs()) {
                    throw new IllegalArgumentException("이미지 저장 폴더 생성에 실패 하였습니다.");
                }
            }

            String fullPath = folder.getAbsolutePath() + File.separator + savedFileName;

            try {
                file.transferTo(new File(fullPath));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return savedFileName;
        } else {
            return null; // 추후 빈파일이 넘어왔을 경우 예외 처리 필요
        }
    }

    // 뮤지컬 상세조회
    @Override
    @Transactional
    public MusicalResponseDto getMusical(Long id) {
        Musical musical = musicalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당하는 게시물을 찾을 수 없습니다."));

        musical.setViewCount(musical.getViewCount() + 1);
        musicalRepository.save(musical); // 변경사항 저장

        // 티켓 
        List<MusicalTicketPrice> allByMusicalTicketPrice = musicalTicketPriceRepository.findAllByMusicalId(musical.getId());
        // 예매처 
        List<MusicalTicketOffice> allByMusicalTicketOffice = musicalTicketOfficeRepository.findAllByMusicalId(musical.getId());

        MusicalResponseDto responseDto = modelMapper.map(musical, MusicalResponseDto.class);
        responseDto.setWriter(musical.getMember().getNickname());
//        responseDto.setSaveImageName(musical.getFileSavedName());
        responseDto.setDataSource(musical.getDataSource());

        // 이미지 처리 로직 개선
        if (musical.getDataSource() == DataSource.KOPIS_API) {
            // KOPIS 데이터인 경우
            responseDto.setImageUrl(musical.getKopisPoster()); // KOPIS 원본 포스터 URL
            responseDto.setSaveImageName(musical.getFileSavedName()); // 파일명도 설정
        } else {
            // 수기 입력 데이터인 경우
            responseDto.setSaveImageName(musical.getFileSavedName());
            // imageUrl은 프론트엔드에서 동적으로 생성하거나 getImageUrl() 메소드 사용
        }

        List<MusicalTicketPriceDto> ticketPriceDtos = allByMusicalTicketPrice
                .stream()
                .map(ticketPrice -> modelMapper.map(ticketPrice, MusicalTicketPriceDto.class))
                .toList();
        responseDto.setTicketPrices(ticketPriceDtos);

        List<MusicalTicketOfficeDto> ticketOfficeDtos = allByMusicalTicketOffice
                .stream()
                .map(ticketOffice -> modelMapper.map(ticketOffice, MusicalTicketOfficeDto.class))
                .toList();
        responseDto.setTicketOffices(ticketOfficeDtos);
        log.info("응답데이터 save파일 네임 = {}", responseDto.getSaveImageName());
        log.info("응답데이터 imageUrl = {}", responseDto.getImageUrl());

        return responseDto;
    }


    // 뮤지컬 전체 조회 및 검색 기능
    @Override
    public MusicalListDto getList(int page, String keyword, String searchType) {
        // 정렬 기준 만들기
        Sort sort = Sort.by("id").descending();
        // 페이징조건 만들기
        Pageable pageable = PageRequest.of(page - 1, 15, sort);
        // repository 전체조회 메서드 호출하기
        Page<Musical> musicalPage = musicalRepository.searchMusical(keyword, searchType, pageable);

        // 페이징 결과
        long totalCount = musicalPage.getTotalElements();
        int totalPages = musicalPage.getTotalPages();
        boolean hasPrevPage = musicalPage.hasPrevious();
        boolean hasNextPage = musicalPage.hasNext();

        // 반환받은 entity -> dto 로 변환해 controller로 반환하기
        List<MusicalDto> musicalDtos = musicalPage
                .getContent()
                .stream()
                .map(musical -> {
                    MusicalDto musicalDto = modelMapper.map(musical, MusicalDto.class);
                    musicalDto.setWriter(musical.getMember().getNickname());
                    return musicalDto;
                }).toList();

        // 페이지 목록 생성 - 현재 페이지를 기준으로 앞뒤 2개의 페이지를 표시하도록 범위 설정
        int currentPage = musicalPage.getNumber() + 1;
        List<Long> pages = LongStream.rangeClosed(
                Math.max(1, currentPage - 2),
                Math.min(totalPages, currentPage + 2)
        ).boxed().collect(Collectors.toList());

        return MusicalListDto.builder()
                .musicalList(musicalDtos)
                .totalCount(totalCount)
                .totalPages(totalPages)
                .hasPrev(hasPrevPage)
                .hasNext(hasNextPage)
                .pages(pages)
                .build();
    }

//    @Override
//    public MusicalListDto getFilteredList(Integer page, String keyword, String searchType, String status) throws IOException {
//
//        // 정렬 기준 만들기
//        Sort sort = Sort.by("id").descending();
//
//        // 페이징 조건 만들기
//        Pageable pageable = PageRequest.of(page - 1, 15, sort);
//
//        // Status 열거형으로 변환
//        Status statusEnum = Status.valueOf(status);
//
//        // repository 상태 필터링 메서드 호출
//        Page<Musical> musicalPage = musicalRepository.searchMusicalWithStatus(keyword, searchType, statusEnum, pageable);
//
//        // 페이징 결과
//        List<MusicalDto> musicalDtos = musicalPage
//                .getContent()
//                .stream()
//                .map(musical -> {
//                    MusicalDto musicalDto = modelMapper.map(musical, MusicalDto.class);
//                    musicalDto.setWriter(musical.getMember().getNickname());
//                    musicalDto.setStatus(musical.getStatus().name());
//
//                    return musicalDto;
//                })
//                .collect(Collectors.toList()); // Stream을 List로 반환
//
//        // 페이징 결과
//        long totalCount = musicalPage.getTotalElements();
//        int totalPages = musicalPage.getTotalPages();
//        boolean hasPrevPage = musicalPage.hasPrevious();
//        boolean hasNextPage = musicalPage.hasNext();
//
//        // 페이지 목록 생성
//        int currentPage = musicalPage.getNumber() + 1;
//        List<Long> pages = LongStream.rangeClosed(
//                Math.max(1, currentPage - 2),
//                Math.min(totalPages, currentPage + 2)
//        ).boxed().collect(Collectors.toList());
//
//        return MusicalListDto.builder()
//                .musicalList(musicalDtos)
//                .totalCount(totalCount)
//                .totalPages(totalPages)
//                .hasPrev(hasPrevPage)
//                .hasNext(hasNextPage)
//                .pages(pages)
//                .build();
//    }


    // 뮤지컬 수정
    @Transactional
    @Override
    public void update(Long id, MusicalUpdateDto musicalUpdateDto, MultipartFile file) {
//        Musical musical = musicalRepository.findById(id).orElseThrow();
//        Member writer = memberRepository.findById(musicalUpdateDto.getWriter()).orElseThrow();
//        Region region = regionRepository.findById(musicalUpdateDto.getRegion()).orElseThrow();
//
//        // 뮤지컬 entity에 파일 경로를 찾아서 그걸 삭제해줘야하네 일단
//        File savedFile = new File(musical.getFilePath());
//        if (savedFile.exists()) {
//            savedFile.delete();
//        }
//
//        // 새로운 파일을 저장
//        UploadResult uploadResult = null;
//        try {
//            uploadResult = uploadFile(file);
//            musical.changeImage(uploadResult);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        musical.updateMusical(musicalUpdateDto, writer, region);
//
//        List<MusicalTicketOffice> musicalTicketOffices = updateTicketOffices(musicalUpdateDto.getTicketOffices());
//        List<MusicalTicketPrice> musicalTicketPrices = updateTicketPrices(musicalUpdateDto.getTicketPrices());
//        musicalTicketOfficeRepository.saveAll(musicalTicketOffices);
//        musicalTicketPriceRepository.saveAll(musicalTicketPrices);

    }

    private List<MusicalTicketOffice> updateTicketOffices(List<MusicalTicketOfficeDto> officeDtos) {
        List<MusicalTicketOffice> updatedOffices = new ArrayList<>();
        if (officeDtos != null) {
            for (MusicalTicketOfficeDto dto : officeDtos) {
                MusicalTicketOffice office = musicalTicketOfficeRepository.findById(dto.getId())
                        .orElseThrow();
                office.updateMusicalTicketOffice(dto);
                updatedOffices.add(office);
            }
        }
        return updatedOffices;
    }

    private List<MusicalTicketPrice> updateTicketPrices(List<MusicalTicketPriceDto> priceDtos) {
        List<MusicalTicketPrice> updatedPrices = new ArrayList<>();
        if (priceDtos != null) {
            for (MusicalTicketPriceDto dto : priceDtos) {
                MusicalTicketPrice price = musicalTicketPriceRepository.findById(dto.getId())
                        .orElseThrow();
                price.updateMusicalTicketPrice(dto);
                updatedPrices.add(price);
            }
        }
        return updatedPrices;
    }

    // 뮤지컬 소프트 삭제
//    @Transactional
//    @Override
//    public void delete(Long id) {
//        Musical musical = musicalRepository.findById(id).orElseThrow();
//        musical.delete();
//    }

    @Override
    public void delete(Long id) {
        Musical musical = musicalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당하는 게시물을 찾을 수 없습니다."));

        // 뮤지컬 이미지 삭제
        musicalImageRepository.deleteAllByMusicalId(id);

        // 뮤지컬 티켓 오피스 삭제
        musicalTicketOfficeRepository.deleteByMusicalId(id);

        // 뮤지컬 티켓 가격 삭제
        musicalTicketPriceRepository.deleteByMusicalId(id);

        // 뮤지컬 게시물 삭제
        musicalRepository.deleteById(id);
    }

    // 뮤지컬 비공개/공개 상태 변경
    @Transactional
    @Override
    public void updateMusicalStatus(Long id) {
        Musical musical = musicalRepository.findById(id).orElseThrow();
        musical.changeStatus();
    }

}