package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.dto.request.WalletTransactionRequest;
import com.carbonx.marketcarbon.exception.WalletException;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.WalletRepository;
import com.carbonx.marketcarbon.service.WalletTransactionService;
import com.carbonx.marketcarbon.service.impl.WalletServiceImpl;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WalletTransactionService walletTransactionService;

    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private WalletServiceImpl walletService;

    @Captor
    private ArgumentCaptor<WalletTransactionRequest> requestCaptor;

    private User user;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");

        wallet = new Wallet();
        wallet.setId(11L);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setUser(user);

        when(authentication.getName()).thenReturn(user.getEmail());
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(user);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addBalanceToWallet_shouldCreateTransactionAndReturnUpdatedWallet() throws WalletException {
        when(walletRepository.findByUserId(user.getId())).thenReturn(wallet);
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));

        doAnswer(invocation -> {
            WalletTransactionRequest request = invocation.getArgument(0);
            wallet.setBalance(wallet.getBalance().add(request.getAmount()));
            return null;
        }).when(walletTransactionService).createTransaction(any(WalletTransactionRequest.class));

        Wallet result = walletService.addBalanceToWallet(1000L);

        verify(walletTransactionService).createTransaction(requestCaptor.capture());
        WalletTransactionRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(capturedRequest.getType()).isEqualTo(WalletTransactionType.ADD_MONEY);
        assertThat(capturedRequest.getWallet()).isSameAs(wallet);

        assertThat(result).isSameAs(wallet);
        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    void addBalanceToWallet_shouldCreateWalletWhenMissing() throws WalletException {
        when(walletRepository.findByUserId(user.getId())).thenReturn(null);
        Wallet persistedWallet = new Wallet();
        persistedWallet.setId(55L);
        persistedWallet.setBalance(BigDecimal.ZERO);
        persistedWallet.setUser(user);

        when(walletRepository.save(any(Wallet.class))).thenReturn(persistedWallet);
        when(walletRepository.findById(55L)).thenReturn(Optional.of(persistedWallet));

        doAnswer(invocation -> null).when(walletTransactionService).createTransaction(any(WalletTransactionRequest.class));

        Wallet result = walletService.addBalanceToWallet(500L);

        verify(walletRepository).save(any(Wallet.class));
        assertThat(result).isSameAs(persistedWallet);
    }

    @Test
    void addBalanceToWallet_shouldRejectInvalidAmount() {
        assertThatThrownBy(() -> walletService.addBalanceToWallet(null))
                .isInstanceOf(WalletException.class)
                .hasMessageContaining("greater than zero");

        assertThatThrownBy(() -> walletService.addBalanceToWallet(0L))
                .isInstanceOf(WalletException.class);

        verifyNoInteractions(walletTransactionService);
    }

    @Test
    void getUserWallet_shouldReturnExistingWallet() throws WalletException {
        when(walletRepository.findByUserId(user.getId())).thenReturn(wallet);

        Wallet result = walletService.getUserWallet();

        assertThat(result).isSameAs(wallet);
        verify(walletRepository, never()).save(any());
    }

    @Test
    void getUserWallet_shouldGenerateNewWalletWhenMissing() throws WalletException {
        when(walletRepository.findByUserId(user.getId())).thenReturn(null);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
            Wallet saved = invocation.getArgument(0);
            saved.setId(77L);
            saved.setBalance(BigDecimal.ZERO);
            return saved;
        });

        Wallet result = walletService.getUserWallet();

        assertThat(result.getId()).isEqualTo(77L);
        assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
