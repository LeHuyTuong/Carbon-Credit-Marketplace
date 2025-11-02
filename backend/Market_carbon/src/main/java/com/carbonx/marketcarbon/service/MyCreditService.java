package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.response.CarbonCreditResponse;
import com.carbonx.marketcarbon.dto.response.CreditBatchLiteResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface MyCreditService {
    Page<CarbonCreditResponse> listMyCredits(CreditQuery query, Pageable pageable);
    Page<CreditBatchLiteResponse> listMyBatches(Long projectId, Integer vintageYear, Pageable pageable);
    CarbonCreditResponse getMyCreditById(Long creditId);
    CarbonCreditResponse getMyCreditByCode(String creditCode);
    List<CarbonCreditResponse> getMyCreditsByBatchId(Long batchId);

    List<CarbonCreditResponse> getMyRetirableCredits();
    CarbonCreditResponse retireCredit(Long creditId, BigDecimal quantity);

}
