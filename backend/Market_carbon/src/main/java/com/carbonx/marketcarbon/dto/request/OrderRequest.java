package com.carbonx.marketcarbon.dto.request;

import com.carbonx.marketcarbon.common.OrderType;
import com.carbonx.marketcarbon.model.CarbonCredit;
import com.carbonx.marketcarbon.model.OrderItem;
import com.carbonx.marketcarbon.model.User;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

@Data
public class OrderRequest {

    private User user;

    @Enumerated(EnumType.STRING)
    private OrderItem orderItem;

    @Enumerated(EnumType.STRING)
    private OrderType  orderType;

    private CarbonCredit  carbonCredit;

    
}
