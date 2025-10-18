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
    @EntityGraph(attributePaths = "company") // tránh N+1 khi lấy company
    List<Vehicle> findByEvOwner_Id(Long evOwnerId);

    // Company
    Page<Vehicle> findByCompany_Id(Long userId, Pageable pageable);

    Optional<Vehicle> findByCompanyIdAndPlateNumberIgnoreCase(Long companyId, String plateNumber);
}
