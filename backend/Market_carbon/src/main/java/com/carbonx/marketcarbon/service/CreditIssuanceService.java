package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.response.CreditBatchResponse;
import com.carbonx.marketcarbon.model.CarbonCredit;
import com.carbonx.marketcarbon.model.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface CreditIssuanceService {
    CreditBatchResponse issueForReport(Long reportId);
    CreditBatchResponse issueForReport(Long reportId, Integer approvedCredits);
    Page<CreditBatchResponse> listAllBatches(Pageable pageable);
    CreditBatchResponse getBatchById(Long batchId);

    CarbonCredit issueTradeCredit(CarbonCredit sourceCredit,
                                  Company buyerCompany,
                                  BigDecimal quantity,
                                  BigDecimal pricePerUnit,
                                  String issuedBy);

    public CreditBatchResponse previewIssueForReport(Long reportId);
}
