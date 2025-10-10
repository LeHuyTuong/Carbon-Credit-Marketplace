package com.carbonx.marketcarbon.repository;


import com.carbonx.marketcarbon.model.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByPlateNumber(String plateNumber);

    // EV Owner
    @EntityGraph(attributePaths = "company") // tránh bị N+1 khi lấy companyId
    List<Vehicle> findByOwnerId(Long ownerId);

    // Company
    Page<Vehicle> findByCompany_Id(Long userId, Pageable pageable);
}
