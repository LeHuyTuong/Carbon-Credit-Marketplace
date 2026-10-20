package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.config.ConcurrencyConfig;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wiring test: without a Spring context, {@code getAsyncExecutor()} could accidentally create a
 * second pool. This boots a minimal context to prove the @Bean singleton is what @Async uses.
 */
class AsyncExecutorWiringTest {

    @Test
    void contextRegistersBothExecutorsAndUsesTheSingletonAsDefault() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(ConcurrencyConfig.class)) {

            ThreadPoolTaskExecutor profit = context.getBean("profitSharingTaskExecutor", ThreadPoolTaskExecutor.class);
            ThreadPoolTaskExecutor email = context.getBean("emailTaskExecutor", ThreadPoolTaskExecutor.class);
            ThreadPoolTaskExecutor defaultExecutor = context.getBean("applicationTaskExecutor", ThreadPoolTaskExecutor.class);

            assertThat(profit).isNotSameAs(email);
            assertThat(profit.getThreadNamePrefix()).isEqualTo("ProfitShare-");
            assertThat(email.getThreadNamePrefix()).isEqualTo("Email-");
            assertThat(defaultExecutor.getThreadNamePrefix()).isEqualTo("Async-");

            AsyncConfigurer configurer = context.getBean(AsyncConfigurer.class);
            assertThat(configurer.getAsyncUncaughtExceptionHandler()).isNotNull();
            assertThat(configurer.getAsyncExecutor())
                    .as("default @Async must use the same bounded singleton bean")
                    .isSameAs(defaultExecutor);
        }
    }
}
