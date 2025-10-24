package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.response.CarbonCreditResponse;
import com.carbonx.marketcarbon.dto.response.CreditBatchLiteResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MyCreditService {
    Page<CarbonCreditResponse> listMyCredits(CreditQuery query, Pageable pageable);
    Page<CreditBatchLiteResponse> listMyBatches(Long projectId, Integer vintageYear, Pageable pageable);
    CarbonCreditResponse getMyCreditById(Long creditId);
    CarbonCreditResponse getMyCreditByCode(String creditCode);
}
