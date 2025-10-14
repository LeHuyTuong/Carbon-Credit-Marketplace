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
        MarketplaceListing listing = marketplaceListingRepository.findById(request.getListingId())
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
        // B1 find infor of order and lock to process safe
        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if(order.getOrderStatus() != OrderStatus.PENDING){
            throw new AppException(ErrorCode.ORDER_IS_NOT_PENDING);
        }

        //B2 get company buy , sell , listing
        Company buyerCompany = order.getCompany();
        MarketplaceListing listing = marketplaceListingRepository.findByIdWithPessimisticLock(order.getMarketplaceListing().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace listing not found"));
        Company sellerCompany = listing.getCompany();

        BigDecimal quantityToBuy = order.getQuantity();
        BigDecimal totalPrice= order.getTotalPrice();

        //B3 check before process
        if(listing.getQuantity().compareTo(quantityToBuy) <= 0){
            order.setOrderStatus(OrderStatus.PENDING);
            orderRepository.save(order);
            throw new AppException(ErrorCode.AMOUNT_IS_NOT_ENOUGH);
        }

        // find wallet of company by user id
        Wallet buyerWallet = walletRepository.findByUserId(buyerCompany.getUser().getId());

        if(buyerWallet.getBalance().compareTo(totalPrice) <= 0){
            order.setOrderStatus(OrderStatus.ERROR);
            orderRepository.save(order);
            throw new AppException(ErrorCode.WALLET_NOT_ENOUGH_MONEY);
        }

        // B4 start to process trading
        // 4.1 send money
        Wallet sellerWallet = walletRepository.findByUserId(sellerCompany.getUser().getId());
        BigDecimal buyerWalletBalanceBefore = buyerWallet.getBalance();
        BigDecimal sellerWalletBalanceBefore = sellerWallet.getBalance();

        buyerWallet.setBalance(buyerWallet.getBalance().subtract(totalPrice));
        sellerWallet.setBalance(sellerWallet.getBalance().add(totalPrice));

        walletRepository.saveAll(List.of(buyerWallet, sellerWallet));

        //4.2  transfer credit
        CarbonCredit sourceCredit = listing.getCarbonCredit();

        // Tìm hoặc tạo một khối tín chỉ mới cho người mua
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
                    newCredit.setAmount(0);
                    return newCredit;
                });

        buyerCredit.setCarbonCredit(buyerCredit.getCarbonCredit().add(quantityToBuy));
        carbonCreditRepository.save(buyerCredit);

        //4.3 update listing
        listing.setQuantity(listing.getQuantity().subtract(quantityToBuy));
        if(listing.getQuantity().compareTo(BigDecimal.ZERO) <= 0){
            listing.setStatus(ListingStatus.SOLD);
        }
        marketplaceListingRepository.save(listing);

        //4.4 update status order
        order.setOrderStatus(OrderStatus.SUCCESS);
        orderRepository.save(order);

        //5 log wallet transaction
        // log buyer wallet
        walletTransactionService.createTransaction(WalletTransactionRequest.builder()
                        .wallet(buyerWallet)
                        .order(order)
                        .type(WalletTransactionType.BUY_CARBON_CREDIT)
                        .description("Buy" + quantityToBuy + "credits from" + sellerCompany.getCompanyName())
                        .balanceBefore(buyerWalletBalanceBefore)
                        .balanceAfter(buyerWallet.getBalance())
                        .amount(totalPrice.negate()) // money subtract
                .build());

        // log seller wallet
        walletTransactionService.createTransaction(WalletTransactionRequest.builder()
                        .wallet(sellerWallet)
                        .order(order)
                        .type(WalletTransactionType.SELL_CARBON_CREDIT)
                        .description("Sell" + quantityToBuy + "credits from" + buyerCompany.getCompanyName())
                        .balanceBefore(sellerWalletBalanceBefore)
                        .balanceAfter(sellerWallet.getBalance())
                        .amount(totalPrice.negate())
                .build());
    }


}
