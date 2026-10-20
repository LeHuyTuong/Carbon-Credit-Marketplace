package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.service.impl.EmailServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Async;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract guard: every {@code @Async} method on {@link EmailServiceImpl} must target
 * the dedicated email executor, otherwise SMTP blocking can exhaust the profit-sharing pool.
 */
class EmailAsyncQualifierTest {

    @Test
    void everyAsyncMethodOnEmailServiceImplUsesEmailTaskExecutor() {
        Method[] asyncMethods = Arrays.stream(EmailServiceImpl.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Async.class))
                .toArray(Method[]::new);

        assertThat(asyncMethods)
                .as("EmailServiceImpl is expected to expose async email methods")
                .isNotEmpty();

        assertThat(asyncMethods).allSatisfy(method -> {
            Async async = method.getAnnotation(Async.class);
            assertThat(async.value())
                    .as("%s must run on emailTaskExecutor", method.getName())
                    .isEqualTo("emailTaskExecutor");
        });
    }

    @Test
    void sendEmailIsNotUnqualified() throws Exception {
        Method sendEmail = EmailServiceImpl.class.getDeclaredMethod(
                "sendEmail", String.class, String.class, List.class);
        Async async = sendEmail.getAnnotation(Async.class);

        assertThat(async).as("sendEmail must remain async").isNotNull();
        assertThat(async.value())
                .as("bare @Async would fall back to the default executor")
                .isNotBlank()
                .isEqualTo("emailTaskExecutor");
    }

    @Test
    void payoutNotificationsAlsoUseEmailTaskExecutor() throws Exception {
        for (String methodName : List.of("sendPayoutSuccessToOwner", "sendDistributionSummaryToCompany")) {
            Method method = Arrays.stream(EmailServiceImpl.class.getDeclaredMethods())
                    .filter(candidate -> candidate.getName().equals(methodName))
                    .findFirst()
                    .orElseThrow();
            Async async = method.getAnnotation(Async.class);

            assertThat(async).as("%s must stay async", methodName).isNotNull();
            assertThat(async.value()).isEqualTo("emailTaskExecutor");
        }
    }
}
