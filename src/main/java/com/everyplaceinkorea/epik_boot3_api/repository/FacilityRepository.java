package com.everyplaceinkorea.epik_boot3_api.repository;

import com.everyplaceinkorea.epik_boot3_api.entity.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, Long> {

  // KOPIS facilityId(mt10id)로 시설 조회
  // 용도: API 동기화 시 중복 생성 방지
  Optional<Facility> findByFacilityId(String facilityId);

  // 시설명으로 조회
  Optional<Facility> findByName(String name);
}
