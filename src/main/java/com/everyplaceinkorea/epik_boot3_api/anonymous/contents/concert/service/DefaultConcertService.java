package com.everyplaceinkorea.epik_boot3_api.anonymous.contents.concert.service;

import com.everyplaceinkorea.epik_boot3_api.anonymous.contents.concert.dto.ConcertResponseDto;
import com.everyplaceinkorea.epik_boot3_api.entity.common.DataSource;
import com.everyplaceinkorea.epik_boot3_api.entity.concert.Concert;
import com.everyplaceinkorea.epik_boot3_api.image.service.ImageCacheService;
import com.everyplaceinkorea.epik_boot3_api.repository.concert.ConcertRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DefaultConcertService implements ConcertService {

    private final ConcertRepository concertRepository;
    private final ModelMapper modelMapper;
    private final ImageCacheService imageCacheService;

    @Override
    public List<ConcertResponseDto> getConcertsByRegion(Long regionId, Integer page) {
        Sort sort = Sort.by("id").descending();
        Pageable pageable = PageRequest.of(page - 1, 15, sort);
        return concertRepository.findConcertsByRegion(regionId, LocalDate.now(), pageable)
                .getContent()
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

  @Override
  public List<ConcertResponseDto> getConcertsByRegion(Long regionId) {
    LocalDate today = LocalDate.now();
    return concertRepository.findAllConcertsByRegion(regionId, today)
            .stream()
            .map(this::mapToResponseDto)
            .toList();
  }

  // 콘서트 랜덤 조회
    @Override
    public List<ConcertResponseDto> getConcertsByRandom(Integer page) {
      LocalDate today = LocalDate.now();
      Pageable pageable = PageRequest.of(page - 1, 50);
      return concertRepository.findActiveConcertByRandom(today, pageable)
              .stream()
              .map(this::mapToResponseDto)
              .toList();
    }

  @Override
  public List<ConcertResponseDto> getConcertsByRandom() {
    LocalDate today = LocalDate.now();
    return concertRepository.findAllActiveConcertByRandom(today)
            .stream()
            .map(this::mapToResponseDto)
            .toList();
  }

  @Override
  public List<ConcertResponseDto> getConcertsByGenre(String genreName, Integer page) {
    LocalDate today = LocalDate.now();
    Sort sort = Sort.by("startDate").ascending();
    Pageable pageable = PageRequest.of(page - 1, 15, sort);
    Page<Concert> concertPage = concertRepository.findConcertsByGenre(genreName, today, pageable);
    return concertPage.getContent()
            .stream()
            .map(this::mapToResponseDto)
            .toList();
  }

  @Override
  public List<ConcertResponseDto> getConcertsByGenre(String genreName) {
    LocalDate today = LocalDate.now();
    return concertRepository.findAllConcertsByGenre(genreName, today)
            .stream()
            .map(this::mapToResponseDto)
            .toList();
  }


  private ConcertResponseDto mapToResponseDto(Concert concert) {
    ConcertResponseDto dto = modelMapper.map(concert, ConcertResponseDto.class);

    // 공연 상태 설정
    LocalDate today = LocalDate.now();
    if(today.isBefore(concert.getStartDate())) {
      dto.setPerformanceStatus("공연예정");
    } else if(today.isAfter(concert.getEndDate())) {
      dto.setPerformanceStatus("종료");
    } else {
      dto.setPerformanceStatus("진행중");
    }

    // 데이터 소스 설정
    dto.setDataSource(concert.getDataSource());

    // 장르 정보 설정
    dto.setGenreName(concert.getKopisGenrenm());

    // 지역 설정
    dto.setRegionName(concert.getKopisArea());

    // 이미지 처리: KOPIS API 데이터인 경우 원본 포스터 URL 사용
    if(concert.getDataSource() == DataSource.KOPIS_API) {
      String cachedPosterUrl = imageCacheService.getOrCacheImage(
              concert.getKopisPoster(),
              concert.getId().toString()
      );
      dto.setImageUrl(cachedPosterUrl);
    }

    // 모든 경우에 파일명 설정
    dto.setFileSavedName(concert.getFileSavedName());

    return dto;
  }

}
