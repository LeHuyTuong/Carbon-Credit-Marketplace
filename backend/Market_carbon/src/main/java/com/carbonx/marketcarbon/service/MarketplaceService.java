package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.request.CreditListingRequest;
import com.carbonx.marketcarbon.dto.response.MarketplaceListingResponse;

import java.util.List;


public interface MarketplaceService {
    MarketplaceListingResponse listCreditsForSale(CreditListingRequest request);
    List<MarketplaceListingResponse> getActiveListing();

}
