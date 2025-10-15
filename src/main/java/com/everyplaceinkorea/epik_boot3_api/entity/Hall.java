package com.everyplaceinkorea.epik_boot3_api.entity;

import com.everyplaceinkorea.epik_boot3_api.entity.common.DataSource;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "hall",
      indexes = {
        @Index(name = "idx_hall_facility", columnList = "facility_id")
      })
public class Hall {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;             // 고유 ID

  @Column(name = "hall_id", unique = true, nullable = false, length = 50)
  private String hallId;  // 공연장 ID (KOPIS mt13id)

  @Column(name = "name", nullable = false, length = 200)
  private String name;         // 공연장명

  @Column(name = "seat_count")
  private Integer seatCount;   // 좌석수

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "facility_id", nullable = false)
  private Facility facility;

  @Enumerated(EnumType.STRING)
  @Column(name = "data_source")
  private DataSource dataSource;

  @Column(name = "last_synced")
  private LocalDateTime lastSynced;
}
