package com.carbonx.marketcarbon.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PriceAnalyticsService {
    record PriceStats(BigDecimal avg, BigDecimal min, BigDecimal max) {}
    PriceStats statsForCompany(Long companyId, LocalDateTime from, LocalDateTime to);
    PriceStats statsForMarket(LocalDateTime from, LocalDateTime to);
}
