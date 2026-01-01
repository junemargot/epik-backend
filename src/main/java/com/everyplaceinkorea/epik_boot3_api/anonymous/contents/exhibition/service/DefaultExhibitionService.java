package com.everyplaceinkorea.epik_boot3_api.anonymous.contents.exhibition.service;

import com.everyplaceinkorea.epik_boot3_api.anonymous.contents.exhibition.dto.ExhibitionResponseDto;
import com.everyplaceinkorea.epik_boot3_api.entity.exhibition.Exhibition;
import com.everyplaceinkorea.epik_boot3_api.repository.exhibition.ExhibitionRepository;
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
public class DefaultExhibitionService implements ExhibitionService {

    private final ExhibitionRepository exhibitionRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<ExhibitionResponseDto> getExhibitionsByRegion(Long regionId, Integer page) {

        // 정렬 기준 만들기
        Sort sort = Sort.by("id").descending();
        // 페이징조건 만들기
        Pageable pageable = PageRequest.of(page - 1, 15, sort);

        Page<Exhibition> musicals = exhibitionRepository.findExhibitionsByRegion(regionId, LocalDate.now(), pageable);
//        musicals.getContent().forEach(System.out::println);
        List<ExhibitionResponseDto> responseDtos = musicals
                .getContent()
                .stream()
                .map(exhibition ->{
                    ExhibitionResponseDto responseDto = modelMapper.map(exhibition, ExhibitionResponseDto.class);

                    LocalDate today = LocalDate.now();
                    if(today.isBefore(exhibition.getStartDate())) {
                        responseDto.setPerformanceStatus("공연예정");
                    } else if(today.isAfter(exhibition.getEndDate())) {
                        responseDto.setPerformanceStatus("종료");
                    } else {
                        responseDto.setPerformanceStatus("진행중");
                    }

                    return responseDto;
                })
                .toList();

        return responseDtos;
    }

    // 전시회 랜덤 조회
    @Override
    public List<ExhibitionResponseDto> getExhibitionsByRandom() {
        LocalDate today = LocalDate.now();
        List<Exhibition> exhibitions = exhibitionRepository.findActiveExhibitionByRandom(today);

        return exhibitions.stream()
            .map(exhibition -> {
                ExhibitionResponseDto dto = modelMapper.map(exhibition, ExhibitionResponseDto.class);

                if(today.isBefore(exhibition.getStartDate())) {
                    dto.setPerformanceStatus("공연예정");
                } else if(today.isAfter(exhibition.getEndDate())) {
                    dto.setPerformanceStatus("종료");
                } else {
                    dto.setPerformanceStatus("진행중");
                }

                return dto;
            }).toList();
    }
}

