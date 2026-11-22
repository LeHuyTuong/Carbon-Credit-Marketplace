package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.common.ApplicationStatus;
import com.carbonx.marketcarbon.common.CreditStatus;
import com.carbonx.marketcarbon.common.OrderStatus;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainStateMachineTest {

    @Test
    void applicationStatusAllowsReviewRevisionAndFinalApprovalFlow() {
        assertThat(ApplicationStatus.SUBMITTED.canTransitionTo(ApplicationStatus.UNDER_REVIEW)).isTrue();
        assertThat(ApplicationStatus.UNDER_REVIEW.canTransitionTo(ApplicationStatus.NEEDS_REVISION)).isTrue();
        assertThat(ApplicationStatus.NEEDS_REVISION.canTransitionTo(ApplicationStatus.UNDER_REVIEW)).isTrue();
        assertThat(ApplicationStatus.CVA_APPROVED.canTransitionTo(ApplicationStatus.ADMIN_APPROVED)).isTrue();
        assertThat(ApplicationStatus.CVA_APPROVED.canTransitionTo(ApplicationStatus.ADMIN_REJECTED)).isTrue();

        assertThatCode(() -> ApplicationStatus.UNDER_REVIEW
                .requireTransitionTo(ApplicationStatus.NEEDS_REVISION)).doesNotThrowAnyException();
    }

    @Test
    void applicationStatusLocksFinalStatesAndForbidsJumps() {
        assertThat(ApplicationStatus.ADMIN_APPROVED.canTransitionTo(ApplicationStatus.UNDER_REVIEW)).isFalse();
        assertThat(ApplicationStatus.ADMIN_REJECTED.canTransitionTo(ApplicationStatus.NEEDS_REVISION)).isFalse();
        assertThat(ApplicationStatus.SUBMITTED.canTransitionTo(ApplicationStatus.ADMIN_APPROVED)).isFalse();
        assertThat(ApplicationStatus.UNDER_REVIEW.canTransitionTo(null)).isFalse();

        assertThatThrownBy(() -> ApplicationStatus.ADMIN_APPROVED
                .requireTransitionTo(ApplicationStatus.UNDER_REVIEW))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    void orderStatusAllowsPendingToTerminalOutcomes() {
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.SUCCESS)).isTrue();
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.ERROR)).isTrue();

        assertThatCode(() -> OrderStatus.PENDING.requireTransitionTo(OrderStatus.SUCCESS))
                .doesNotThrowAnyException();
    }

    @Test
    void orderStatusLocksSuccessAndCancelled() {
        assertThat(OrderStatus.SUCCESS.canTransitionTo(OrderStatus.PENDING)).isFalse();
        assertThat(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.SUCCESS)).isFalse();
        assertThat(OrderStatus.SUCCESS.canTransitionTo(null)).isFalse();

        assertThatThrownBy(() -> OrderStatus.CANCELLED.requireTransitionTo(OrderStatus.PENDING))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    void creditStatusAllowsTransitionsBeforeRetirementOrExpiry() {
        assertThat(CreditStatus.AVAILABLE.canTransitionTo(CreditStatus.ISSUED)).isTrue();
        assertThat(CreditStatus.ISSUED.canTransitionTo(CreditStatus.TRADED)).isTrue();
        assertThat(CreditStatus.LISTED.canTransitionTo(CreditStatus.RETIRED)).isTrue();

        assertThatCode(() -> CreditStatus.ISSUED.requireTransitionTo(CreditStatus.RETIRED))
                .doesNotThrowAnyException();
    }

    @Test
    void creditStatusLocksRetiredAndExpired() {
        assertThat(CreditStatus.RETIRED.canTransitionTo(CreditStatus.AVAILABLE)).isFalse();
        assertThat(CreditStatus.EXPIRED.canTransitionTo(CreditStatus.SOLD)).isFalse();
        assertThat(CreditStatus.RETIRED.canTransitionTo(CreditStatus.RETIRED)).isFalse();

        assertThatThrownBy(() -> CreditStatus.RETIRED.requireTransitionTo(CreditStatus.AVAILABLE))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    void keepsAllLegacyEnumConstants() {
        assertThat(ApplicationStatus.values()).hasSize(7);
        assertThat(OrderStatus.values()).hasSize(6);
        assertThat(CreditStatus.values()).hasSize(9);
    }
}
