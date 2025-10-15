package com.everyplaceinkorea.epik_boot3_api.repository;

import com.everyplaceinkorea.epik_boot3_api.entity.Hall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HallRepository extends JpaRepository<Hall, Long> {

  /**
   * KOPIS hallId(mt13id)로 공연장 조회
   * 용도: KOPIS API 동기화 시 중복 생성 방지
   */
  Optional<Hall> findByHallId(String hallId);

  /**
   * 시설 ID로 공연장 목록 조회
   * 용도: 특정 시설의 공연장 목록 표시
   * 기존 findByFacility보다 효율적 (Facility 객체 조회 불필요)
   */
  List<Hall> findByFacility_id(Long facilityId);

  /**
   * KOPIS 시설 ID로 공연장 목록 조회 (선택사항)
   * 용도: KOPIS facilityId로 직접 조회
   */
  List<Hall> findByFacility_FacilityId(String kopisFacilityId);

  /**
   * 시설 ID와 공연장명으로 조회 (선택사항)
   * 용도: Concert의 venue 파싱 시 정확한 매칭
   * 예: facilityId=1, name="콘서트홀"
   */
  Optional<Hall> findByFacility_IdAndName(Long facilityId, String name);

  /**
   * 시설 객체로 공연장 목록 조회 (기존 메서드 - 유지 가능)
   * 용도: Facility 엔티티를 이미 가지고 있을 때
   */
  List<Hall> findByFacility(com.everyplaceinkorea.epik_boot3_api.entity.Facility facility);
}
