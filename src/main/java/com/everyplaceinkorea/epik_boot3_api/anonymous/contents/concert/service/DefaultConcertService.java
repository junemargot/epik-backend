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

        // 정렬 기준 만들기
        Sort sort = Sort.by("id").descending();
        // 페이징조건 만들기
        Pageable pageable = PageRequest.of(page - 1, 15, sort);

        Page<Concert> concerts = concertRepository.findConcertsByRegion(regionId, LocalDate.now(), pageable);
//        concerts.getContent().forEach(System.out::println);
        List<ConcertResponseDto> responseDtos = concerts
                .getContent()
                .stream()
                .map(Concert ->{
                    ConcertResponseDto responseDto = modelMapper.map(Concert, ConcertResponseDto.class);
                    return responseDto;
                })
                .toList();

        return responseDtos;
    }

    // 콘서트 랜덤 조회
    @Override
    public List<ConcertResponseDto> getConcertsByRandom() {
      LocalDate today = LocalDate.now();
      List<Concert> concerts = concertRepository.findActiveConcertByRandom(today);

      return concerts.stream()
              .map(concert -> {
                ConcertResponseDto dto = modelMapper.map(concert, ConcertResponseDto.class);

                // 데이터 소스 설정
                dto.setDataSource(concert.getDataSource());
                
                // 이미지 처리 로직 개선
                if(concert.getDataSource() == DataSource.KOPIS_API) {
                  dto.setImageUrl(concert.getKopisPoster()); // KOPIS 원본 포스터 URL
                  dto.setFileSavedName(concert.getFileSavedName()); // 파일명 설정
                } else {
                  dto.setFileSavedName(concert.getFileSavedName());
                }

                return dto;
              })
              .toList();
    }

}
