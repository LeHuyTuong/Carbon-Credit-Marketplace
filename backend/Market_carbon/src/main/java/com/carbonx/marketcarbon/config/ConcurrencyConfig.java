package com.carbonx.marketcarbon.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * Cấu hình toàn bộ executor cho các tác vụ bất đồng bộ.
 *
 * <p>Mỗi loại tác vụ có pool riêng để email (I/O-bound, SMTP chậm) không chiếm hết
 * queue của profit-sharing (tác vụ tài chính). Tất cả pool đều bounded + CallerRunsPolicy
 * để tạo backpressure thay vì âm thầm mất task, và copy MDC sang worker thread để log
 * vẫn giữ được trace id.</p>
 */
@Configuration
@EnableAsync
@Slf4j
public class ConcurrencyConfig implements AsyncConfigurer {

    public static final String PROFIT_SHARING_EXECUTOR = "profitSharingTaskExecutor";
    public static final String EMAIL_EXECUTOR = "emailTaskExecutor";

    /**
     * Pool cho profit-sharing: tác vụ nặng, tuần tự theo từng owner.
     */
    @Bean(PROFIT_SHARING_EXECUTOR)
    public ThreadPoolTaskExecutor profitSharingTaskExecutor() {
        return buildExecutor(10, 20, 500, "ProfitShare-");
    }

    /**
     * Pool riêng cho email: I/O-bound nên pool nhỏ, queue ngắn để tránh giữ task quá lâu.
     */
    @Bean(EMAIL_EXECUTOR)
    public ThreadPoolTaskExecutor emailTaskExecutor() {
        return buildExecutor(2, 5, 200, "Email-");
    }

    /**
     * Executor mặc định cho các {@code @Async} không đặt qualifier (notification helper).
     */
    @Bean("applicationTaskExecutor")
    public ThreadPoolTaskExecutor defaultAsyncTaskExecutor() {
        return buildExecutor(4, 8, 200, "Async-");
    }

    @Override
    public Executor getAsyncExecutor() {
        // Gọi qua @Configuration proxy để luôn trả về đúng singleton bean ở trên.
        return defaultAsyncTaskExecutor();
    }

    /**
     * Async void không thể ném lỗi về caller, nên chỉ log ERROR kèm method + kiểu tham số.
     * Không rethrow: ném lại chỉ tạo uncaught exception trên worker thread.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            String parameterTypes = Arrays.stream(method.getParameterTypes())
                    .map(Class::getTypeName)
                    .collect(Collectors.joining(", "));
            log.error("Uncaught async exception in {}({}): {}",
                    method.getName(), parameterTypes, ex.getMessage(), ex);
        };
    }

    private static ThreadPoolTaskExecutor buildExecutor(int corePoolSize,
                                                        int maxPoolSize,
                                                        int queueCapacity,
                                                        String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Số luồng cơ sở trong pool
        executor.setCorePoolSize(corePoolSize);
        // Số luồng tối đa
        executor.setMaxPoolSize(maxPoolSize);
        // Hàng đợi có giới hạn: không cho phép phình vô hạn khi downstream chậm
        executor.setQueueCapacity(queueCapacity);
        // Khi pool + queue đầy, caller tự chạy task để tạo backpressure
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // Tên tiền tố cho các luồng
        executor.setThreadNamePrefix(threadNamePrefix);
        // Tắt máy êm: chờ task đang chạy tối đa 60s
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        // Giữ MDC (trace id, user...) khi chuyển task sang worker thread
        executor.setTaskDecorator(new MdcTaskDecorator());
        // Không gọi initialize() ở đây: Spring gọi afterPropertiesSet() một lần khi tạo bean.
        // Gọi sớm sẽ tạo pool thứ nhất rồi bị thay bằng pool thứ hai (leak thread).
        return executor;
    }

    /**
     * Copy MDC của thread submit sang worker, sau đó clear để thread trong pool
     * không mang context của request trước sang request sau.
     */
    static class MdcTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    if (contextMap == null) {
                        MDC.clear();
                    } else {
                        MDC.setContextMap(contextMap);
                    }
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        }
    }
}
