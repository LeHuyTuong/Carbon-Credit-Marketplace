package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.repository.MarketplaceListingRepository;
import com.carbonx.marketcarbon.scheduler.PriceUpdateScheduler;
import com.carbonx.marketcarbon.service.DynamicPricingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceUpdateSchedulerTest {

    @Mock
    private DynamicPricingService dynamicPricingService;

    @Mock
    private MarketplaceListingRepository marketplaceListingRepository;

    @InjectMocks
    private PriceUpdateScheduler scheduler;

    @Test
    void scheduledCron_ShouldDefaultToDaily8AmVietnamTime() throws NoSuchMethodException {
        Method method = PriceUpdateScheduler.class.getDeclaredMethod("updateMarketPrices");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).isEqualTo("${app.pricing.cron:0 0 8 * * *}");
        assertThat(scheduled.zone()).isEqualTo("Asia/Ho_Chi_Minh");
    }

    @Test
    void updateMarketPrices_ShouldRoundHalfUpAndUpdateRedis() {
        when(marketplaceListingRepository.getWeightedAveragePrice()).thenReturn(17.556);

        scheduler.updateMarketPrices();

        verify(dynamicPricingService).updateMarketPrice(new BigDecimal("17.56"));
        verify(dynamicPricingService).updateKwhPerCreditFactor(new BigDecimal("2500"));
    }

    @Test
    void updateMarketPrices_ShouldSkipWhenNoListingAvailable() {
        when(marketplaceListingRepository.getWeightedAveragePrice()).thenReturn(null);

        scheduler.updateMarketPrices();

        verifyNoInteractions(dynamicPricingService);
    }
}
