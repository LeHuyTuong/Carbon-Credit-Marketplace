package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.config.ConcurrencyConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit-level contract test for the async executors. Instantiates {@link ConcurrencyConfig}
 * directly (no Spring context) so a broken pool policy is caught without booting the app.
 */
class AsyncExecutorConfigTest {

    private final ConcurrencyConfig config = new ConcurrencyConfig();
    private final List<ThreadPoolTaskExecutor> startedExecutors = new ArrayList<>();

    @AfterEach
    void shutdownExecutors() {
        startedExecutors.forEach(ThreadPoolTaskExecutor::shutdown);
    }

    @Test
    void profitAndEmailExecutorsAreDistinctBeansWithExpectedPools() throws Exception {
        ThreadPoolTaskExecutor profit = executorBean("profitSharingTaskExecutor");
        ThreadPoolTaskExecutor email = executorBean("emailTaskExecutor");

        assertThat(profit)
                .as("email traffic must not share the profit-sharing pool")
                .isNotSameAs(email);

        assertThat(profit.getCorePoolSize()).isEqualTo(10);
        assertThat(profit.getMaxPoolSize()).isEqualTo(20);
        assertThat(profit.getQueueCapacity()).isEqualTo(500);
        assertThat(profit.getThreadNamePrefix()).isEqualTo("ProfitShare-");

        assertThat(email.getCorePoolSize()).isEqualTo(2);
        assertThat(email.getMaxPoolSize()).isEqualTo(5);
        assertThat(email.getQueueCapacity()).isEqualTo(200);
        assertThat(email.getThreadNamePrefix()).isEqualTo("Email-");
        assertThat(email.getMaxPoolSize()).isLessThan(profit.getMaxPoolSize());
    }

    @Test
    void namedExecutorsUseCallerRunsPolicy() throws Exception {
        for (String beanName : List.of("profitSharingTaskExecutor", "emailTaskExecutor")) {
            ThreadPoolTaskExecutor executor = executorBean(beanName);
            assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler())
                    .as("%s must apply backpressure instead of dropping tasks", beanName)
                    .isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);
        }
    }

    @Test
    void namedExecutorsWaitForTasksAndAwaitSixtySecondsOnShutdown() throws Exception {
        for (String beanName : List.of("profitSharingTaskExecutor", "emailTaskExecutor")) {
            ThreadPoolTaskExecutor executor = executorBean(beanName);
            assertThat(readField(executor, "waitForTasksToCompleteOnShutdown"))
                    .as("%s waitForTasksToCompleteOnShutdown", beanName)
                    .isEqualTo(true);
            assertThat(readField(executor, "awaitTerminationMillis"))
                    .as("%s await termination millis", beanName)
                    .isEqualTo(60_000L);
        }
    }

    @Test
    void tasksRunOnThreadsWithExpectedPrefixes() throws Exception {
        assertThat(runAndCaptureThreadName(executorBean("profitSharingTaskExecutor")))
                .startsWith("ProfitShare-");
        assertThat(runAndCaptureThreadName(executorBean("emailTaskExecutor")))
                .startsWith("Email-");
    }

    @Test
    void defaultExecutorIsBoundedAndUsesAsyncPrefix() throws Exception {
        ThreadPoolTaskExecutor defaultExecutor = defaultAsyncExecutor();

        assertThat(defaultExecutor.getCorePoolSize()).isEqualTo(4);
        assertThat(defaultExecutor.getMaxPoolSize()).isEqualTo(8);
        assertThat(defaultExecutor.getQueueCapacity()).isEqualTo(200);
        assertThat(defaultExecutor.getThreadNamePrefix()).isEqualTo("Async-");
        assertThat(defaultExecutor.getThreadPoolExecutor().getRejectedExecutionHandler())
                .isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);
        assertThat(readField(defaultExecutor, "waitForTasksToCompleteOnShutdown")).isEqualTo(true);
        assertThat(readField(defaultExecutor, "awaitTerminationMillis")).isEqualTo(60_000L);
        assertThat(runAndCaptureThreadName(defaultExecutor)).startsWith("Async-");
    }

    @Test
    void asyncUncaughtExceptionHandlerIsConfigured() throws Exception {
        assertThat(AsyncConfigurer.class.isAssignableFrom(ConcurrencyConfig.class))
                .as("ConcurrencyConfig must supply the default async executor")
                .isTrue();

        Method handlerMethod = ConcurrencyConfig.class.getMethod("getAsyncUncaughtExceptionHandler");
        AsyncUncaughtExceptionHandler handler = (AsyncUncaughtExceptionHandler) handlerMethod.invoke(config);
        assertThat(handler).isNotNull();

        Method method = ConcurrencyConfig.class.getDeclaredMethod("profitSharingTaskExecutor");
        assertThatCode(() -> handler.handleUncaughtException(
                new IllegalStateException("boom"), method, new Object[]{"arg"}))
                .as("async void cannot propagate; the handler must only log")
                .doesNotThrowAnyException();
    }

    @Test
    void taskDecoratorCopiesMdcIntoWorkerAndClearsItAfterwards() throws Exception {
        ThreadPoolTaskExecutor profit = executorBean("profitSharingTaskExecutor");
        TaskDecorator decorator = (TaskDecorator) readField(profit, "taskDecorator");
        assertThat(decorator).as("MDC must survive the hand-off to a worker thread").isNotNull();

        AtomicReference<String> traceInsideWorker = new AtomicReference<>();
        MDC.put("traceId", "trace-for-async-test");
        Runnable decorated = decorator.decorate(() -> traceInsideWorker.set(MDC.get("traceId")));
        MDC.clear(); // simulate the worker thread starting with an empty context

        decorated.run();

        assertThat(traceInsideWorker.get()).isEqualTo("trace-for-async-test");
        assertThat(MDC.getCopyOfContextMap())
                .as("MDC must be cleared so the pooled thread does not leak context")
                .satisfiesAnyOf(
                        map -> assertThat(map).isNull(),
                        map -> assertThat(map).isEmpty());
    }

    private ThreadPoolTaskExecutor executorBean(String beanName) throws Exception {
        Method method = ConcurrencyConfig.class.getMethod(beanName);
        Bean bean = method.getAnnotation(Bean.class);
        assertThat(bean).as("@Bean method %s", beanName).isNotNull();
        assertThat(bean.value()).as("@Bean name for %s", beanName).containsExactly(beanName);

        Object value = method.invoke(config);
        assertThat(value).isInstanceOf(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) value;
        // In a Spring context afterPropertiesSet() does this; the test constructs the
        // config directly, so initialize explicitly before inspecting the pool.
        executor.initialize();
        startedExecutors.add(executor);
        return executor;
    }

    private ThreadPoolTaskExecutor defaultAsyncExecutor() throws Exception {
        assertThat(AsyncConfigurer.class.isAssignableFrom(ConcurrencyConfig.class))
                .as("default @Async methods need an explicit bounded executor")
                .isTrue();
        Method method = ConcurrencyConfig.class.getMethod("getAsyncExecutor");
        Object value = method.invoke(config);
        assertThat(value).isInstanceOf(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) value;
        executor.initialize();
        startedExecutors.add(executor);
        return executor;
    }

    private String runAndCaptureThreadName(ThreadPoolTaskExecutor executor) throws InterruptedException {
        AtomicReference<String> threadName = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        executor.execute(() -> {
            threadName.set(Thread.currentThread().getName());
            done.countDown();
        });
        assertThat(done.await(5, TimeUnit.SECONDS)).as("task should run within 5s").isTrue();
        return threadName.get();
    }

    private static Object readField(Object target, String fieldName) throws Exception {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
