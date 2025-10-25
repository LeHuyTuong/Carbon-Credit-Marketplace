package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.common.CreditStatus;
import jakarta.validation.Valid;
import lombok.Builder;

import java.math.BigDecimal;

@Valid
@Builder
public class WalletCarbonCreditResponse {
    Long creditId;
    String creditCode;
    BigDecimal availableQuantity;
    BigDecimal listedQuantity;
    CreditStatus status;
    Long sellerCompanyId;
    String sellerCompanyName;
    Long sourceCreditId;
    String sourceCreditCode;
    Long originCreditId;
    String originCreditCode;
    Long originCompanyId;
    String originCompanyName;
}
