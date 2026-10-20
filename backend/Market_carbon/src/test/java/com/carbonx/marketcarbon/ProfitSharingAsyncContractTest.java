package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.dto.request.ProfitSharingRequest;
import com.carbonx.marketcarbon.service.ProfitSharingService;
import com.carbonx.marketcarbon.service.impl.ProfitSharingServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract guards, not behaviour tests: the async payout path cannot be exercised without a
 * full DB, so these reflection/source checks pin the invariants that caused the audit findings
 * (A5 caller identity, A6 self-proxy, A2/A3 executor qualifier). They fail fast if a refactor
 * reintroduces a SecurityContext/ApplicationContext dependency or drops the qualifier.
 */
class ProfitSharingAsyncContractTest {

    private static final Path SERVICE_SOURCE = Path.of(
            "src/main/java/com/carbonx/marketcarbon/service/impl/ProfitSharingServiceImpl.java");

    @Test
    void shareCompanyProfitTakesCallerIdAndTargetsProfitSharingExecutor() throws Exception {
        Method method = ProfitSharingServiceImpl.class.getMethod(
                "shareCompanyProfit", Long.class, ProfitSharingRequest.class);

        assertThat(method.getReturnType()).as("void keeps callers returning 200 started").isEqualTo(void.class);
        assertThat(method.getParameterTypes()).containsExactly(Long.class, ProfitSharingRequest.class);

        Async async = method.getAnnotation(Async.class);
        assertThat(async).as("implementation must keep @Async").isNotNull();
        assertThat(async.value()).isEqualTo("profitSharingTaskExecutor");
    }

    @Test
    void interfaceDoesNotDeclareAsyncSoTheQualifierHasOneSourceOfTruth() throws Exception {
        Method method = ProfitSharingService.class.getMethod(
                "shareCompanyProfit", Long.class, ProfitSharingRequest.class);

        assertThat(method.getParameterTypes()).containsExactly(Long.class, ProfitSharingRequest.class);
        assertThat(method.getAnnotation(Async.class))
                .as("the interface must not carry @Async; the implementation owns the qualifier")
                .isNull();
    }

    @Test
    void serviceHasNoApplicationContextSelfProxy() {
        assertThat(Arrays.stream(ProfitSharingServiceImpl.class.getDeclaredFields())
                .map(Field::getType))
                .as("self-proxy via ApplicationContext must be gone")
                .doesNotContain(ApplicationContext.class);

        String source = readServiceSource();
        assertThat(source).doesNotContain("ApplicationContext");
        assertThat(source).doesNotContain("getBean(");
    }

    @Test
    void serviceDoesNotReadSecurityContextInsideAsyncThread() {
        assertThat(readServiceSource())
                .as("the caller must be resolved in the controller and passed by id")
                .doesNotContain("SecurityContextHolder");
    }

    @Test
    void payoutHelpersArePrivateAndRunThroughTransactionTemplate() throws Exception {
        for (String methodName : List.of(
                "createDistributionEvent",
                "processPayoutForOwner",
                "saveFailedDetail",
                "updateReportsToPaidOut")) {

            List<Method> matches = Arrays.stream(ProfitSharingServiceImpl.class.getDeclaredMethods())
                    .filter(method -> method.getName().equals(methodName))
                    .toList();

            assertThat(matches).as("%s must still exist exactly once", methodName).hasSize(1);
            Method method = matches.get(0);
            assertThat(Modifier.isPrivate(method.getModifiers()))
                    .as("%s must be private because it is invoked via 'this'", methodName)
                    .isTrue();
            assertThat(method.getAnnotation(Transactional.class))
                    .as("%s must not pretend @Transactional works on a self-invocation", methodName)
                    .isNull();
        }

        assertThat(Arrays.stream(ProfitSharingServiceImpl.class.getDeclaredFields())
                .map(Field::getType))
                .as("programmatic transactions need the platform manager")
                .contains(PlatformTransactionManager.class);

        String source = readServiceSource();
        assertThat(source).contains("TransactionTemplate");
        assertThat(source).contains("PROPAGATION_REQUIRES_NEW");
    }

    private static String readServiceSource() {
        try {
            return Files.readString(SERVICE_SOURCE, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read " + SERVICE_SOURCE.toAbsolutePath(), e);
        }
    }
}
