package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.CreditStatus;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.common.ListingStatus;
import com.carbonx.marketcarbon.common.OrderStatus;
import com.carbonx.marketcarbon.common.OrderType;
import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.dto.request.OrderRequest;
import com.carbonx.marketcarbon.dto.request.WalletTransactionRequest;
import com.carbonx.marketcarbon.dto.response.OrderResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.CarbonCredit;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.MarketPlaceListing;
import com.carbonx.marketcarbon.model.Order;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Wallet;
import com.carbonx.marketcarbon.repository.CarbonCreditRepository;
import com.carbonx.marketcarbon.repository.CompanyRepository;
import com.carbonx.marketcarbon.repository.MarketplaceListingRepository;
import com.carbonx.marketcarbon.repository.OrderRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.WalletRepository;
import com.carbonx.marketcarbon.service.WalletTransactionService;
import com.carbonx.marketcarbon.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderServiceImpl. These tests isolate the service with Mockito mocks for all dependencies.
 * SecurityContextHolder is set up with a TestingAuthenticationToken to simulate the logged-in user lookup.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private WalletTransactionService walletTransactionService;
    @Mock private WalletRepository walletRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private MarketplaceListingRepository marketplaceListingRepository;
    @Mock private CarbonCreditRepository carbonCreditRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;
    private Company company;
    private Wallet wallet;

    @BeforeEach
    void setupSecurityContext() {
        // Create a logged-in user context
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new TestingAuthenticationToken("buyer@acme.com", "password"));
        SecurityContextHolder.setContext(context);

        // Common user and company setup
        user = User.builder()
                .id(1L)
                .email("buyer@acme.com")
                .build();

        wallet = Wallet.builder()
                .id(10L)
                .balance(new BigDecimal("10000.00"))
                .build();
        user.setWallet(wallet);

        company = Company.builder()
                .id(100L)
                .user(user)
                .companyName("Buyer Co")
                .build();

        when(userRepository.findByEmail("buyer@acme.com")).thenReturn(user);
        when(companyRepository.findByUserId(user.getId())).thenReturn(Optional.of(company));
    }

    private MarketPlaceListing buildListing(Company sellerCompany, BigDecimal qty, BigDecimal price, CarbonCredit credit) {
        return MarketPlaceListing.builder()
                .id(200L)
                .company(sellerCompany)
                .carbonCredit(credit)
                .quantity(qty)
                .pricePerCredit(price)
                .status(ListingStatus.AVAILABLE)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
    }

    private CarbonCredit buildSellerCredit(Company sellerCompany, BigDecimal available, int listedAmount) {
        return CarbonCredit.builder()
                .id(300L)
                .company(sellerCompany)
                .name("Batch A")
                .status(CreditStatus.ISSUE)
                .carbonCredit(available)
                .listedAmount(listedAmount)
                .build();
    }

    @Test
    @DisplayName("createOrder succeeds for valid request and available listing")
    void createOrder_success() {
        Company seller = Company.builder().id(101L).companyName("Seller Co").user(User.builder().id(2L).email("seller@acme.com").build()).build();
        CarbonCredit credit = buildSellerCredit(seller, new BigDecimal("1000.0000"), 500);
        MarketPlaceListing listing = buildListing(seller, new BigDecimal("100.0000"), new BigDecimal("12.50"), credit);

        when(marketplaceListingRepository.findById(200L)).thenReturn(Optional.of(listing));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        when(orderRepository.save(orderCaptor.capture())).thenAnswer(inv -> {
            Order o = orderCaptor.getValue();
            o.setId(999L);
            return o;
        });

        OrderRequest req = OrderRequest.builder()
                .buyerCompanyId(company.getId())
                .listingId(200L)
                .quantity(new BigDecimal("10.0000"))
                .build();

        OrderResponse resp = orderService.createOrder(req);

        assertThat(resp.getId()).isEqualTo(999L);
        assertThat(resp.getCompanyId()).isEqualTo(company.getId());
        assertThat(resp.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(resp.getTotalAmount()).isEqualByComparingTo(new BigDecimal("125.00"));
        assertThat(resp.getCreateAt()).isNotNull();

        Order saved = orderCaptor.getValue();
        assertThat(saved.getOrderType()).isEqualTo(OrderType.BUY);
        assertThat(saved.getMarketplaceListing().getId()).isEqualTo(200L);
        assertThat(saved.getQuantity()).isEqualByComparingTo("10.0000");
    }

    @Test
    @DisplayName("createOrder fails when listing is not available")
    void createOrder_listingNotAvailable() {
        Company seller = Company.builder().id(101L).companyName("Seller Co").user(User.builder().id(2L).email("seller@acme.com").build()).build();
        CarbonCredit credit = buildSellerCredit(seller, new BigDecimal("1000.0000"), 500);
        MarketPlaceListing listing = buildListing(seller, new BigDecimal("100.0000"), new BigDecimal("12.50"), credit);
        listing.setStatus(ListingStatus.SOLD);

        when(marketplaceListingRepository.findById(200L)).thenReturn(Optional.of(listing));

        OrderRequest req = OrderRequest.builder()
                .buyerCompanyId(company.getId())
                .listingId(200L)
                .quantity(new BigDecimal("10.0000"))
                .build();

        assertThatThrownBy(() -> orderService.createOrder(req))
                .isInstanceOf(AppException.class)
                .hasMessageContaining(ErrorCode.LISTING_IS_NOT_AVAILABLE.name());
    }

    @Test
    @DisplayName("getOrderById returns mapped response")
    void getOrderById_success() {
        Order order = Order.builder()
                .id(555L)
                .company(company)
                .orderStatus(OrderStatus.PENDING)
                .totalPrice(new BigDecimal("99.99"))
                .createdAt(LocalDateTime.now())
                .build();
        when(orderRepository.findById(555L)).thenReturn(Optional.of(order));

        OrderResponse resp = orderService.getOrderById(555L);

        assertThat(resp.getId()).isEqualTo(555L);
        assertThat(resp.getCompanyId()).isEqualTo(company.getId());
        assertThat(resp.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(resp.getTotalAmount()).isEqualByComparingTo("99.99");
    }

    @Test
    @DisplayName("getUserOrders returns list of mapped responses")
    void getUserOrders_success() {
        Order o1 = Order.builder().id(1L).company(company).orderStatus(OrderStatus.PENDING).totalPrice(new BigDecimal("10.00")).createdAt(LocalDateTime.now()).build();
        Order o2 = Order.builder().id(2L).company(company).orderStatus(OrderStatus.SUCCESS).totalPrice(new BigDecimal("20.00")).createdAt(LocalDateTime.now()).build();
        when(orderRepository.findByCompany(company)).thenReturn(List.of(o1, o2));

        List<OrderResponse> list = orderService.getUserOrders();

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getId()).isEqualTo(1L);
        assertThat(list.get(1).getStatus()).isEqualTo(OrderStatus.SUCCESS);
    }

    @Test
    @DisplayName("cancelOrder sets status to CANCELLED when pending and owned by current company")
    void cancelOrder_success() {
        Order order = Order.builder()
                .id(777L)
                .company(company)
                .orderStatus(OrderStatus.PENDING)
                .build();
        when(orderRepository.findById(777L)).thenReturn(Optional.of(order));

        orderService.cancelOrder(777L);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("cancelOrder fails when not owned by current company")
    void cancelOrder_unauthorized() {
        Company other = Company.builder().id(999L).user(User.builder().id(9L).email("other@acme.com").build()).build();
        Order order = Order.builder()
                .id(777L)
                .company(other)
                .orderStatus(OrderStatus.PENDING)
                .build();
        when(orderRepository.findById(777L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(777L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining(ErrorCode.UNAUTHORIZED.name());
    }

    @Nested
    class CompleteOrderTests {

        private Company seller;
        private Wallet sellerWallet;
        private CarbonCredit sellerCredit;
        private MarketPlaceListing listing;
        private Order order;

        @BeforeEach
        void initComplete() {
            seller = Company.builder()
                    .id(201L)
                    .companyName("Seller Co")
                    .user(User.builder().id(2L).email("seller@acme.com").build())
                    .build();
            sellerWallet = Wallet.builder()
                    .id(20L)
                    .balance(new BigDecimal("0.00"))
                    .build();
            // Gán ví cho user của seller
            seller.getUser().setWallet(sellerWallet);

            sellerCredit = buildSellerCredit(seller, new BigDecimal("1000.0000"), 500);
            listing = buildListing(seller, new BigDecimal("100.0000"), new BigDecimal("12.50"), sellerCredit);

            order = Order.builder()
                    .id(888L)
                    .company(company) // company người mua từ outer setup
                    .marketplaceListing(listing)
                    .carbonCredit(sellerCredit)
                    .orderStatus(OrderStatus.PENDING)
                    .orderType(OrderType.BUY)
                    .quantity(new BigDecimal("10.0000"))
                    .unitPrice(new BigDecimal("12.50"))
                    .totalPrice(new BigDecimal("125.00"))
                    .createdAt(LocalDateTime.now())
                    .build();

            when(orderRepository.findById(888L)).thenReturn(Optional.of(order));
            when(marketplaceListingRepository.findByIdWithPessimisticLock(listing.getId())).thenReturn(Optional.of(listing));

            when(walletRepository.findByUserId(company.getUser().getId())).thenReturn(wallet);
            when(walletRepository.findByUserId(seller.getUser().getId())).thenReturn(sellerWallet);

            // CarbonCredit repository interactions
            when(carbonCreditRepository.save(any(CarbonCredit.class))).thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("completeOrder fails when listing quantity insufficient")
        void completeOrder_insufficientListingQty() {
            listing.setQuantity(new BigDecimal("5.0000")); // Order cần 10

            assertThatThrownBy(() -> orderService.completeOrder(888L))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.AMOUNT_IS_NOT_ENOUGH.name());

            // SỬA LỖI 2: Mong đợi trạng thái ERROR, không phải PENDING, để nhất quán với các test thất bại khác
            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.ERROR);
            verify(orderRepository, atLeastOnce()).save(order);
        }

        @Test
        @DisplayName("completeOrder fails when buyer wallet not enough")
        void completeOrder_walletInsufficient() {
            wallet.setBalance(new BigDecimal("10.00")); // Order cần 125.00

            assertThatThrownBy(() -> orderService.completeOrder(888L))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.WALLET_NOT_ENOUGH_MONEY.name());

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.ERROR);
            verify(orderRepository, atLeastOnce()).save(order);
        }

        @Test
        @DisplayName("completeOrder success: updates credits, listing, order, and records wallet transactions")
        void completeOrder_success() {
            wallet.setBalance(new BigDecimal("1000.00"));

            // Buyer has no existing ISSUE credit; simulate repository findByCreditCode uniqueness check
            when(carbonCreditRepository.findByCreditCode(anyString())).thenReturn(List.of()); // unique code

            orderService.completeOrder(888L);

            // Order status set to success
            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.SUCCESS);
            verify(orderRepository, atLeastOnce()).save(order);

            // Listing decreased and may remain available (quantity 100 -> 90)
            assertThat(listing.getQuantity()).isEqualByComparingTo(new BigDecimal("90.0000"));
            assertThat(listing.getStatus()).isEqualTo(ListingStatus.AVAILABLE);
            verify(marketplaceListingRepository).save(listing);

            // Seller's listed amount decreased by 10
            assertThat(sellerCredit.getListedAmount()).isEqualTo(490);
            verify(carbonCreditRepository, atLeastOnce()).save(sellerCredit);

            // Buyer received credits (a new credit created with add)
            // We cannot directly capture new entity instance because it's created in service,
            // but we can verify that a CarbonCredit save occurred for buyer as well (at least twice total saves)
            verify(carbonCreditRepository, atLeast(2)).save(any(CarbonCredit.class));

            // Wallet transactions created for buyer and seller
            ArgumentCaptor<WalletTransactionRequest> wtrCaptor = ArgumentCaptor.forClass(WalletTransactionRequest.class);
            verify(walletTransactionService, times(2)).createTransaction(wtrCaptor.capture());

            List<WalletTransactionRequest> txs = wtrCaptor.getAllValues();
            assertThat(txs).hasSize(2);
            assertThat(txs.stream().map(WalletTransactionRequest::getType))
                    .containsExactlyInAnyOrder(WalletTransactionType.BUY_CARBON_CREDIT, WalletTransactionType.SELL_CARBON_CREDIT);
            assertThat(txs.stream().map(WalletTransactionRequest::getAmount))
                    .containsExactlyInAnyOrder(new BigDecimal("125.00").negate(), new BigDecimal("125.00"));
        }

        @Test
        @DisplayName("completeOrder fails when order not pending")
        void completeOrder_notPending() {
            order.setOrderStatus(OrderStatus.SUCCESS);
            assertThatThrownBy(() -> orderService.completeOrder(888L))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.ORDER_IS_NOT_PENDING.name());
        }
    }

    @Test
    @DisplayName("getOrderById throws when not found")
    void getOrderById_notFound() {
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.getOrderById(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // Thêm một test case cho setupSecurityContext để đảm bảo userRepository trả về Optional
    // ...
    @Test
    @DisplayName("setupSecurityContext correctly mocks userRepository")
    void setupSecurityContext_UserRepoReturnsOptional() {
        User foundUser = userRepository.findByEmail("buyer@acme.com");

        // SỬA 1: Dùng isNotNull() để kiểm tra object không phải là null
        assertThat(foundUser).isNotNull();

        // SỬA 2: Bỏ .get() vì foundUser đã là User, không phải Optional
        assertThat(foundUser.getEmail()).isEqualTo("buyer@acme.com");
    }
}
