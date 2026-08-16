package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.common.ListingStatus;
import com.carbonx.marketcarbon.model.MarketPlaceListing;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MarketplaceListingRepository extends JpaRepository<MarketPlaceListing,Long> {
    // Tìm các niêm yết còn hoạt động (chưa hết hạn và còn hàng)
    List<MarketPlaceListing> findByStatusAndExpiresAtAfter(ListingStatus status, LocalDate now);

    // Eager-load all related entities in a single query to eliminate N+1
    @Query("SELECT DISTINCT ml FROM MarketPlaceListing ml " +
           "LEFT JOIN FETCH ml.company c " +
           "LEFT JOIN FETCH c.wallet " +
           "LEFT JOIN FETCH ml.carbonCredit cc " +
           "LEFT JOIN FETCH cc.project " +
           "LEFT JOIN FETCH cc.batch b " +
           "LEFT JOIN FETCH b.certificate " +
           "LEFT JOIN FETCH cc.company cc_company " +
           "LEFT JOIN FETCH cc_company.wallet " +
           "WHERE ml.status = :status AND ml.expiresAt > :now")
    List<MarketPlaceListing> findByStatusAndExpiresAtAfterWithDetails(
            @Param("status") ListingStatus status, @Param("now") LocalDate now);

    @Query("SELECT DISTINCT ml FROM MarketPlaceListing ml " +
           "LEFT JOIN FETCH ml.company c " +
           "LEFT JOIN FETCH c.wallet " +
           "LEFT JOIN FETCH ml.carbonCredit cc " +
           "LEFT JOIN FETCH cc.project " +
           "LEFT JOIN FETCH cc.batch b " +
           "LEFT JOIN FETCH b.certificate " +
           "LEFT JOIN FETCH cc.company cc_company " +
           "LEFT JOIN FETCH cc_company.wallet " +
           "WHERE ml.company.id = :companyId")
    List<MarketPlaceListing> findByCompanyIdWithDetails(@Param("companyId") Long companyId);

    // Tìm một niêm yết và "KHÓA" nó lại để xử lý, tránh 2 người cùng mua một lúc
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MarketPlaceListing m WHERE m.id = :id")
    Optional<MarketPlaceListing> findByIdWithPessimisticLock(@Param("id") Long id);

    // PESSIMISTIC_WRITE lock + eager-load company and carbonCredit in a single query
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT DISTINCT m FROM MarketPlaceListing m " +
           "LEFT JOIN FETCH m.company " +
           "LEFT JOIN FETCH m.carbonCredit " +
           "WHERE m.id = :id")
    Optional<MarketPlaceListing> findByIdWithPessimisticLockAndDetails(@Param("id") Long id);

    // Eager-load company and carbonCredit without lock (for read operations)
    @Query("SELECT DISTINCT m FROM MarketPlaceListing m " +
           "LEFT JOIN FETCH m.company " +
           "LEFT JOIN FETCH m.carbonCredit " +
           "WHERE m.id = :id")
    Optional<MarketPlaceListing> findByIdWithDetails(@Param("id") Long id);

    List<MarketPlaceListing> findByCompanyId(Long id);

    // Tìm các niêm yết đã hết hạn và vẫn đang AVAILABLE
    List<MarketPlaceListing> findByStatusAndExpiresAtBefore(ListingStatus status, LocalDate now);

    //listing theo công ty và carbon credit
    List<MarketPlaceListing> findByCompanyIdAndCarbonCreditIdAndStatus(
            Long companyId,
            Long carbonCreditId,
            ListingStatus status);

    // listing theo công ty và batch
    List<MarketPlaceListing> findByCompanyIdAndCarbonCredit_Batch_IdAndStatus(
            Long companyId,
            Long batchId,
            ListingStatus status);

    @Query("""
    SELECT SUM(m.pricePerCredit * m.quantity) / SUM(m.quantity)
    FROM MarketPlaceListing m
    """)
    Double getWeightedAveragePrice();


}
