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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
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
        log.info("Starting order completion process for orderId: {}", orderId);

        // B1: Lấy thông tin đơn hàng và kiểm tra trạng thái trước khi xử lý
        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // Kiểm tra idempotency - nếu đơn hàng đã xử lý thành công
        if (order.getOrderStatus() == OrderStatus.SUCCESS) {
            log.info("Order already completed successfully: {}", orderId);
            return;
        }

        // B2: Khóa bản ghi listing để đảm bảo nhất quán khi nhiều giao dịch cùng lúc
        MarketPlaceListing listing = marketplaceListingRepository.findByIdWithPessimisticLock(order.getMarketplaceListing().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace listing not found"));

        // B3 Lấy thông tin bên mua, bên bán và khối tín chỉ
        Company buyerCompany = order.getCompany();
        Company sellerCompany = listing.getCompany();
        CarbonCredit sourceCredit = carbonCreditRepository.findByIdWithPessimisticLock(listing.getCarbonCredit().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Carbon credit not found"));


        BigDecimal quantityToBuy = order.getQuantity();
        BigDecimal totalPrice = order.getTotalPrice();

        // B4: Kiểm tra số lượng còn lại trên sàn có đủ để đáp ứng đơn hay không
        if (listing.getQuantity().compareTo(quantityToBuy) < 0) {
            order.setOrderStatus(OrderStatus.ERROR);
            orderRepository.save(order);
            log.warn("Insufficient quantity in listing. Available: {}, Requested: {}",
                    listing.getQuantity(), quantityToBuy);
            throw new AppException(ErrorCode.AMOUNT_IS_NOT_ENOUGH);
        }

        // B4: Xử lý giao dịch trong một transaction
        try{
            //4.1 lấy ví của các bên
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

            Wallet sellerWallet = walletRepository.findByUserId(sellerCompany.getUser().getId());
            if (sellerWallet == null) {
                throw new ResourceNotFoundException("Seller wallet not found for user: " + sellerCompany.getUser().getId());
            }

            // 4.2 Tính toán chính xác số lượng niêm yết sau khi bán
            BigDecimal currentListedAmount = sourceCredit.getListedAmount();
            if (currentListedAmount == null) {
                currentListedAmount = BigDecimal.ZERO;
            }

            // Số lượng niêm yết mới = số lượng niêm yết cũ - số lượng mua
            BigDecimal updatedListedAmount = currentListedAmount.subtract(quantityToBuy);
            if (updatedListedAmount.compareTo(BigDecimal.ZERO) < 0) {
                updatedListedAmount = BigDecimal.ZERO;
            }

            // Tính toán lại tổng số lượng sở hữu và available sau khi bán
            BigDecimal directAvailable = sourceCredit.getCarbonCredit() != null
                    ? sourceCredit.getCarbonCredit()
                    : BigDecimal.ZERO;

            LocalDate expiryDate = sourceCredit.getExpiryDate() != null ?
                    sourceCredit.getExpiryDate()
                    : LocalDate.now();


            // 4.3 Tổng số sau khi bán  trừ đi số lượng đã bán
            BigDecimal totalAfterSale = directAvailable.add(updatedListedAmount);  // Tổng = Available + Listed

            // 4.4 Cập nhật lại thông tin tín chỉ
            sourceCredit.setListedAmount(updatedListedAmount);
            sourceCredit.setAmount(totalAfterSale);  // Tổng = Available + Listed
            sourceCredit.setCarbonCredit(directAvailable);
            sourceCredit.setExpiryDate(expiryDate);

            // 4.5 Thêm xác thực để đảm bảo tính nhất quán
            if (totalAfterSale.compareTo(BigDecimal.ZERO) <= 0) {
                // Nếu đã bán hết, đặt trạng thái tín chỉ thành TRADED
                sourceCredit.setStatus(CreditStatus.TRADED);
            }

            // Lưu lại tín chỉ đã cập nhật số lượng niêm yết cho bên bán
            carbonCreditRepository.save(sourceCredit);

            // Tạo tín chỉ mới cho người mua
            String issuedBy = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                    .map(Authentication::getName)
                    .orElse(buyerCompany.getUser() != null ? buyerCompany.getUser().getEmail() : "system@carbon.con") ;

            CarbonCredit carbonCredit = creditIssuanceService.issueTradeCredit(
                    sourceCredit,
                    buyerCompany,
                    quantityToBuy,
                    listing.getPricePerCredit(),
                    issuedBy);

            // 5.1 Cập nhật số dư credit carbon trong vi
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


            // 5.2 : Cập nhật lại listing (số lượng còn lại, trạng thái nếu đã bán hết)
            // Cập nhật tồn kho listing sau khi giao dịch hoàn tất
            listing.setQuantity(listing.getQuantity().subtract(quantityToBuy));
            BigDecimal currentSold = listing.getSoldQuantity() != null ? listing.getSoldQuantity() : BigDecimal.ZERO;
            listing.setSoldQuantity(currentSold.add(quantityToBuy));

            if (listing.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                listing.setStatus(ListingStatus.SOLD);
                listing.setExpiresAt(sourceCredit.getExpiryDate());
            }
            marketplaceListingRepository.save(listing);

            // 6.5: Đánh dấu đơn hàng hoàn tất thành công
            order.setOrderStatus(OrderStatus.SUCCESS);
            order.setCompletedAt(LocalDateTime.now(VIETNAM_ZONE));
            orderRepository.save(order);

            // 7 Xử lý giao dịch tài chính
            processFinancialTransactions(order, buyerWallet, sellerWallet, totalPrice, quantityToBuy,
                    listing, sellerCompany, buyerCompany);

            log.info("Order {} completed successfully. Buyer: {}, Seller: {}, Amount: {}, Price: {}",
                    orderId, buyerCompany.getId(), sellerCompany.getId(), quantityToBuy, totalPrice);

        }catch (Exception e) {
            log.error("Error completing order: {}", orderId, e);
            order.setOrderStatus(OrderStatus.ERROR);
            orderRepository.save(order);
            throw e; // Transaction sẽ rollback tự động
        }
    }

    // xử lý giao dịch tài chính
    private void processFinancialTransactions(Order order, Wallet buyerWallet, Wallet sellerWallet,
                                              BigDecimal totalPrice, BigDecimal quantity,
                                              MarketPlaceListing listing, Company sellerCompany,
                                              Company buyerCompany) {
        try {
            // Giao dịch trừ tiền người mua
            WalletTransactionRequest buyerTxn = WalletTransactionRequest.builder()
                    .wallet(buyerWallet)
                    .order(order)
                    .type(WalletTransactionType.BUY_CARBON_CREDIT)
                    .description("Buy " + quantity + " credits from listing " + listing.getId() +
                            " (Seller: " + sellerCompany.getCompanyName() + ")")
                    .amount(totalPrice)
                    .build();

            walletTransactionService.createTransaction(buyerTxn);

            // Giao dịch cộng tiền người bán
            WalletTransactionRequest sellerTxn = WalletTransactionRequest.builder()
                    .wallet(sellerWallet)
                    .order(order)
                    .type(WalletTransactionType.SELL_CARBON_CREDIT)
                    .description("Sell " + quantity + " credits via listing " + listing.getId() +
                            " (Buyer: " + buyerCompany.getCompanyName() + ")")
                    .amount(totalPrice)
                    .build();

            walletTransactionService.createTransaction(sellerTxn);

        } catch (Exception e) {
            log.error("Error processing financial transactions for order: {}", order.getId(), e);
            throw new AppException(ErrorCode.TRANSACTION_PROCESSING_ERROR);
        }
    }

}
