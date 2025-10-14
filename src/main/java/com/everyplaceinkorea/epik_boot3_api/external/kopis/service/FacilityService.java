package com.everyplaceinkorea.epik_boot3_api.external.kopis.service;

import com.everyplaceinkorea.epik_boot3_api.entity.Facility;
import com.everyplaceinkorea.epik_boot3_api.entity.Hall;
import com.everyplaceinkorea.epik_boot3_api.entity.common.DataSource;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.KopisApiService;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.dto.KopisFacilityDto;
import com.everyplaceinkorea.epik_boot3_api.external.kopis.dto.KopisHallDto;
import com.everyplaceinkorea.epik_boot3_api.repository.FacilityRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.HallRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Double.parseDouble;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacilityService {

  private final KopisApiService kopisApiService;
  private final FacilityRepository facilityRepository;
  private final HallRepository hallRepository;

  /**
   * KOPIS API에서 시설 정보를 가져와 동기화
   * @param kopisFacilityId
   * @return Facility 엔티티 (Optional)
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 30)
  public Optional<Facility> syncFacility(String kopisFacilityId) {
    log.info("시설 동기화 시작: {}", kopisFacilityId);

    try {
      // 1. KOPIS API 호출 및 파싱
      String xmlResponse = kopisApiService.getFacilityDetail(kopisFacilityId);
      if (xmlResponse == null || xmlResponse.trim().isEmpty()) {
        log.warn("시설 API 응답 없음: {}", kopisFacilityId);
        return Optional.empty();
      }
      KopisFacilityDto dto = parseFacilityFromXml(xmlResponse);
      if (dto == null) {
        log.warn("시설 파싱 실패: {}", kopisFacilityId);
        return Optional.empty();
      }

      // 2. Facility 엔티티 찾기 또는 생성
      Facility facility = facilityRepository.findByFacilityId(dto.getMt10id())
              .orElseGet(() -> {
                Facility newFacility = new Facility();
                newFacility.setFacilityId(dto.getMt10id()); // 새 엔티티에 KOPIS ID 설정
                return newFacility;
              });

      // 3. Facility 정보 업데이트
      updateFacility(facility, dto);

      // 4. Facility 먼저 저장 (ID 생성)
      Facility savedFacility = facilityRepository.saveAndFlush(facility);

      // 5. 별도 메서드로 Hall 동기화 (새 영속성 컨텍스트)
      if (dto.getHalls() != null && !dto.getHalls().isEmpty()) {
        syncHallsWithFacility(savedFacility.getId(), dto.getHalls());
      }
      return Optional.of(savedFacility);

    } catch (DataAccessException e) {
      log.error("시설 동기화 실패 (DB 오류): facilityId={}", kopisFacilityId, e);
      throw new RuntimeException("시설 DB 저장 실패: " + kopisFacilityId, e);

    } catch (WebClientException e) {
      log.error("시설 동기화 실패 (API 오류): facilityId={}", kopisFacilityId, e);
      return Optional.empty();

    } catch (Exception e) {
      log.error("시설 동기화 실패 (예상치 못한 오류): facilityId={}", kopisFacilityId, e);
      return Optional.empty();
    }
  }

  /*
   * Facility 정보 업데이트
   */
  private void updateFacility(Facility facility, KopisFacilityDto dto) {
    facility.setName(dto.getFcltynm());
    facility.setAddress(dto.getAdres());
    facility.setLatitude(parseCoordinate(dto.getLatitude()));
    facility.setLongitude(parseCoordinate(dto.getLongitude()));
    facility.setTel(dto.getTelno());
    facility.setUrl(dto.getRelateurl());
    facility.setDataSource(DataSource.KOPIS_API);
    facility.setLastSynced(LocalDateTime.now());
  }

  /**
   * Hall 동기화 (별도 트랜잭션)
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void syncHallsWithFacility(Long facilityId, List<KopisHallDto> hallDtos) {
    try {
      // Facility를 다시 조회 (깨끗한 영속성 컨텍스트)
      Facility facility = facilityRepository.findById(facilityId)
              .orElseThrow(() -> new RuntimeException("Facility not found: " + facilityId));

      log.info("Hall 동기화 시작: {} (ID={}), Hall 개수={}",
              facility.getName(), facilityId, hallDtos.size());

      int hallCount = 0;
      for (KopisHallDto hallDto : hallDtos) {
        try {
          Hall hall = hallRepository.findByHallId(hallDto.getMt13id())
                  .orElse(new Hall());

          hall.setHallId(hallDto.getMt13id());
          hall.setName(hallDto.getPrfplcnm());
          hall.setSeatCount(parseSeatScale(hallDto.getSeatscale()));
          hall.setDataSource(DataSource.KOPIS_API);
          hall.setLastSynced(LocalDateTime.now());
          hall.setFacility(facility);

          hallRepository.save(hall);
          hallCount++;
          log.debug("  ✅ Hall 저장: {}", hall.getName());

        } catch (Exception e) {
          log.error("  ❌ Hall 저장 실패: {}, 에러: {}", hallDto.getPrfplcnm(), e.getMessage());
        }
      }

      log.info("✅ Hall {} 개 저장 완료", hallCount);

    } catch (Exception e) {
      log.error("❌ Hall 동기화 실패: facilityId={}, 에러: {}", facilityId, e.getMessage(), e);
    }
  }

  public KopisFacilityDto parseFacilityFromXml(String xmlResponse) {
    try {
      // <db> 블록 추출
      Pattern dbPattern = Pattern.compile("<db>(.*?)</db>", Pattern.DOTALL);
      Matcher dbMatcher = dbPattern.matcher(xmlResponse);

      if(!dbMatcher.find()) {
        log.warn("XML에서 <db> 블록을 찾을 수 없음");
        return null;
      }

      String dbContent = dbMatcher.group(1);

      // 시설 기본 정보 추출
      KopisFacilityDto dto = new KopisFacilityDto();
      dto.setMt10id(extractXmlValue(dbContent, "mt10id"));
      dto.setFcltynm(extractXmlValue(dbContent, "fcltynm"));
      dto.setAdres(extractXmlValue(dbContent, "adres"));
      dto.setSidonm(extractXmlValue(dbContent, "sidonm"));
      dto.setGugunm(extractXmlValue(dbContent, "gugunm"));
      dto.setTelno(extractXmlValue(dbContent, "telno"));
      dto.setRelateurl(extractXmlValue(dbContent, "relateurl"));
      dto.setLatitude(extractXmlValue(dbContent, "la"));
      dto.setLongitude(extractXmlValue(dbContent, "lo"));

      // 공연장 정보 추출
      List<KopisHallDto> halls = parseHallsFromXml(dbContent);
      dto.setHalls(halls);

      return dto;

    } catch (Exception e) {
      log.error("시설 정보 파싱 실패: {}", e.getMessage(), e);
      return null;
    }
  }

  // XML 태그에서 값 추출
  private String extractXmlValue(String dbContent, String tagName) {
    Pattern pattern = Pattern.compile("<" + tagName + ">(.*?)</" + tagName + ">", Pattern.DOTALL);
    Matcher matcher = pattern.matcher(dbContent);
    if(matcher.find()) {
      String value = matcher.group(1).trim();

      // CDATA 처리
      if (value.startsWith("<![CDATA[") && value.endsWith("]]>")) {
        value = value.substring(9, value.length() - 3);
      }

      return value.isEmpty() ? null : value;
    }

    return null;
  }

  // XML에서 공연장 정보 리스트 추출
  private List<KopisHallDto> parseHallsFromXml(String dbContent) {
    List<KopisHallDto> halls = new ArrayList<>();

    try {
      // mt13s 블록 추출
      Pattern mt13sPattern = Pattern.compile("<mt13s>(.*?)</mt13s>", Pattern.DOTALL);
      Matcher mt13sMatcher = mt13sPattern.matcher(dbContent);
      if(!mt13sMatcher.find()) {
        log.warn("XML에서 <mt13s> 블록을 찾을 수 없음");
        return halls;
      }

      String mt13sContent = mt13sMatcher.group(1);
      log.debug("mt13s 블록 추출 완료");

      // mt13 블록 추출
      Pattern mt13Pattern = Pattern.compile("<mt13>(.*?)</mt13>", Pattern.DOTALL);
      Matcher mt13Matcher = mt13Pattern.matcher(mt13sContent);

      // 각 공연장 정보를 순서대로 매칭
      while(mt13Matcher.find()) {
        String mt13Block = mt13Matcher.group(1);
        KopisHallDto hallDto = new KopisHallDto();

        hallDto.setMt13id(extractXmlValue(mt13Block, "mt13id"));
        hallDto.setPrfplcnm(extractXmlValue(mt13Block,"prfplcnm"));
        hallDto.setSeatscale(extractXmlValue(mt13Block, "seatscale"));

        halls.add(hallDto);
        log.debug("공연장 파싱: {} (좌석: {})", hallDto.getPrfplcnm(), hallDto.getSeatscale());
      }

      log.debug("공연장 {}개 파싱 완료", halls.size());

    } catch (Exception e) {
      log.error("공연장 정보 파싱 실패: {}", e.getMessage(), e);
    }

    return halls;
  }

  private Integer parseSeatScale(String seatscale) {
    try {
      return seatscale != null ? Integer.parseInt(seatscale.trim()) : null;
    } catch (NumberFormatException e) {
      log.warn("좌석 규모 변환 실패: {}", seatscale);
      return null;
    }
  }

  private Double parseCoordinate(String coordinate) {
    if(coordinate == null || coordinate.trim().isEmpty()) {
      return null;
    }

    try {
      return parseDouble(coordinate.trim());
    } catch (NumberFormatException e) {
      log.warn("좌표 변환 실패: {}", coordinate);
      return null;
    }
  }
}
