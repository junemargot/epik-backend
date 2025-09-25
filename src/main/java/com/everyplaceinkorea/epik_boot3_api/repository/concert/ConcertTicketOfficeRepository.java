package com.everyplaceinkorea.epik_boot3_api.repository.concert;

import com.everyplaceinkorea.epik_boot3_api.entity.concert.Concert;
import com.everyplaceinkorea.epik_boot3_api.entity.concert.ConcertTicketOffice;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface ConcertTicketOfficeRepository extends JpaRepository<ConcertTicketOffice, Long> {

  List<ConcertTicketOffice> findAllByConcertId(Long id);

  @Modifying
  @Transactional
  @Query("DELETE FROM ConcertTicketOffice c WHERE c.concert.id = :concertId")
  void deleteAllByConcertId(@Param("concertId") Long id);

  List<ConcertTicketOffice> findByConcert(Concert concert);
}
