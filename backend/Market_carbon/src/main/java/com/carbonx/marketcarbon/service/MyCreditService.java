package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.request.RetireBatchRequest;
import com.carbonx.marketcarbon.dto.response.CarbonCreditResponse;
import com.carbonx.marketcarbon.dto.response.CreditBatchLiteResponse;
import com.carbonx.marketcarbon.dto.response.RetirableBatchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface MyCreditService {
    List<CarbonCreditResponse> listMyCredits(CreditQuery query);
    Page<CreditBatchLiteResponse> listMyBatches(Long projectId, Integer vintageYear, Pageable pageable);
    CarbonCreditResponse getMyCreditById(Long creditId);
    CarbonCreditResponse getMyCreditByCode(String creditCode);
    List<CarbonCreditResponse> getMyCreditsByBatchId(Long batchId);

    List<CarbonCreditResponse> getMyRetirableCredits();
    /**
     * Retire một TỔNG SỐ LƯỢNG từ một LÔ (batch),
     * tự động trừ dần từ các dòng credit con.
     */
    List<CarbonCreditResponse> retireCreditsFromBatch(RetireBatchRequest request);

    List<RetirableBatchResponse> getMyRetirableCreditsBatch();
}
