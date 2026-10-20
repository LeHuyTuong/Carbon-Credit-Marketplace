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
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * Cấu hình toàn bộ executor cho các tác vụ bất đồng bộ.
 *
 * <p>Mỗi loại tác vụ có pool riêng để email (I/O-bound, SMTP chậm) không chiếm hết
 * queue của profit-sharing (tác vụ tài chính). Profit-sharing và pool mặc định dùng
 * CallerRunsPolicy để tạo backpressure; riêng pool email log-and-drop khi đầy vì email
 * chỉ là thông báo — chạy SMTP inline trên thread payout sẽ giữ khóa ví trong transaction.
 * Tất cả pool đều bounded và copy MDC sang worker thread để log vẫn giữ được trace id.</p>
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
        return buildExecutor(10, 20, 500, "ProfitShare-",
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * Pool riêng cho email: I/O-bound nên pool nhỏ, queue ngắn để tránh giữ task quá lâu.
     * Khi đầy thì log-and-drop: email chỉ là thông báo, không được chạy SMTP inline
     * trên thread payout đang giữ khóa ví.
     */
    @Bean(EMAIL_EXECUTOR)
    public ThreadPoolTaskExecutor emailTaskExecutor() {
        return buildExecutor(2, 5, 200, "Email-",
                new LogAndDropRejectedExecutionHandler(EMAIL_EXECUTOR));
    }

    /**
     * Executor mặc định cho các {@code @Async} không đặt qualifier (notification helper).
     */
    @Bean("applicationTaskExecutor")
    public ThreadPoolTaskExecutor defaultAsyncTaskExecutor() {
        return buildExecutor(4, 8, 200, "Async-",
                new ThreadPoolExecutor.CallerRunsPolicy());
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
                                                        String threadNamePrefix,
                                                        RejectedExecutionHandler rejectedExecutionHandler) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Số luồng cơ sở trong pool
        executor.setCorePoolSize(corePoolSize);
        // Số luồng tối đa
        executor.setMaxPoolSize(maxPoolSize);
        // Hàng đợi có giới hạn: không cho phép phình vô hạn khi downstream chậm
        executor.setQueueCapacity(queueCapacity);
        // Chính sách khi pool + queue đầy: backpressure hoặc drop tùy loại tác vụ
        executor.setRejectedExecutionHandler(rejectedExecutionHandler);
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
     * Chính sách cho pool email: khi đầy thì log ERROR rồi bỏ task, KHÔNG chạy inline.
     *
     * <p>Email chỉ là thông báo. Chạy nó trên thread caller có thể là thread payout đang
     * giữ {@code PESSIMISTIC_WRITE} trên ví trong một transaction {@code REQUIRES_NEW};
     * SMTP chậm sẽ kéo dài thời gian giữ khóa và block payout/withdrawal khác.</p>
     */
    static final class LogAndDropRejectedExecutionHandler implements RejectedExecutionHandler {

        private final String executorName;

        LogAndDropRejectedExecutionHandler(String executorName) {
            this.executorName = executorName;
        }

        @Override
        public void rejectedExecution(Runnable task, ThreadPoolExecutor executor) {
            log.error("{} rejected a task: pool size={}, active count={}, queue size={}. "
                            + "Dropping it because notifications must never run inline on the caller thread.",
                    executorName,
                    executor.getPoolSize(),
                    executor.getActiveCount(),
                    executor.getQueue().size());
        }
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
