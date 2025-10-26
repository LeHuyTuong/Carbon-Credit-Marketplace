package com.carbonx.marketcarbon;


import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.dto.request.WalletTransactionRequest;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.model.WalletTransaction;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.WalletRepository;
import com.carbonx.marketcarbon.repository.WalletTransactionRepository;
import com.carbonx.marketcarbon.service.impl.WalletTransactionServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletTransactionServiceImplTest {

    @Mock
    private WalletTransactionRepository walletTransactionRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private WalletTransactionServiceImpl walletTransactionService;

    @Captor
    private ArgumentCaptor<WalletTransaction> transactionCaptor;

    private User user;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(5L);
        user.setEmail("wallet@example.com");

        wallet = new Wallet();
        wallet.setId(99L);
        wallet.setBalance(BigDecimal.valueOf(200));
        wallet.setUser(user);

        lenient().when(authentication.getName()).thenReturn(user.getEmail());
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        lenient().when(userRepository.findByEmail(user.getEmail())).thenReturn(user);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTransaction_withExplicitWallet_shouldUpdateBalanceAndPersistTransaction() {
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
        when(walletRepository.save(wallet)).thenReturn(wallet);
        when(walletTransactionRepository.save(any(WalletTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletTransactionRequest request = WalletTransactionRequest.builder()
                .wallet(wallet)
                .amount(BigDecimal.valueOf(50))
                .type(WalletTransactionType.ADD_MONEY)
                .description("Top up")
                .build();

        WalletTransaction transaction = walletTransactionService.createTransaction(request);

        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(250));

        verify(walletRepository).save(wallet);
        verify(walletTransactionRepository).save(transactionCaptor.capture());
        WalletTransaction savedTransaction = transactionCaptor.getValue();
        assertThat(savedTransaction.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(savedTransaction.getBalanceBefore()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(savedTransaction.getBalanceAfter()).isEqualByComparingTo(BigDecimal.valueOf(250));
        assertThat(transaction).isSameAs(savedTransaction);
    }

    @Test
    void createTransaction_shouldUseCurrentUserWalletWhenRequestWalletMissing() {
        when(walletRepository.findByUserId(user.getId())).thenReturn(wallet);
        when(walletRepository.save(wallet)).thenReturn(wallet);
        when(walletTransactionRepository.save(any(WalletTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletTransactionRequest request = WalletTransactionRequest.builder()
                .amount(BigDecimal.valueOf(150))
                .type(WalletTransactionType.WITHDRAWAL)
                .description("Withdraw")
                .build();

        WalletTransaction transaction = walletTransactionService.createTransaction(request);

        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(50));
        verify(walletTransactionRepository).save(transactionCaptor.capture());
        WalletTransaction savedTransaction = transactionCaptor.getValue();
        assertThat(savedTransaction.getWallet()).isSameAs(wallet);
        assertThat(transaction.getWallet()).isSameAs(wallet);
    }

    @Test
    void createTransaction_shouldRejectZeroAmount() {
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
        WalletTransactionRequest request = WalletTransactionRequest.builder()
                .wallet(wallet)
                .amount(BigDecimal.ZERO)
                .type(WalletTransactionType.ADD_MONEY)
                .build();

        assertThatThrownBy(() -> walletTransactionService.createTransaction(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different from zero");
    }



    @Test
    void getTransaction_shouldReturnEmptyListWhenWalletMissing() {
        when(walletRepository.findByUserId(user.getId())).thenReturn(null);

        List<WalletTransaction> result = walletTransactionService.getTransaction();

        assertThat(result).isEmpty();
        verify(walletTransactionRepository, never()).findByWalletOrderByCreatedAtDesc(any());
    }

    @Test
    void getTransaction_shouldReturnTransactionsForWallet() {
        when(walletRepository.findByUserId(user.getId())).thenReturn(wallet);
        WalletTransaction tx = new WalletTransaction();
        when(walletTransactionRepository.findByWalletOrderByCreatedAtDesc(wallet)).thenReturn(List.of(tx));

        List<WalletTransaction> result = walletTransactionService.getTransaction();

        assertThat(result).containsExactly(tx);
    }
}
