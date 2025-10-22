package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.*;
import com.carbonx.marketcarbon.dto.request.OrderRequest;
import com.carbonx.marketcarbon.dto.request.WalletTransactionRequest;
import com.carbonx.marketcarbon.dto.response.OrderResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.*;
import com.carbonx.marketcarbon.repository.*;
import com.carbonx.marketcarbon.service.OrderService;
import com.carbonx.marketcarbon.service.WalletTransactionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
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

    // helper find user login
    private User currentUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if(user == null){
            throw new ResourceNotFoundException("User not found with email: " + email);
        }
        return user;
    }
    // helper find company login
    private Company currentCompany(User user){
        return companyRepository.findByUserId(user.getId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Company not found with userid: " + user.getId()));
    }

    @Transactional
    @Override
    public OrderResponse createOrder(OrderRequest request) {
        User user = currentUser();
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
                .createdAt(LocalDateTime.now())
                .build();

        Order saveOrder = orderRepository.save(order);
        return OrderResponse.builder()
                .id(saveOrder.getId())
                .companyId(saveOrder.getCompany().getId())
                .status(saveOrder.getOrderStatus())
                .totalAmount(saveOrder.getTotalPrice())
                .createAt(saveOrder.getCreatedAt())
                .build();
    }

    @Transactional
    @Override
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> new ResourceNotFoundException("Order not found with id: " + orderId));

        return OrderResponse.builder()
                .id(order.getId())
                .companyId(order.getCompany().getId())
                .status(order.getOrderStatus())
                .totalAmount(order.getTotalPrice())
                .createAt(order.getCreatedAt())
                .build();
    }

    @Override
    public List<OrderResponse> getUserOrders() {
        Company company = currentCompany(currentUser());
        List<Order> orders = orderRepository.findByCompany(company);

        return orders.stream()
                .map(order -> OrderResponse.builder()
                        .id(order.getId())
                        .companyId(company.getId())
                        .status(order.getOrderStatus())
                        .totalAmount(order.getTotalPrice())
                        .createAt(order.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        Company company = currentCompany(currentUser());
        if(!order.getCompany().getId().equals(company.getId())){
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if(order.getOrderStatus() != OrderStatus.PENDING){
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

        if(order.getOrderStatus() != OrderStatus.PENDING){
            throw new AppException(ErrorCode.ORDER_IS_NOT_PENDING);
        }

        // B2: Khóa bản ghi listing để đảm bảo nhất quán khi nhiều giao dịch cùng lúc
        MarketPlaceListing listing = marketplaceListingRepository.findByIdWithPessimisticLock(order.getMarketplaceListing().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace listing not found"));

        // B3: Lấy thông tin bên mua, bên bán và khối tín chỉ đang được rao bán
        Company buyerCompany = order.getCompany();
        Company sellerCompany = listing.getCompany();
        CarbonCredit sellerCredit = listing.getCarbonCredit();

        BigDecimal quantityToBuy = order.getQuantity();
        BigDecimal totalPrice= order.getTotalPrice();

        // B4: Kiểm tra số lượng còn lại trên sàn có đủ để đáp ứng đơn hay không
        if(listing.getQuantity().compareTo(quantityToBuy) < 0){
            order.setOrderStatus(OrderStatus.PENDING); // notify user
            orderRepository.save(order);
            throw new AppException(ErrorCode.AMOUNT_IS_NOT_ENOUGH);
        }

        // B5: Lấy ví của bên mua và đảm bảo số dư đủ để thanh toán
        Wallet buyerWallet = walletRepository.findByUserId(buyerCompany.getUser().getId());

        if(buyerWallet.getBalance().compareTo(totalPrice) < 0){
            order.setOrderStatus(OrderStatus.ERROR);
            orderRepository.save(order);
            throw new AppException(ErrorCode.WALLET_NOT_ENOUGH_MONEY);
        }

        // B6: Bắt đầu xử lý giao dịch
        // 6.1: Xác định ví của bên bán để ghi nhận tiền về
        Wallet sellerWallet = walletRepository.findByUserId(sellerCompany.getUser().getId());

        // 6.2: Trừ số tín chỉ được mua khỏi tổng tín chỉ của người bán (bao gồm cả đang rao bán)
        CarbonCredit sourceCredit = listing.getCarbonCredit();
        int currentListedAmount = sourceCredit.getListedAmount();
        BigDecimal totalSellerCredits = sourceCredit.getCarbonCredit()
                .add(BigDecimal.valueOf(currentListedAmount));

        int updatedListedAmount = currentListedAmount - quantityToBuy.intValueExact();
        int safeListedAmount = Math.max(0, updatedListedAmount);

        BigDecimal remainingTotalCredits = totalSellerCredits.subtract(quantityToBuy);
        if (remainingTotalCredits.compareTo(BigDecimal.ZERO) < 0) {
            remainingTotalCredits = BigDecimal.ZERO;
        }

        sourceCredit.setListedAmount(safeListedAmount);
        sourceCredit.setCarbonCredit(remainingTotalCredits);
        sourceCredit.setCreditCode(order.getCarbonCredit().getCreditCode());

        carbonCreditRepository.save(sourceCredit);

        // 6.3: Cộng tín chỉ cho bên mua, tái sử dụng lô cũ nếu có, tạo lô mới nếu chưa có
        CarbonCredit buyerCredit = buyerCompany.getCarbonCredits().stream()
                .filter(c -> c.getStatus() == CreditStatus.ISSUE)
                .findFirst() // tim thay tin chi dau tien neu co
                .orElseGet(() -> {  // khong co thi tao moi
                    // code CHỈ CHẠY khi không tìm thấy lô nào có sẵn
                    CarbonCredit newCredit = new CarbonCredit();
                    // new credit copy attribute from sourceCredit
                    newCredit.setCompany(buyerCompany);
                    newCredit.setStatus(CreditStatus.ISSUE);
                    newCredit.setName(sourceCredit.getName());
                    newCredit.setCarbonCredit(BigDecimal.ZERO);
                    newCredit.setCreditCode(order.getCarbonCredit().getCreditCode());
                    return newCredit;
                });

        BigDecimal buyerTotalCredits = buyerCredit.getCarbonCredit()
                .add(BigDecimal.valueOf(buyerCredit.getListedAmount()));
        BigDecimal updatedBuyerTotal = buyerTotalCredits.add(quantityToBuy);
        buyerCredit.setCarbonCredit(updatedBuyerTotal);
        carbonCreditRepository.save(buyerCredit);

        // 6.4: Cập nhật lại listing (số lượng còn lại, trạng thái nếu đã bán hết)
        listing.setQuantity(listing.getQuantity().subtract(quantityToBuy));
        if(listing.getQuantity().compareTo(BigDecimal.ZERO) <= 0){
            listing.setStatus(ListingStatus.SOLD);
        }
        marketplaceListingRepository.save(listing);

        // 6.5: Đánh dấu đơn hàng hoàn tất thành công
        order.setOrderStatus(OrderStatus.SUCCESS);
        orderRepository.save(order);

        // B7: Ghi nhận lịch sử giao dịch ví cho cả hai bên
        // 7.1: Ghi nhận trừ tiền bên mua
        walletTransactionService.createTransaction(WalletTransactionRequest.builder()
                .wallet(buyerWallet)
                .order(order)
                .type(WalletTransactionType.BUY_CARBON_CREDIT)
                .description("Buy" + quantityToBuy + "credits from" + sellerCompany.getCompanyName())
                .amount(totalPrice.negate())
                .build());

        // 7.2: Ghi nhận cộng tiền cho bên bán
        walletTransactionService.createTransaction(WalletTransactionRequest.builder()
                .wallet(sellerWallet)
                .order(order)
                .type(WalletTransactionType.SELL_CARBON_CREDIT)
                .description("Sell" + quantityToBuy + "credits from" + buyerCompany.getCompanyName())
                .amount(totalPrice)
                .build());
    }



    private String generateUniqueCreditCode(CarbonCredit referenceCredit){
        String prefix = (referenceCredit != null && referenceCredit.getCreditCode() != null)
                ? referenceCredit.getCreditCode()
                : "CC";
        String candidate;
        do {
            candidate = prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        } while (!carbonCreditRepository.findByCreditCode(candidate).isEmpty());
        return candidate;
    }

}
