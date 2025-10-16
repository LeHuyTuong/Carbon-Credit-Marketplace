package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.ChargingData;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChargingDataRepository extends JpaRepository<ChargingData, Long> {
    @Query("""
  select coalesce(sum(c.chargingEnergy), 0)
  from ChargingData c
  where c.vehicle.evOwner.company.id = :companyId
    and c.timestamp >= :from and c.timestamp < :to
""")
    BigDecimal sumEnergyByCompanyAndRange(@Param("companyId") Long companyId,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);


    @Query("""
  select c from ChargingData c
  where c.vehicle.evOwner.id = :evOwnerId
    and c.timestamp >= :from and c.timestamp < :to
  order by c.timestamp asc
""")
    List<ChargingData> findOwnerMonth(@Param("evOwnerId") Long evOwnerId,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    List<ChargingData> findByVehicle_Company_IdAndTimestampBetween(
            Long companyId, LocalDateTime from, LocalDateTime to);
}
