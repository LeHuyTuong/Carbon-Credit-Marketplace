package com.carbonx.marketcarbon.service;


import com.carbonx.marketcarbon.common.ProjectStatus;
import com.carbonx.marketcarbon.dto.request.CreditIssuanceRequest;
import com.carbonx.marketcarbon.dto.response.ProjectResponse;
import com.carbonx.marketcarbon.exception.WalletException;
import com.carbonx.marketcarbon.model.CarbonCredit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;


public interface CarbonCreditService {
    BigDecimal calculateCarbonCredit(BigDecimal chargingEnergy);

    CarbonCredit issueCredits(CreditIssuanceRequest request);

    ProjectResponse finalApprove(Long projectId, ProjectStatus status);

    Page<ProjectResponse> adminListReviewedByCva(Long cvaId, Pageable pageable);

    Page<ProjectResponse> adminInbox(Pageable pageable);

    CarbonCredit approveCarbonCredit(Long carbonCreditId) throws WalletException;
}
