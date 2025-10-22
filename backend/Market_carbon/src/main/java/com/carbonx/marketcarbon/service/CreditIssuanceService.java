package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.response.CreditBatchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CreditIssuanceService {
    CreditBatchResponse issueForReport(Long reportId);
    Page<CreditBatchResponse> listAllBatches(Pageable pageable);
    CreditBatchResponse getBatchById(Long batchId);

}
