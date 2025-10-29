package com.carbonx.marketcarbon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class MarketplaceListingResponse {

    private Long listingId;

    private BigDecimal quantity;

    private BigDecimal availableQuantity;

    private BigDecimal originalQuantity;

    private BigDecimal soldQuantity;

    private BigDecimal pricePerCredit;

    private String sellerCompanyName;

    private Long projectId;

    private String projectTitle;

    private LocalDate expiresAt;

    private String logo;

}
