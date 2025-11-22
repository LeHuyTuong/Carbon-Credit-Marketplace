package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.repository.OrderStatsRepository;
import com.carbonx.marketcarbon.service.PriceAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PriceAnalyticsServiceImpl implements PriceAnalyticsService {

    private final OrderStatsRepository orderStatsRepository;

    @Override
    public PriceStats statsForCompany(Long companyId, LocalDateTime from, LocalDateTime to) {
        BigDecimal min = orderStatsRepository.minPrice(companyId, from, to);
        BigDecimal max = orderStatsRepository.maxPrice(companyId, from, to);
        BigDecimal avg = orderStatsRepository.avgPrice(companyId, from, to);

        return new PriceStats(
                zeroToNull(avg),
                zeroToNull(min),
                zeroToNull(max)
        );
    }

    @Override
    public PriceStats statsForMarket(LocalDateTime from, LocalDateTime to) {
        BigDecimal min = orderStatsRepository.minPriceMarket(from, to);
        BigDecimal max = orderStatsRepository.maxPriceMarket(from, to);
        BigDecimal avg = orderStatsRepository.avgPriceMarket(from, to);
        return new PriceStats(avg, min, max);
    }

    private static BigDecimal zeroToNull(BigDecimal v) {
        if (v == null) return null;
        if (BigDecimal.ZERO.compareTo(v) == 0) return null;
        return v;
    }
}
