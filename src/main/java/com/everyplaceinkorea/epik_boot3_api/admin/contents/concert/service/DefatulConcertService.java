package com.everyplaceinkorea.epik_boot3_api.admin.contents.concert.service;

import com.everyplaceinkorea.epik_boot3_api.admin.contents.concert.dto.*;
import com.everyplaceinkorea.epik_boot3_api.entity.Region;
import com.everyplaceinkorea.epik_boot3_api.entity.common.DataSource;
import com.everyplaceinkorea.epik_boot3_api.entity.concert.*;
import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import com.everyplaceinkorea.epik_boot3_api.EditorImage.UploadFolderType;
import com.everyplaceinkorea.epik_boot3_api.repository.Member.MemberRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.RegionRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.concert.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefatulConcertService implements ConcertService {

  private final ConcertRepository concertRepository;
  private final ConcertTicketOfficeRepository concertTicketOfficeRepository;
  private final ConcertTicketPriceRepository concertTicketPriceRepository;
  private final MemberRepository memberRepository;
  private final RegionRepository regionRepository;
  private final ModelMapper modelMapper;
  private final ConcertImageRepository concertImageRepository;
  private final ConcertBookmarkRepository concertBookmarkRepository;

  @Value("${file.tmp-dir}")
  private String tmpPath;

  @Value("${file.upload-dir}")
  private String uploadPath;

  @Override
  public ConcertListDto getList(int page, String keyword, String searchType) {
    // 기본 페이지 번호 설정 - 클라이언트는 1-based 페이지 인덱스를 사용하므로 Spring JPA의 0-based 인덱스에 맞게 변환
    int pageNumber = page - 1;

    // 한 페이지에 표시할 데이터 개수
    int pageSize = 15;

    // 게시물 ID 기준 내림차순 정렬(최신 게시물이 먼저 나오도록 설정)
    Sort sort = Sort.by("id").descending();

    // 페이지 요청 객체 생성
    Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

    // 검색 조건에 맞는 데이터 가져오기
    Page<Concert> concertPage = concertRepository.searchConcert(keyword, searchType, pageable);

    long totalCount = concertPage.getTotalElements(); // 총 게시물 수
    int totalPages = concertPage.getTotalPages(); // 총 페이지 수
    boolean hasPrevPage = concertPage.hasPrevious();
    boolean hasNextPage = concertPage.hasNext();

    // Concert Entity -> ConcertDto 매핑
    List<ConcertDto> concertDtos = concertPage.getContent() // 현재 페이지의 데이터 리스트
            .stream()
            .map(concert -> {
              ConcertDto concertDto = modelMapper.map(concert, ConcertDto.class);
              concertDto.setWriter(concert.getMember().getNickname());

              return concertDto;
            }).toList();

    // 페이지 목록 생성 - 현재 페이지를 기준으로 앞뒤 2개의 페이지를 표시하도록 범위 설정
    int currentPage = concertPage.getNumber() + 1;
    List<Long> pages = LongStream.rangeClosed(
            Math.max(1, currentPage - 2),
            Math.min(totalPages, currentPage + 2)
    ).boxed().collect(Collectors.toList());

    // 응답 반환
    return ConcertListDto.builder()
            .concertList(concertDtos)
            .totalCount(totalCount)
            .totalPages(totalPages)
            .hasPrev(hasPrevPage)
            .hasNext(hasNextPage)
            .pages(pages)
            .build();

  }

  @Transactional
  @Override
  public ConcertResponseDto getById(Long id) {
    Concert concert = concertRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("CONCERT NOT FOUND WITH ID: " + id));

    concert.setViewCount(concert.getViewCount() + 1);

    List<ConcertTicketOffice> concertTicket = concertTicketOfficeRepository.findAllByConcertId(id);
    List<ConcertTicketPrice> concertPrice = concertTicketPriceRepository.findAllByConcertId(id);

    // concertTicket entity -> dto
    // entity를 하나씩 꺼내서 dto로 만든걸
    // concertPrice entity -> dto

    ConcertResponseDto concertResponseDto = modelMapper.map(concert, ConcertResponseDto.class);
    concertResponseDto.setWriter(concert.getMember().getNickname()); // 닉네임 설정
    concertResponseDto.setDataSource(concert.getDataSource()); // 데이터 소스 설정

    // 이미지 처리 로직 개선
//    if(concert.getDataSource() == DataSource.KOPIS_API) {
//      concertResponseDto.setImageUrl(concert.getKopisPoster()); // KOPIS 원본 포스터 URL
//      concertResponseDto.setSaveImageName(concert.getFileSavedName()); // 파일명 설정
//
//      // 상세 이미지 처리 추가
//      if (concert.getDetailImages() != null && !concert.getDetailImages().trim().isEmpty()) {
//        String[] imageUrls = concert.getDetailImages().split(",");
//        List<String> imageList = Arrays.asList(imageUrls);
//        concertResponseDto.setConcertImages(imageList);
//        System.out.println("상세 이미지 설정 완료: " + imageList.size() + "개");
//      } else {
//        System.out.println("상세 이미지 없음");
//      }
//    } else {
//      concertResponseDto.setSaveImageName(concert.getFileSavedName());
//    }

    // 티켓 오피스 정보 설정
//    List<ConcertTicketOfficeDto> concertTicketOfficeDtos = new ArrayList<>();
//    for(ConcertTicketOffice ticketOffice : concertTicket) {
//      ConcertTicketOfficeDto ticketOfficeDto = modelMapper.map(ticketOffice, ConcertTicketOfficeDto.class);
//
//      concertTicketOfficeDtos.add(ticketOfficeDto);
//    }
//    concertResponseDto.setTicketOffices(concertTicketOfficeDtos); // loop 밖에서 한 번만 설정
//
//
//    // 티켓 가격 정보 설정
//    List<ConcertTicketPriceDto> concertTicketPriceDtos = new ArrayList<>();
//    for(ConcertTicketPrice ticketPrice : concertPrice) {
//      ConcertTicketPriceDto ticketPriceDto = modelMapper.map(ticketPrice, ConcertTicketPriceDto.class);
//
//      concertTicketPriceDtos.add(ticketPriceDto);
//    }
//    concertResponseDto.setTicketPrices(concertTicketPriceDtos); // loop 밖에서 한 번만 설정

    // 이미지 처리 로직
    handleConcertImages(concert, concertResponseDto);

    // 티켓 정보 통합 처리 (새로 추가)
    handleTicketInformation(concert, concertResponseDto);

    log.info("응답데이터 save파일 네임 = {}", concertResponseDto.getSaveImageName());
    log.info("응답데이터 imageUrl = {}", concertResponseDto.getImageUrl());

    return concertResponseDto;

  }

  // 콘서트 컨텐츠 등록 - Concert Entity에 선언된 필드들에 값을 넣어 save
  @Transactional
  @Override
  public ConcertResponseDto create(ConcertRequestDto concertRequestDto, MultipartFile files) throws IOException {

    concertRequestDto.setWriter(1L);
    concertRequestDto.setRegion(1L);

    ConcertUploadResultDto uploadResult = uploadFile(files);
    if(uploadResult == null || uploadResult.getFilePath() == null) {
      throw new IllegalArgumentException("FILE UPLOAD FAILED");
    }

    Member member = memberRepository.findById(concertRequestDto.getWriter()).orElseThrow();
    Region region = regionRepository.findById(concertRequestDto.getRegion()).orElseThrow();

    Concert concert = modelMapper.map(concertRequestDto, Concert.class);

    concert.setViewCount(0);
    concert.setMember(member);
    concert.setRegion(region);
    concert.addImage(uploadResult);


    Concert savedConcert = concertRepository.save(concert);

    List<ConcertTicketOffice> concertTicketOffices = saveTicketOffices(concertRequestDto.getConcertTicketOffices(), savedConcert);
    List<ConcertTicketPrice> concertTicketPrices = saveTicketPrices(concertRequestDto.getConcertTicketPrices(), savedConcert);

    concertTicketPriceRepository.saveAll(concertTicketPrices);
    concertTicketOfficeRepository.saveAll(concertTicketOffices);

    imageSave(concertRequestDto, savedConcert);

    ConcertResponseDto responseDto = modelMapper.map(savedConcert, ConcertResponseDto.class);
    responseDto.setWriter(member.getNickname());
    responseDto.setSaveImageName(uploadResult.getFileSavedName());

    responseDto.setTicketOffices(concertTicketOffices.stream()
            .map(office -> modelMapper.map(office, ConcertTicketOfficeDto.class)).collect(Collectors.toList()));

    responseDto.setTicketPrices(concertTicketPrices.stream()
            .map(price -> modelMapper.map(price, ConcertTicketPriceDto.class)).collect(Collectors.toList()));

    log.info("responseDto = {} ", responseDto.toString());
    return responseDto;
  }

  private void imageSave(ConcertRequestDto concertRequestDto, Concert concert) throws IOException {
    String[] fileNames = concertRequestDto.getFileNames();

    for (String fileName : fileNames) {
      Path folderPath = Paths.get(System.getProperty("user.dir") + File.separator + tmpPath + File.separator + UploadFolderType.CONCERT);
      String fullPath = System.getProperty("user.dir") + File.separator + tmpPath + File.separator + UploadFolderType.CONCERT.getFolderName() + fileName;
      Path sourcePath = Paths.get(fullPath);

      log.info("fullPath = {}", fullPath);

      Path targetpath = Paths.get(System.getProperty("user.dir") + File.separator + uploadPath + UploadFolderType.CONCERT.getFolderName() + File.separator + fileName);
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

      ConcertImage concertImage = ConcertImage.builder()
              .fileSaveName(fileName)
              .filePath(fullPath)
              .concert(concert)
              .build();

      concertImageRepository.save(concertImage);
    }

  }

  @Override
  public ConcertResponseDto update(Long id, ConcertRequestDto concertRequestDto, MultipartFile file) {
    Concert concert = concertRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("CONCERT NOT FOUND"));

    concert.setTitle(concertRequestDto.getTitle());
    concert.setContent(concertRequestDto.getContent());
    concert = concertRepository.save(concert);

    ConcertResponseDto responseDto = modelMapper.map(concert, ConcertResponseDto.class);
    responseDto.setWriter(concert.getMember().getNickname());

    return responseDto;
  }

  @Override
  public void delete(Long id) {
    Concert concert = concertRepository.findById(id).orElseThrow(() ->
            new IllegalArgumentException("해당하는 게시물을 찾을 수 없습니다."));

    // 콘서트 이미지 삭제
    concertImageRepository.deleteAllByConcertId(id);

    // 콘서트 티켓 오피스 삭제
    concertTicketOfficeRepository.deleteAllByConcertId(id);

    // 콘서트 티켓 가격 삭제
    concertTicketPriceRepository.deleteAllByConcertId(id);

    // 콘서트 게시물 삭제
    concertRepository.deleteById(id);
  }

  @Override
  public void updateConcertStatus(Long id) {

  }

  // 콘서트 이미지 업로드
  private ConcertUploadResultDto uploadFile(MultipartFile file) throws IOException {
    if (file != null && !file.isEmpty()) {
      String originalFilename = file.getOriginalFilename(); // 실제 파일명
      String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
      String savedFileName = UUID.randomUUID().toString().replace("-", "") + extension; // uuid.확장자

      File folder = new File(uploadPath + File.separator + UploadFolderType.CONCERT.getFolderName());
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

      return new ConcertUploadResultDto(savedFileName, fullPath);
    } else {
      return null; // 추후 빈파일이 넘어왔을 경우 예외 처리 필요
    }
  }

  private List<ConcertTicketOffice> saveTicketOffices(List<ConcertTicketOfficeDto> ticketOffices, Concert savedConcert) {
    if(ticketOffices == null) {
      return new ArrayList<>();
    }

    return ticketOffices.stream()
            .map(dto -> {
              ConcertTicketOffice office = new ConcertTicketOffice();
              office.setName(dto.getName());
              office.setLink(dto.getLink());
              office.setConcert(savedConcert);
              return office;
            }).collect(Collectors.toList());
  }

  private List<ConcertTicketPrice> saveTicketPrices(List<ConcertTicketPriceDto> ticketPrices, Concert savedConcert) {
    if(ticketPrices == null) {
      return new ArrayList<>();
    }

    return ticketPrices.stream()
            .map(dto -> {
              ConcertTicketPrice price = new ConcertTicketPrice();
              price.setSeat(dto.getSeat());
              price.setPrice(dto.getPrice());
              price.setConcert(savedConcert);
              return price;
            }).collect(Collectors.toList());
  }

  /**
   * 이미지 처리 로직
   */
  private void handleConcertImages(Concert concert, ConcertResponseDto concertResponseDto) {
    if(concert.getDataSource() == DataSource.KOPIS_API) {
      concertResponseDto.setImageUrl(concert.getKopisPoster());
      concertResponseDto.setSaveImageName(concert.getFileSavedName());

      // 상세 이미지 처리
      if(concert.getDetailImages() != null && !concert.getDetailImages().trim().isEmpty()) {
        String[] imageUrls = concert.getDetailImages().split(",");
        List<String> imageList = Arrays.asList(imageUrls);
        concertResponseDto.setConcertImages(imageList);
        log.info("상세 이미지 설정 완료: {}개", imageList.size());
      } else {
        log.info("상세 이미지 없음");
      }
    } else {
      concertResponseDto.setSaveImageName(concert.getFileSavedName());
    }
  }

  /**
   * 데이터 소수에 따라 티켓 정보 다르게 처리
   */
  private void handleTicketInformation(Concert concert, ConcertResponseDto responseDto) {
    if(concert.getDataSource() == DataSource.KOPIS_API) {
      handleKopisTicketData(concert, responseDto);
    } else {
      handleManualTicketData(concert, responseDto);
    }
  }

  /**
   * KOPIS 티켓 데이터 처리
   */
  private void handleKopisTicketData(Concert concert, ConcertResponseDto responseDto) {
    // 티켓 가격 파싱
    List<ConcertTicketPriceDto> ticketPriceDtos = parseKopisTicketPrices(concert.getTicketPrice());
    responseDto.setTicketPrices(ticketPriceDtos);

    // 예매처 파싱
//    List<ConcertTicketOfficeDto> ticketOfficeDtos = parseKopisTicketOffices(concert.getBookingSite());
//    responseDto.setTicketOffices(ticketOfficeDtos);

//    log.info("KOPIS 티켓 데이터 파싱 완료 - 가격: {}개, 예매처: {}개", ticketPriceDtos.size(), ticketOfficeDtos.size());
  }

  /**
   * 수기 등록 티켓 데이터 처리 (기존 로직)
   */
  private void handleManualTicketData(Concert concert, ConcertResponseDto responseDto) {
    List<ConcertTicketOffice> concertTicket = concertTicketOfficeRepository.findAllByConcertId(concert.getId());
    List<ConcertTicketPrice> concertPrice = concertTicketPriceRepository.findAllByConcertId(concert.getId());

    // 티켓 오피스 정보 설정
    List<ConcertTicketOfficeDto> concertTicketOfficeDtos = new ArrayList<>();
    for(ConcertTicketOffice ticketOffice : concertTicket) {
      ConcertTicketOfficeDto ticketOfficeDto = modelMapper.map(ticketOffice, ConcertTicketOfficeDto.class);
      concertTicketOfficeDtos.add(ticketOfficeDto);
    }
    responseDto.setTicketOffices(concertTicketOfficeDtos);


    // 티켓 가격 정보 설정
    List<ConcertTicketPriceDto> concertTicketPriceDtos = new ArrayList<>();
    for(ConcertTicketPrice ticketPrice : concertPrice) {
      ConcertTicketPriceDto ticketPriceDto = modelMapper.map(ticketPrice, ConcertTicketPriceDto.class);
      concertTicketPriceDtos.add(ticketPriceDto);
    }
    responseDto.setTicketPrices(concertTicketPriceDtos); // loop 밖에서 한 번만 설정

    log.info("수동 등록 티켓 데이터 조회 완료 - 가격: {}개, 예매처: {}개", concertTicketPriceDtos.size(), concertTicketOfficeDtos.size());
  }

  /**
   * KOPIS 티켓 가격 문자열 파싱
   * 예: "R석 80,000원, VIP석 150,000원" -> ConcertTicketPriceDto 리스트
   */
  private List<ConcertTicketPriceDto> parseKopisTicketPrices(String ticketPriceStr) {
    List<ConcertTicketPriceDto> ticketPrices = new ArrayList<>();

    if (ticketPriceStr == null || ticketPriceStr.trim().isEmpty()) {
      return ticketPrices;
    }

    log.debug("=== 티켓 가격 파싱 시작 ===");
    log.debug("원본 문자열: [{}]", ticketPriceStr);

    try {
      // 천단위 쉼표가 포함된 가격을 추출
      Pattern pattern = Pattern.compile("([A-Za-z가-힣]+석?)\\s*([0-9,]+원?)");
      Matcher matcher = pattern.matcher(ticketPriceStr);

      while(matcher.find()) {
        String seatGrade = matcher.group(1).trim();
        String priceStr = matcher.group(2);

        if(!priceStr.endsWith("원")) {
          priceStr += "원";
        }

        log.debug("매칭된 그룹: 좌석등급=[{}], 가격문자열=[{}]", seatGrade, priceStr);

        ConcertTicketPriceDto dto = new ConcertTicketPriceDto();
        dto.setSeat(seatGrade);
        dto.setPrice(priceStr);
        ticketPrices.add(dto);

        log.info("파싱 성공: {}석 {}원", seatGrade, priceStr);
      }

      log.info("=== 파싱 완료: 총 {}개 좌석 등급 ===", ticketPrices.size());

    } catch (Exception e) {
      log.error("티켓 가격 파싱 중 오류: {}", e.getMessage(), e);
    }

    return ticketPrices;
  }
}
