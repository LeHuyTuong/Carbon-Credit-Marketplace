package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.common.CreditStatus;
import com.carbonx.marketcarbon.common.ListingStatus;
import com.carbonx.marketcarbon.common.OrderStatus;
import com.carbonx.marketcarbon.common.OrderType;
import com.carbonx.marketcarbon.dto.request.WalletTransactionRequest;
import com.carbonx.marketcarbon.model.*;
import com.carbonx.marketcarbon.repository.*;
import com.carbonx.marketcarbon.service.WalletTransactionService;
import com.carbonx.marketcarbon.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletTransactionService walletTransactionService;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private MarketplaceListingRepository marketplaceListingRepository;

    @Mock
    private CarbonCreditRepository carbonCreditRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User buyerUser;
    private User sellerUser;
    private Company buyerCompany;
    private Company sellerCompany;
    private Wallet buyerWallet;
    private Wallet sellerWallet;

    @BeforeEach
    void setUp() {
        // Khởi tạo dữ liệu người dùng bên mua
        buyerUser = new User();
        buyerUser.setId(1L);

        // Khởi tạo dữ liệu người dùng bên bán
        sellerUser = new User();
        sellerUser.setId(2L);

        // Doanh nghiệp bên mua gắn với user tương ứng
        buyerCompany = new Company();
        buyerCompany.setId(10L);
        buyerCompany.setUser(buyerUser);

        // Doanh nghiệp bên bán và thông tin hiển thị tên công ty
        sellerCompany = new Company();
        sellerCompany.setId(20L);
        sellerCompany.setUser(sellerUser);
        sellerCompany.setCompanyName("Seller Co");

        // Ví của bên mua với số dư đủ lớn để thanh toán giao dịch
        buyerWallet = new Wallet();
        buyerWallet.setId(100L);
        buyerWallet.setBalance(new BigDecimal("1000"));
        buyerWallet.setUser(buyerUser);

        // Ví của bên bán để nhận tiền về sau khi hoàn tất đơn
        sellerWallet = new Wallet();
        sellerWallet.setId(200L);
        sellerWallet.setBalance(BigDecimal.ZERO);
        sellerWallet.setUser(sellerUser);
    }

    @Test
    void completeOrder_PartialPurchaseRetainsSellerCredits() {
        // Số lượng tín chỉ người mua muốn lấy trong đơn thử nghiệm
        BigDecimal quantityToBuy = new BigDecimal("20");

        // Khối tín chỉ của bên bán: tổng 80 tín chỉ (50 đang niêm yết, 30 chưa niêm yết)
        CarbonCredit sellerCredit = new CarbonCredit();
        sellerCredit.setId(500L);
        sellerCredit.setCompany(sellerCompany);
        sellerCredit.setStatus(CreditStatus.ISSUE);
        sellerCredit.setName("Seller Credit");
        sellerCredit.setListedAmount(50);
        sellerCredit.setCarbonCredit(new BigDecimal("80")); // total holdings: 80 (30 unlisted + 50 listed)
        sellerCredit.setCreditCode("SELL-001");

        // Listing đang mở bán 50 tín chỉ với giá 10 mỗi tín chỉ
        MarketPlaceListing listing = new MarketPlaceListing();
        listing.setId(1000L);
        listing.setCompany(sellerCompany);
        listing.setCarbonCredit(sellerCredit);
        listing.setQuantity(new BigDecimal("50"));
        listing.setPricePerCredit(new BigDecimal("10"));
        listing.setStatus(ListingStatus.AVAILABLE);

        // Đơn hàng bên mua tạo cho 20 tín chỉ
        Order order = new Order();
        order.setId(3000L);
        order.setCompany(buyerCompany);
        order.setMarketplaceListing(listing);
        order.setCarbonCredit(sellerCredit);
        order.setOrderType(OrderType.BUY);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setQuantity(quantityToBuy);
        order.setUnitPrice(new BigDecimal("10"));
        order.setTotalPrice(new BigDecimal("200"));

        // Thiết lập giả lập (mock) cho các repository trả về dữ liệu đã chuẩn bị
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(marketplaceListingRepository.findByIdWithPessimisticLock(listing.getId())).thenReturn(Optional.of(listing));
        when(walletRepository.findByUserId(buyerUser.getId())).thenReturn(buyerWallet);
        when(walletRepository.findByUserId(sellerUser.getId())).thenReturn(sellerWallet);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(marketplaceListingRepository.save(any(MarketPlaceListing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Bắt các lần lưu CarbonCredit để kiểm tra giá trị sau giao dịch
        ArgumentCaptor<CarbonCredit> creditCaptor = ArgumentCaptor.forClass(CarbonCredit.class);
        when(carbonCreditRepository.save(creditCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // Không cần hành động gì khi ghi nhật ký ví vì chỉ kiểm tra gọi phương thức
        when(walletTransactionService.createTransaction(any(WalletTransactionRequest.class)))
                .thenReturn(new WalletTransaction());

        // Thực thi luồng hoàn tất đơn hàng
        orderService.completeOrder(order.getId());

        // Đảm bảo có ít nhất một lần lưu CarbonCredit (đầu tiên là cập nhật bên bán)
        assertThat(creditCaptor.getAllValues()).isNotEmpty();
        CarbonCredit savedSellerCredit = creditCaptor.getAllValues().get(0);

        // Kiểm tra trạng thái đơn, số lượng còn lại trên listing và trạng thái listing
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.SUCCESS);
        assertThat(listing.getQuantity()).isEqualByComparingTo(new BigDecimal("30"));
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.AVAILABLE);

        // Đảm bảo bên bán vẫn giữ lại tín chỉ chưa bán (30 niêm yết + 30 không niêm yết)
        assertThat(savedSellerCredit.getListedAmount()).isEqualTo(30);
        assertThat(savedSellerCredit.getCarbonCredit()).isEqualByComparingTo(new BigDecimal("30"));

        // Xác nhận phương thức tạo giao dịch ví đã được gọi
        verify(walletTransactionService).createTransaction(any(WalletTransactionRequest.class));
    }
}
