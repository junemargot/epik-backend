package com.everyplaceinkorea.epik_boot3_api.entity;

import com.everyplaceinkorea.epik_boot3_api.entity.common.DataSource;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "facility")
public class Facility {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;         // 고유 ID

  @Column(name = "facility_id", unique = true, nullable = false, length = 50)
  private String facilityId; // 시설 ID

  @Column(name = "name", nullable = false, length = 200)
  private String name;       // 시설명

  @Column(name = "address", nullable = false, length = 500)
  private String address;    // 상세 주소

  @Column(name = "latitude")
  private Double latitude;   // 위도

  @Column(name = "longitude")
  private Double longitude;  // 경도

  @Column(name = "tel", length = 50)
  private String tel;        // 전화번호

  @Column(name = "url", length = 500)
  private String url;        // 홈페이지

  @Enumerated(EnumType.STRING)
  @Column(name = "data_source")
  private DataSource dataSource;

  @Column(name = "last_synced")
  private LocalDateTime lastSynced;

  @OneToMany(mappedBy = "facility", cascade = CascadeType.ALL)
  private List<Hall> halls = new ArrayList<>();
}
