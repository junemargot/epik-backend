package com.everyplaceinkorea.epik_boot3_api.anonymous.contents.concert.service;

import com.everyplaceinkorea.epik_boot3_api.anonymous.contents.concert.dto.ConcertResponseDto;
import com.everyplaceinkorea.epik_boot3_api.entity.common.DataSource;
import com.everyplaceinkorea.epik_boot3_api.entity.concert.Concert;
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

    // 콘서트 랜덤 조회
    @Override
    public List<ConcertResponseDto> getConcertsByRandom() {
      LocalDate today = LocalDate.now();
      return concertRepository.findActiveConcertByRandom(today)
              .stream()
              .map(this::mapToResponseDto)
              .toList();
    }

  @Override
  public List<ConcertResponseDto> getConcertsByGenre(String genreName) {
    LocalDate today = LocalDate.now();
    return concertRepository.findConcertsByGenre(genreName, today)
            .stream()
            .map(this::mapToResponseDto)
            .toList();
  }

  private ConcertResponseDto mapToResponseDto(Concert concert) {
    ConcertResponseDto dto = modelMapper.map(concert, ConcertResponseDto.class);

    // 데이터 소스 설정
    dto.setDataSource(concert.getDataSource());

    // 이미지 처리: KOPIS API 데이터인 경우 원본 포스터 URL 사용
    if(concert.getDataSource() == DataSource.KOPIS_API) {
      dto.setImageUrl(concert.getKopisPoster());
    }

    // 모든 경우에 파일명 설정
    dto.setFileSavedName(concert.getFileSavedName());

    return dto;
  }

}
