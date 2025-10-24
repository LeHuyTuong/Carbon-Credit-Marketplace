package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.response.CreditInventorySummaryResponse;

public interface MyCreditInventoryService {
    CreditInventorySummaryResponse getMyInventorySummary();
    long getMyAvailableBalance();
}
