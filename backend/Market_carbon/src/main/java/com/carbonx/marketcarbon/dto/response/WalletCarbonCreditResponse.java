package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.common.CreditStatus;
import jakarta.validation.Valid;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Valid
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletCarbonCreditResponse {
    Long creditId;
    String creditCode;
    BigDecimal ownedQuantity;
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
    LocalDate expirationDate;

    // Thông tin Batch giúp xác định rõ tín chỉ thuộc về công ty nào
    Long batchId;
    String batchCode;
    Long batchCompanyId;
    String batchCompanyName;
}
