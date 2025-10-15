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
   * KOPIS API에서 시설 및 하위 공연장 정보를 가져와 DB에 동기화(생성 또는 업데이트)
   * 예외 처리:
   * - DataAccessException: DB 오류는 RuntimeException을 발생시켜 트랜잭션 롤백
   * - API 호출 오류나 파싱 실패 시에는 Optional.empty()를 반환
   *
   * @param kopisFacilityId 동기화할 대상의 KOPIS 시설 ID ("FC001242")
   * @return 동기화에 성공한 Facility 엔티티를 포함하는 Optional. 실패 시 Optional.empty()
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 60)
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
      log.debug("시설 저장: {} (ID={})", savedFacility.getName(), savedFacility.getId());

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

  /**
   * DTO로부터 받은 최신 정보로 Facility 엔티티의 필드를 업데이트하는 헬퍼 메서드
   *
   * @param facility 업데이트할 Facility 엔티티 (DB에서 조회했거나 새로 생성된 객체)
   * @param dto      KOPIS API로부터 파싱된 최신 시설 정보를 담은 DTO
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
   * 특정 시설에 속한 모든 공연장(Hall) 정보를 데이터베이스에 동기화
   * 별도의 트랜잭션으로 실행, 공연장 동기화 실패가 시설 동기화로부터 격리
   * 개별 공연장 저장 실패 시, 오류를 로그로 남기고 다음 공연장 동기화 진행
   *
   * @param facilityId 부모 Facility 엔티티의 데이터베이스 ID
   * @param hallDtos   동기화할 공연장 정보 DTO 리스트
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
          log.debug("Hall 저장: {}", hall.getName());

        } catch (Exception e) {
          log.error("Hall 저장 실패: {}, 에러: {}", hallDto.getPrfplcnm(), e.getMessage());
        }
      }

      log.info("Hall {} 개 저장 완료", hallCount);

    } catch (Exception e) {
      log.error("Hall 동기화 실패: facilityId={}, 에러: {}", facilityId, e.getMessage(), e);
    }
  }

  /**
   * KOPIS 시설 상세 API의 원본 XML 응답 문자열을 KopisFacilityDto 객체로 파싱
   *
   * @param xmlResponse KOPIS API의 XML 응답 원본 문자열
   * @return 파싱된 데이터가 채워진 KopisFacilityDto 객체. 파싱 실패 시 null
   */
  public KopisFacilityDto parseFacilityFromXml(String xmlResponse) {
    try {
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

  /**
   * XML 형식의 문자열에서 특정 태그에 해당하는 값을 추출하는 유틸리티 메서드
   *
   * @param dbContent 값을 추출할 XML 문자열
   * @param tagName   추출 대상 태그의 이름
   * @return 추출된 값. 태그가 없거나 값이 비어있으면 null
   */
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

  /**
   * 시설 정보 XML 내용 중, 하위 공연장 목록에 해당하는 부분을 파싱하여 KopisHallDto 리스트 생성
   *
   * @param dbContent 시설 정보 XML의 <db> 블록 내부 문자열
   * @return 파싱된 KopisHallDto 객체의 리스트. 공연장 정보가 없거나 파싱 실패 시 빈 리스트
   */
  private List<KopisHallDto> parseHallsFromXml(String dbContent) {
    List<KopisHallDto> halls = new ArrayList<>();

    try {
      Pattern mt13sPattern = Pattern.compile("<mt13s>(.*?)</mt13s>", Pattern.DOTALL);
      Matcher mt13sMatcher = mt13sPattern.matcher(dbContent);
      if(!mt13sMatcher.find()) {
        log.warn("XML에서 <mt13s> 블록을 찾을 수 없음");
        return halls;
      }

      String mt13sContent = mt13sMatcher.group(1);
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
      }

    } catch (Exception e) {
      log.error("공연장 정보 파싱 실패: {}", e.getMessage(), e);
    }

    return halls;
  }

  /**
   * 콤마(,)가 포함된 좌석 수 문자열을 Integer 타입으로 변환
   *
   * @param seatscale 좌석 수를 나타내는 문자열 (예: "1,500", "800")
   * @return 변환된 Integer 값. 변환 실패 또는 입력이 null이면 null
   */
  private Integer parseSeatScale(String seatscale) {
    try {
      return seatscale != null ? Integer.parseInt(seatscale.trim()) : null;
    } catch (NumberFormatException e) {
      log.warn("좌석 규모 변환 실패: {}", seatscale);
      return null;
    }
  }

  /**
   * 좌표(위도/경도) 문자열을 Double 타입으로 변환
   *
   * @param coordinate 좌표를 나타내는 문자열
   * @return 변환된 Double 값. 변환 실패 또는 입력이 null이면 null
   */
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
