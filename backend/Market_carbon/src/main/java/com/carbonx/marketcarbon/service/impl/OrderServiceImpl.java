package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.*;
import com.carbonx.marketcarbon.dto.request.OrderRequest;
import com.carbonx.marketcarbon.dto.request.WalletTransactionRequest;
import com.carbonx.marketcarbon.dto.response.CreditTradeResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.*;
import com.carbonx.marketcarbon.repository.*;
import com.carbonx.marketcarbon.service.CreditIssuanceService;
import com.carbonx.marketcarbon.service.OrderService;
import com.carbonx.marketcarbon.service.WalletTransactionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final WalletTransactionService walletTransactionService;
    private final WalletRepository walletRepository;
    private final CompanyRepository companyRepository;
    private final MarketplaceListingRepository marketplaceListingRepository;
    private final CarbonCreditRepository carbonCreditRepository;
    private final CreditIssuanceService creditIssuanceService;


    // Định nghĩa múi giờ Việt Nam
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    // helper find user login
    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with email: " + email);
        }
        return user;
    }

    // helper find company login
    private Company currentCompany(User user) {
        return companyRepository.findByUserId(user.getId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Company not found with userid: " + user.getId()));
    }

    @Transactional
    @Override
    public CreditTradeResponse createOrder(OrderRequest request) {
        User user = currentUser();
        // Lấy thông tin công ty người mua từ user hiện tại
        // Lấy buyerCompany từ request hoặc context thay vì luôn là công ty của user hiện tại nếu cần
        Company buyerCompany = currentCompany(user);

        //1 find listing user want to buy
        MarketPlaceListing listing = marketplaceListingRepository.findById(request.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace listing not found"));

        // 2 check conditional > 0
        if (listing.getStatus() != com.carbonx.marketcarbon.common.ListingStatus.AVAILABLE) {
            throw new AppException(ErrorCode.LISTING_IS_NOT_AVAILABLE);
        }
        if (request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.AMOUNT_IS_NOT_VALID);
        }
        if (listing.getQuantity().compareTo(request.getQuantity()) < 0) {
            throw new AppException(ErrorCode.AMOUNT_IS_NOT_ENOUGH);
        }
        // Kiểm tra xem người mua có đang cố mua listing của chính mình không
        if (listing.getCompany().getId().equals(buyerCompany.getId())) {
            throw new AppException(ErrorCode.SELLER_COMPANY_INVALID);
        }

        //3 sum of price order
        BigDecimal unitPrice = listing.getPricePerCredit();
        BigDecimal totalPrice = unitPrice.multiply(request.getQuantity());

        Order order = Order.builder()
                .company(buyerCompany)
                .marketplaceListing(listing)
                .carbonCredit(listing.getCarbonCredit())
                .orderType(OrderType.BUY)
                .orderStatus(OrderStatus.PENDING)
                .quantity(request.getQuantity())
                .unitPrice(unitPrice)
                .totalPrice(totalPrice)
                .platformFee(BigDecimal.ZERO) // tinhs sau
                .sellerPayout(totalPrice)
                .createdAt(LocalDateTime.now(VIETNAM_ZONE))
                .build();

        Order saveOrder = orderRepository.save(order);
        return CreditTradeResponse.builder()
                .id(saveOrder.getId())
                .companyId(saveOrder.getCompany().getId())
                .status(saveOrder.getOrderStatus())
                .totalAmount(saveOrder.getTotalPrice())
                .creditAmount(saveOrder.getQuantity())
                .createAt(saveOrder.getCreatedAt())
                .build();
    }

    @Override
    public CreditTradeResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> new ResourceNotFoundException("Order not found with id: " + orderId));

        return CreditTradeResponse.builder()
                .id(order.getId())
                .companyId(order.getCompany().getId())
                .status(order.getOrderStatus())
                .totalAmount(order.getTotalPrice())
                .createAt(order.getCreatedAt())
                .creditAmount(order.getQuantity())
                .build();
    }

    @Override
    public List<CreditTradeResponse> getUserOrders() {
        Company company = currentCompany(currentUser());
        List<Order> orders = orderRepository.findByCompany(company);

        return orders.stream()
                .map(order -> CreditTradeResponse.builder()
                        .id(order.getId())
                        .companyId(company.getId())
                        .status(order.getOrderStatus())
                        .totalAmount(order.getTotalPrice())
                        .creditAmount(order.getQuantity())
                        .createAt(order.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        User user = currentUser();
        Company company = currentCompany(user);

        // Chỉ công ty tạo đơn hàng mới có quyền hủy
        if (!order.getCompany().getId().equals(company.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.ORDER_IS_NOT_PENDING);
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    @Transactional
    @Override
    public void completeOrder(Long orderId) {
        // B1: Lấy thông tin đơn hàng và kiểm tra trạng thái trước khi xử lý
        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.ORDER_IS_NOT_PENDING);
        }

        // B2: Khóa bản ghi listing để đảm bảo nhất quán khi nhiều giao dịch cùng lúc
        MarketPlaceListing listing = marketplaceListingRepository.findByIdWithPessimisticLock(order.getMarketplaceListing().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace listing not found"));

        // B3: Lấy thông tin bên mua, bên bán và khối tín chỉ đang được rao bán
        Company buyerCompany = order.getCompany();
        Company sellerCompany = listing.getCompany();
        CarbonCredit sourceCredit = listing.getCarbonCredit();

        BigDecimal quantityToBuy = order.getQuantity();
        BigDecimal totalPrice = order.getTotalPrice();

        // B4: Kiểm tra số lượng còn lại trên sàn có đủ để đáp ứng đơn hay không
        if (listing.getQuantity().compareTo(quantityToBuy) < 0) {
            order.setOrderStatus(OrderStatus.ERROR);
            orderRepository.save(order);
            throw new AppException(ErrorCode.AMOUNT_IS_NOT_ENOUGH);
        }

        // B5: Lấy ví của bên mua và đảm bảo số dư đủ để thanh toán
        Wallet buyerWallet = walletRepository.findByUserId(buyerCompany.getUser().getId());
        if (buyerWallet == null) {
            throw new ResourceNotFoundException("Buyer wallet not found for user: " + buyerCompany.getUser().getId());
        }

        // Chỉ kiểm tra, không trừ tiền ở đây
        if (buyerWallet.getBalance().compareTo(totalPrice) < 0) {
            order.setOrderStatus(OrderStatus.ERROR); // Đánh dấu lỗi nếu không đủ tiền
            orderRepository.save(order);
            throw new AppException(ErrorCode.WALLET_NOT_ENOUGH_MONEY);
        }

        // B6: Bắt đầu xử lý giao dịch
        // 6.1: Xác định ví của bên bán để ghi nhận tiền về
        Wallet sellerWallet = walletRepository.findByUserId(sellerCompany.getUser().getId());
        if (sellerWallet == null) {
            throw new ResourceNotFoundException("Seller wallet not found for user: " + sellerCompany.getUser().getId());
        }

        // 6.2: Trừ số tín chỉ được mua khỏi tổng tín chỉ của người bán (bao gồm cả đang rao bán)
        // Trừ số lượng khỏi phần đang niêm yết (listedAmount) của tín chỉ gốc
        BigDecimal currentListedAmount = sourceCredit.getListedAmount();
        // Số lượng niêm yết mới = số lượng niêm yết cũ - số lượng mua
        BigDecimal updatedListedAmount = currentListedAmount.subtract(quantityToBuy);
        sourceCredit.setListedAmount(updatedListedAmount.max(BigDecimal.ZERO));

        sourceCredit.setCompany(buyerCompany); // Gán công ty người mua cho tín chỉ này
        sourceCredit.setStatus(CreditStatus.ISSUE); // Đảm bảo trạng thái là ISSUE sau khi mua

        // Lưu lại tín chỉ đã cập nhật số lượng niêm yết cho bên bán
        carbonCreditRepository.save(sourceCredit);

        String issuedBy = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getName)
                .orElse(buyerCompany.getUser() != null ? buyerCompany.getUser().getEmail() : "system@carbon.con") ;

        CarbonCredit carbonCredit = creditIssuanceService.issueTradeCredit(
                sourceCredit,
                buyerCompany,
                quantityToBuy,
                listing.getPricePerCredit(),
                issuedBy);

        // 6.3 Cập nhật số dư credit carbon trong vi
        // Cộng số lượng tín chỉ đã mua vào ví người mua
        BigDecimal currentBuyerCredit = buyerWallet.getCarbonCreditBalance() != null ? buyerWallet.getCarbonCreditBalance() : BigDecimal.ZERO;
        buyerWallet.setCarbonCreditBalance(currentBuyerCredit.add(quantityToBuy));
        walletRepository.save(buyerWallet); // Lưu thay đổi số dư tín chỉ

        // Trừ số dư tín chỉ tổng trong ví người bán
        BigDecimal currentSellerCredit = sellerWallet.getCarbonCreditBalance() != null ? sellerWallet.getCarbonCreditBalance() : BigDecimal.ZERO;
        sellerWallet.setCarbonCreditBalance(currentSellerCredit.subtract(quantityToBuy));

        // Đảm bảo không âm
        if (sellerWallet.getCarbonCreditBalance().compareTo(BigDecimal.ZERO) < 0) {
            sellerWallet.setCarbonCreditBalance(BigDecimal.ZERO);
        }
        walletRepository.save(sellerWallet);

        // 6.4: Cập nhật lại listing (số lượng còn lại, trạng thái nếu đã bán hết)
        // Cập nhật tồn kho listing sau khi giao dịch hoàn tất
        listing.setQuantity(listing.getQuantity().subtract(quantityToBuy));
        BigDecimal currentSold = listing.getSoldQuantity() != null ? listing.getSoldQuantity() : BigDecimal.ZERO;
        listing.setSoldQuantity(currentSold.add(quantityToBuy));

        if (listing.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            listing.setStatus(ListingStatus.SOLD);
            listing.setExpiresAt(LocalDateTime.now(VIETNAM_ZONE)); // Cập nhật thời gian hết hạn khi bán hết
        }
        marketplaceListingRepository.save(listing);


        // 6.5: Đánh dấu đơn hàng hoàn tất thành công
        order.setOrderStatus(OrderStatus.SUCCESS);
        orderRepository.save(order);

        // B7: Ghi nhận lịch sử giao dịch ví cho cả hai bên
        // 7.1: Ghi nhận trừ tiền bên mua (createTransaction sẽ xử lý trừ tiền)
        try {
            walletTransactionService.createTransaction(WalletTransactionRequest.builder()
                    .wallet(buyerWallet)
                    .order(order)
                    .type(WalletTransactionType.BUY_CARBON_CREDIT)
                    .description("Buy " + quantityToBuy + " credits from listing " + listing.getId() + " (Seller: " + sellerCompany.getCompanyName() + ")")
                    // createTransaction sẽ tự trừ tiền dựa trên amount dương này và type BUY
                    .amount(totalPrice)
                    .build());
        } catch (Exception e) {
            // Nếu giao dịch trừ tiền thất bại, rollback trạng thái đơn hàng và throw lỗi
            order.setOrderStatus(OrderStatus.ERROR);
            orderRepository.save(order);
            throw new RuntimeException("Failed to process buyer transaction: " + e.getMessage(), e);
        }


        // 7.2: Ghi nhận cộng tiền cho bên bán (createTransaction sẽ xử lý cộng tiền)
        try {
            walletTransactionService.createTransaction(WalletTransactionRequest.builder()
                    .wallet(sellerWallet)
                    .order(order)
                    .type(WalletTransactionType.SELL_CARBON_CREDIT)
                    .description("Sell " + quantityToBuy + " credits via listing " + listing.getId() + " (Buyer: " + buyerCompany.getCompanyName() + ")")
                    // createTransaction sẽ tự cộng tiền dựa trên amount dương này và type SELL
                    .amount(totalPrice)
                    .build());
        } catch (Exception e) {
            order.setOrderStatus(OrderStatus.ERROR); // Đánh dấu lỗi
            orderRepository.save(order);
            throw new RuntimeException("Failed to process seller transaction (Buyer was charged): " + e.getMessage(), e);
        }
    }

}
