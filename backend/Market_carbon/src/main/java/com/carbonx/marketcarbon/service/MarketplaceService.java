package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.request.CreditListingRequest;
import com.carbonx.marketcarbon.dto.request.OrderRequest;
import com.carbonx.marketcarbon.dto.response.MarketplaceListingResponse;
import com.carbonx.marketcarbon.model.CreditListing;
import com.carbonx.marketcarbon.model.MarketplaceListing;
import com.carbonx.marketcarbon.model.Transaction;

import java.util.List;


public interface MarketplaceService {
    MarketplaceListingResponse listCreditsForSale(CreditListingRequest request);
    List<MarketplaceListingResponse> getActiveListing();

}
