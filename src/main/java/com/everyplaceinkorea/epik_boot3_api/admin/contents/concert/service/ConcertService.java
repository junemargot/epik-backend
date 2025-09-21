package com.everyplaceinkorea.epik_boot3_api.admin.contents.concert.service;

import com.everyplaceinkorea.epik_boot3_api.admin.contents.concert.dto.ConcertListDto;
import com.everyplaceinkorea.epik_boot3_api.admin.contents.concert.dto.ConcertRequestDto;
import com.everyplaceinkorea.epik_boot3_api.admin.contents.concert.dto.ConcertResponseDto;
import com.everyplaceinkorea.epik_boot3_api.entity.concert.Concert;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ConcertService {

//  전체 조회(R)
//  ConcertListDto getList();
  ConcertListDto getList(int page, String keyword, String searchType);

  // 상세(R)
  ConcertResponseDto getById(Long id);

  // 등록(C)
  ConcertResponseDto create(ConcertRequestDto concertRequestDto, MultipartFile file) throws IOException;

  // 수정(U)
  ConcertResponseDto update(Long id, ConcertRequestDto concertRequestDto, MultipartFile file);

  // 삭제(D)
  void delete(Long id);

  // 검색

  // 상태 변경
  void updateConcertStatus(Long id);

}
