package com.carbonx.marketcarbon.repository;


import com.carbonx.marketcarbon.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    boolean existsByPlateNumber(String plateNumber);

    Optional<Object> findByPlateNumber(String plateNumber);
}
