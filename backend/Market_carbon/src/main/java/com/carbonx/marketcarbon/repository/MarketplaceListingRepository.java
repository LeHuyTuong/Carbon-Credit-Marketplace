package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.common.ListingStatus;
import com.carbonx.marketcarbon.model.MarketPlaceListing;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MarketplaceListingRepository extends JpaRepository<MarketPlaceListing,Long> {
    // Tìm các niêm yết còn hoạt động (chưa hết hạn và còn hàng)
    List<MarketPlaceListing> findByStatusAndExpiresAtAfter(ListingStatus status, LocalDate now);

    // Tìm một niêm yết và "KHÓA" nó lại để xử lý, tránh 2 người cùng mua một lúc
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MarketPlaceListing m WHERE m.id = :id")
    Optional<MarketPlaceListing> findByIdWithPessimisticLock(@Param("id") Long id);

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
}
