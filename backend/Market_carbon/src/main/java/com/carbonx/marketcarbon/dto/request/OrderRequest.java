package com.carbonx.marketcarbon.dto.request;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderRequest {

    private Long buyerCompanyId;

    private Long listingId;

    private BigDecimal quantity;
}
