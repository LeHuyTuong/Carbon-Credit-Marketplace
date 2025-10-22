package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.CreditSerialCounter;
import com.carbonx.marketcarbon.model.Project;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CreditSerialCounterRepository extends JpaRepository<CreditSerialCounter, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           select c from CreditSerialCounter c
           where c.vintageYear = :year and c.project = :project and c.company = :company
           """)
    Optional<CreditSerialCounter> lockBy(@Param("year") int year,
                                         @Param("project") Project project,
                                         @Param("company") Company company);
}
