package com.everyplaceinkorea.epik_boot3_api.entity.member;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "member_management")
public class MemberManagement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "delete_reason", columnDefinition = "TEXT")
  private String deleteReason;

  @Column(name = "delete_date", columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)")
  private Instant deleteDate;

  @Column(name = "resign_reason", columnDefinition = "TEXT")
  private String resignReason;

  @Column(name = "resign_date", columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)")
  private Instant resignDate;
}
