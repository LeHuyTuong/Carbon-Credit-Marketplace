package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.common.ListingStatus;
import com.carbonx.marketcarbon.model.CreditListing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreditListingRepository extends JpaRepository<CreditListing, Long> {
    List<CreditListing> findByStatusIn(List<ListingStatus> statuses);
}
