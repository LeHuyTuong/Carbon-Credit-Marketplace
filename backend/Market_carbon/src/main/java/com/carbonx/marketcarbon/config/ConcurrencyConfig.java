package com.carbonx.marketcarbon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

@Configuration
@EnableAsync
public class ConcurrencyConfig {
    /**
     * Định nghĩa một ThreadPoolTaskExecutor
     * TaskExecutor này sẽ được sử dụng cho các tác vụ bất đồng bộ (async)
     * liên quan đến việc chia lợi nhuận.
     */
    @Bean("profitSharingTaskExecutor")
    public AsyncTaskExecutor profitSharingTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Số luồng cơ sở trong pool
        executor.setCorePoolSize(10);
        // Số luồng tối đa
        executor.setMaxPoolSize(20);
        // Kích thước hàng đợi
        executor.setQueueCapacity(500);
        // Tên tiền tố cho các luồng
        executor.setThreadNamePrefix("ProfitShare-");
        // Khởi tạo executor
        executor.initialize();
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }
}
