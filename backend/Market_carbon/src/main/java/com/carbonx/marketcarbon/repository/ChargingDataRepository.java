package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.model.ChargingData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChargingDataRepository extends JpaRepository<ChargingData, Long> {
}
