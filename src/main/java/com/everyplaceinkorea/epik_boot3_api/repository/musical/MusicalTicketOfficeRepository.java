package com.everyplaceinkorea.epik_boot3_api.repository.musical;

import com.everyplaceinkorea.epik_boot3_api.entity.musical.Musical;
import com.everyplaceinkorea.epik_boot3_api.entity.musical.MusicalTicketOffice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MusicalTicketOfficeRepository extends JpaRepository<MusicalTicketOffice, Long> {

    List<MusicalTicketOffice> findAllByMusicalId(Long id);

    @Modifying
    @Transactional
    @Query("DELETE FROM MusicalTicketOffice m WHERE m.musical.id = :musicalId")
    void deleteByMusicalId(@Param("musicalId") Long id);

    List<MusicalTicketOffice> findByMusical(Musical musical);
}
