package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.request.CreditListingRequest;
import com.carbonx.marketcarbon.dto.request.CreditListingUpdateRequest;
import com.carbonx.marketcarbon.dto.response.MarketplaceListingResponse;

import java.util.List;


public interface MarketplaceService {
    MarketplaceListingResponse listCreditsForSale(CreditListingRequest request);
    List<MarketplaceListingResponse> getActiveListing();

    MarketplaceListingResponse updateListCredits(CreditListingUpdateRequest request);
    MarketplaceListingResponse deleteListCredits(Long creditListingId);
}
