package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.common.Status;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.model.Withdrawal;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.WalletRepository;
import com.carbonx.marketcarbon.repository.WithdrawalRepository;
import com.carbonx.marketcarbon.service.impl.WithdrawalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WithdrawalRepository withdrawalRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionService walletTransactionService;

    @InjectMocks
    private WithdrawalServiceImpl withdrawalService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private User user;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");

        wallet = new Wallet();
        wallet.setId(11L);
        wallet.setBalance(BigDecimal.valueOf(1000));
        wallet.setUser(user);

        when(authentication.getName()).thenReturn(user.getEmail());
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(user);
    }

    @Test
    void testRequestWithdrawal_Success() {
        when(walletRepository.findByUserId(user.getId())).thenReturn(wallet);
        when(withdrawalRepository.save(any(Withdrawal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        long amount = 500L;
        Withdrawal withdrawal = withdrawalService.requestWithdrawal(amount);

        assertThat(withdrawal).isNotNull();
        assertThat(withdrawal.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(amount));
        assertThat(withdrawal.getStatus()).isEqualTo(Status.PENDING);
        assertThat(withdrawal.getUser()).isEqualTo(user);
    }

    @Test
    void testRequestWithdrawal_InsufficientFunds() {
        when(walletRepository.findByUserId(user.getId())).thenReturn(wallet);

        long amount = 2000L;

        assertThatThrownBy(() -> withdrawalService.requestWithdrawal(amount))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WALLET_NOT_ENOUGH_MONEY);
    }

    @Test
    void testRequestWithdrawal_InvalidAmount() {
        long amount = 5L;

        assertThatThrownBy(() -> withdrawalService.requestWithdrawal(amount))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WITHDRAWAL_MONEY_INVALID_AMOUNT);
    }
}
