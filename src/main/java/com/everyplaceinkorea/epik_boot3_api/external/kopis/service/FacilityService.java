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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
   * @return
   */
  @Transactional
  public Facility syncFacility(String kopisFacilityId) {
    log.info("시설 동기화 시작: {}", kopisFacilityId);

    try {
      // 1. KOPIS API 호출
      String xmlResponse = kopisApiService.getFacilityDetail(kopisFacilityId);
      if(xmlResponse == null || xmlResponse.trim().isEmpty()) {
        log.warn("시설 상세 정보 조회 실패: {}", kopisFacilityId);
        return null;
      }

      // 2. XML 파싱
      KopisFacilityDto dto = parseFacilityFromXml(xmlResponse);
      if(dto == null) {
        log.warn("시설 정보 파싱 실패: {}", kopisFacilityId);
        return null;
      }

      // 3. Facility 생성/업데이트
      Facility facility = facilityRepository.findByFacilityId(dto.getMt10id())
              .orElse(new Facility());

      facility.setFacilityId(dto.getMt10id());
      facility.setName(dto.getFcltynm());
      facility.setAddress(dto.getAdres());
      facility.setLatitude(parseDouble(dto.getLatitude()));
      facility.setLongitude(parseDouble(dto.getLongitude()));
      facility.setTel(dto.getTelno());
      facility.setUrl(dto.getRelateurl());
      facility.setDataSource(DataSource.KOPIS_API);
      facility.setLastSynced(LocalDateTime.now());
      facilityRepository.save(facility);
      log.info("시설 저장 완료: {} ({})", facility.getName(), facility.getFacilityId());

      // 4. Hall 동기화
      if(dto.getHalls() != null && !dto.getHalls().isEmpty()) {
        syncHalls(facility, dto.getHalls());
      }

      return facility;

    } catch (Exception e) {
      log.error("시설 동기화 실패: {}, 에러: {}", kopisFacilityId, e.getMessage(), e);
      return null;
    }
  }

  /**
   * 시설의 공연장 정보 동기화
   * @param facility
   * @param hallDtos
   */
  private void syncHalls(Facility facility, List<KopisHallDto> hallDtos) {
    log.info("공연장 동기화 시작: 시설={}, 공연장={}", facility.getName(), facility.getHalls());

    for(KopisHallDto hallDto : hallDtos) {
      try {
        Hall hall = hallRepository.findByHallId(hallDto.getMt13id())
                .orElse(new Hall());

        hall.setHallId(hallDto.getMt13id());
        hall.setName(hallDto.getPrfplcnm());
        hall.setSeatCount(parseSeatScale(hallDto.getSeatscale()));
        hall.setFacility(facility);
        hall.setDataSource(DataSource.KOPIS_API);
        hall.setLastSynced(LocalDateTime.now());
        hallRepository.save(hall);
        log.debug("공연장 저장 완료: {} ({}석)", hall.getName(), hall.getSeatCount());

      } catch (Exception e) {
        log.error("공연장 동기화 실패: {}, 에러: {}", hallDto.getPrfplcnm(), e.getMessage(), e);
      }
    }
    log.info("공연장 동기화 완료: 시설={}", facility.getName());
  }

  private KopisFacilityDto parseFacilityFromXml(String xmlResponse) {
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
      // mt13id, prfplcnm, seatscale은 반복되는 구조
      // 각 공연장마다 이 3개 태그가 순서대로 나타남
      Pattern mt13Pattern = Pattern.compile("<mt13id>(.*?)</mt13id>", Pattern.DOTALL);
      Pattern prfplcnmPattern = Pattern.compile("<prfplcnm>(.*?)</prfplcnm>", Pattern.DOTALL);
      Pattern seatscalePattern = Pattern.compile("<seatscale>(.*?)</seatscale>", Pattern.DOTALL);

      Matcher mt13Matcher = mt13Pattern.matcher(dbContent);
      Matcher prfplcnmMatcher = prfplcnmPattern.matcher(dbContent);
      Matcher seatscaleMatcher = seatscalePattern.matcher(dbContent);

      // 각 공연장 정보를 순서대로 매칭
      while(mt13Matcher.find() && prfplcnmMatcher.find()) {
        KopisHallDto hallDto = new KopisHallDto();
        hallDto.setMt13id(mt13Matcher.group(1).trim());
        hallDto.setPrfplcnm(prfplcnmMatcher.group(1).trim());

        if(seatscaleMatcher.find()) {
          hallDto.setSeatscale(seatscaleMatcher.group(1).trim());
        }

        halls.add(hallDto);
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
}
