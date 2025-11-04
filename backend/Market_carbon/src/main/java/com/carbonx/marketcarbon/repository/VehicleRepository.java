package com.carbonx.marketcarbon.repository;


import com.carbonx.marketcarbon.model.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByPlateNumber(String plateNumber);

    // EV Owner
    @EntityGraph(attributePaths = "company") // tránh N+1 khi lấy company
    List<Vehicle> findByEvOwner_Id(Long evOwnerId);


    Optional<Vehicle> findByCompanyIdAndPlateNumberIgnoreCase(Long companyId, String plateNumber);

    List<Vehicle> findByCompanyId(Long companyId);

    // Company
    Page<Vehicle> findByCompany_Id(Long userId, Pageable pageable);

    long count(); // count all vehicle for admin

    long countByEvOwner_Id(Long evOwnerId);

    /**
     * Lấy ra một Set (HashSet) chứa tất cả các biển số xe (plateNumber)
     * đã được đăng ký và có liên kết với một EVOwner.
     * @return Set<String> các biển số xe hợp lệ.
     */
    @Query("SELECT v.plateNumber FROM Vehicle v WHERE v.evOwner IS NOT NULL")
    Set<String> findAllRegisteredPlateNumbers();

    /**
     * Tìm Vehicle bằng plateNumber và fetch EAGER EVOwner và User
     * để tránh N+1 query khi xử lý.
     */
    @Query("SELECT v FROM Vehicle v JOIN FETCH v.evOwner e JOIN FETCH e.user u WHERE v.plateNumber = :plateNumber")
    Optional<Vehicle> findByPlateNumberWithDetails(String plateNumber);

    long countByCompany_Id(Long companyId);

}
