package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.ListingStatus;
import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.dto.request.CreditListingRequest;
import com.carbonx.marketcarbon.dto.request.CreditListingUpdateRequest;
import com.carbonx.marketcarbon.dto.response.MarketplaceListingResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.*;
import com.carbonx.marketcarbon.repository.*;
import com.carbonx.marketcarbon.service.MarketplaceService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketplaceServiceImpl implements MarketplaceService {

    private final CarbonCreditRepository  carbonCreditRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final MarketplaceListingRepository marketplaceListingRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletRepository walletRepository;

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private User currentUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email =  authentication.getName();
        User user = userRepository.findByEmail(email);
        if(user == null){
            throw new RuntimeException("User not found with email " + email);
        }
        return user;
    }

    private Company currentCompany(User user) {
        return companyRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
    }

    @Override
    @Transactional
    public MarketplaceListingResponse listCreditsForSale(CreditListingRequest request) {
        log.info("Listing credits for sale: quantity={}, price={}",
                request.getQuantity(), request.getPricePerCredit());

        User currentUser = currentUser();
        Company sellerCompany = currentCompany(currentUser);

        // B1: Kiểm tra đầu vào
        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.AMOUNT_IS_NOT_VALID);
        }

        // B2 Tìm tín chir carbon mà company đang sở hữu , lấy id carbon , conpany , và số lượng muốn bán
        CarbonCredit creditToSell = resolveOwnedCredit(request.getCarbonCreditId(), sellerCompany, request.getQuantity());

        // B3 Kiểm tra quyền sở hữu tín chỉ
        if (!verifyOwnership(creditToSell, sellerCompany, currentUser)) {
            throw new AppException(ErrorCode.COMPANY_NOT_OWN);
        }

        // 3/1 Tính toán số lượng tín chỉ khả dụng
        BigDecimal availableQuantity = getAvailableCreditAmount(creditToSell);

        if (availableQuantity.compareTo(request.getQuantity()) < 0){
            throw new AppException(ErrorCode.AMOUNT_IS_NOT_ENOUGH);
        }

        // Kiểm tra xem đã có listing nào hiện có cho cùng loại tín chỉ carbon chưa
        List<MarketPlaceListing> existingListings = marketplaceListingRepository
                .findByCompanyIdAndCarbonCreditIdAndStatus(
                        sellerCompany.getId(),
                        creditToSell.getId(),
                        ListingStatus.AVAILABLE);

        // B 4 Nếu tìm thấy listing hiện có, cập nhật nó thay vì tạo mới
        if (!existingListings.isEmpty()) {
            MarketPlaceListing existingListing = existingListings.get(0);
            log.info("Found existing listing ID: {}. Merging new request with existing listing.",
                    existingListing.getId());

            // B4.1 Cập nhật số lượng tín chỉ niêm yết và số lượng còn lại
            BigDecimal currentListedAmount = creditToSell.getListedAmount() != null
                    ? creditToSell.getListedAmount()
                    : BigDecimal.ZERO;

            BigDecimal updatedAvailableQuantity = availableQuantity.subtract(request.getQuantity());

            if (updatedAvailableQuantity.compareTo(BigDecimal.ZERO) < 0) {
                throw new AppException(ErrorCode.AMOUNT_IS_NOT_ENOUGH);
            }
            // B4.2 Cập nhập số lượng tín chỉ thêm mới
            BigDecimal updatedListedAmount = currentListedAmount.add(request.getQuantity());
            creditToSell.setCarbonCredit(updatedAvailableQuantity);
            creditToSell.setListedAmount(updatedListedAmount);

            // 4.3 cập nhập tổng số tín chỉ
            BigDecimal recalculatedTotal = updatedAvailableQuantity.add(updatedListedAmount);
            creditToSell.setAmount(recalculatedTotal);

            carbonCreditRepository.save(creditToSell);

            //B4.2 Cập nhật listing hiện có thay vì tạo mới
            BigDecimal newQuantity = existingListing.getQuantity().add(request.getQuantity());
            BigDecimal newOriginalQuantity = existingListing.getOriginalQuantity().add(request.getQuantity());

            // Cập nhật số lượng
            existingListing.setQuantity(newQuantity);
            existingListing.setOriginalQuantity(newOriginalQuantity);

            // Cập nhật giá nếu có request thay đổi
            existingListing.setPricePerCredit(request.getPricePerCredit());


            MarketPlaceListing updatedListing = marketplaceListingRepository.save(existingListing);
            log.info("Updated existing listing ID: {} with additional quantity: {}",
                    updatedListing.getId(), request.getQuantity());

            return buildListingResponse(updatedListing);

        }


        // Nếu không tìm thấy listing hiện có, tạo mới
        // B5 : Cập nhật số lượng tín chỉ niêm yết và số lượng còn lại (giữ nguyên code cũ)
        BigDecimal currentListedAmount = creditToSell.getListedAmount() != null
                ? creditToSell.getListedAmount()
                : BigDecimal.ZERO;

        BigDecimal updatedAvailableQuantity = availableQuantity.subtract(request.getQuantity());

        if (updatedAvailableQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(ErrorCode.AMOUNT_IS_NOT_ENOUGH);
        }

        BigDecimal updatedListedAmount = currentListedAmount.add(request.getQuantity());
        creditToSell.setCarbonCredit(updatedAvailableQuantity);
        creditToSell.setListedAmount(updatedListedAmount);

        BigDecimal recalculatedTotal = updatedAvailableQuantity.add(updatedListedAmount);
        creditToSell.setAmount(recalculatedTotal);

        carbonCreditRepository.save(creditToSell);

        // B6: Tạo mới một listing trên sàn giao dịch
        MarketPlaceListing newListing = MarketPlaceListing.builder()
                .company(sellerCompany)
                .carbonCredit(creditToSell)
                .quantity(request.getQuantity())
                .pricePerCredit(request.getPricePerCredit())
                .originalQuantity(request.getQuantity())
                .soldQuantity(BigDecimal.ZERO)
                .status(ListingStatus.AVAILABLE)
                .createdAt(LocalDateTime.now(VIETNAM_ZONE))
                .build();

        MarketPlaceListing savedListing = marketplaceListingRepository.save(newListing);
        log.info("Created new listing ID: {}", savedListing.getId());

        return buildListingResponse(savedListing);

    }

    @Override
    public List<MarketplaceListingResponse> getActiveListing() {
        List<MarketPlaceListing> activeListings = marketplaceListingRepository.findByStatusAndExpiresAtAfter(ListingStatus.AVAILABLE, LocalDateTime.now());

        return activeListings.stream()
                .map(this::buildListingResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MarketplaceListingResponse updateListCredits(CreditListingUpdateRequest request) {
        //B1 check đầu vào
        if (request == null) {
            throw new AppException(ErrorCode.LISTING_IS_NOT_AVAILABLE);
        }

        // B2 lấy ID và giá từ request
        Long listingId = request.getListingId();
        BigDecimal pricePerCredit = request.getPricePerCredit();

        if (listingId == null) {
            throw new AppException(ErrorCode.LISTING_IS_NOT_AVAILABLE);
        }
        if (pricePerCredit == null || pricePerCredit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.AMOUNT_IS_NOT_VALID);
        }

        User currentUser = currentUser();
        Company sellerCompany = currentCompany(currentUser);

        MarketPlaceListing listing = marketplaceListingRepository.findById(listingId)
                .orElseThrow(() -> new AppException(ErrorCode.LISTING_IS_NOT_AVAILABLE));

        if (!Objects.equals(listing.getCompany().getId(), sellerCompany.getId())) {
            throw new AppException(ErrorCode.COMPANY_NOT_OWN);
        }
        if (listing.getStatus() != ListingStatus.AVAILABLE) {
            throw new AppException(ErrorCode.LISTING_IS_NOT_AVAILABLE);
        }

        listing.setPricePerCredit(pricePerCredit);
        MarketPlaceListing savedListing = marketplaceListingRepository.save(listing);

        return buildListingResponse(savedListing);
    }

    @Override
    @Transactional
    public MarketplaceListingResponse deleteListCredits(Long creditListingId) {
        if (creditListingId == null) {
            throw new AppException(ErrorCode.LISTING_IS_NOT_AVAILABLE);
        }

        User currentUser = currentUser();
        Company sellerCompany = currentCompany(currentUser);

        MarketPlaceListing listing = marketplaceListingRepository.findById(creditListingId)
                .orElseThrow(() -> new AppException(ErrorCode.LISTING_IS_NOT_AVAILABLE));

        if (!Objects.equals(listing.getCompany().getId(), sellerCompany.getId())) {
            throw new AppException(ErrorCode.COMPANY_NOT_OWN);
        }

        CarbonCredit carbonCredit = listing.getCarbonCredit();
        if (carbonCredit != null) {
            BigDecimal remainingQuantity = listing.getQuantity() != null ? listing.getQuantity() : BigDecimal.ZERO;
            BigDecimal creditBalance = carbonCredit.getCarbonCredit() != null ? carbonCredit.getCarbonCredit() : BigDecimal.ZERO;
            BigDecimal listedAmount = carbonCredit.getListedAmount() != null ? carbonCredit.getListedAmount() : BigDecimal.ZERO;

            carbonCredit.setCarbonCredit(creditBalance.add(remainingQuantity));
            BigDecimal updatedListed = listedAmount.subtract(remainingQuantity);
            if (updatedListed.compareTo(BigDecimal.ZERO) < 0) {
                updatedListed = BigDecimal.ZERO;
            }
            carbonCredit.setListedAmount(updatedListed);
            carbonCreditRepository.save(carbonCredit);
        }

        listing.setQuantity(BigDecimal.ZERO);
        listing.setStatus(ListingStatus.CANCELLED);
        MarketplaceListingResponse response = buildListingResponse(listing);
        marketplaceListingRepository.delete(listing);

        return response;
    }

    private MarketplaceListingResponse buildListingResponse(MarketPlaceListing listing) {
        BigDecimal remainingQuantity = listing.getQuantity() != null ? listing.getQuantity() : BigDecimal.ZERO;
        BigDecimal soldQuantity = listing.getSoldQuantity() != null ? listing.getSoldQuantity() : BigDecimal.ZERO;

        BigDecimal originalQuantity = listing.getOriginalQuantity();
        if (originalQuantity == null || originalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            originalQuantity = remainingQuantity.add(soldQuantity);
        }

        Project project = listing.getCarbonCredit().getProject();

        return MarketplaceListingResponse.builder()
                .listingId(listing.getId())
                .quantity(remainingQuantity)
                .availableQuantity(remainingQuantity)
                .originalQuantity(originalQuantity)
                .soldQuantity(soldQuantity)
                .pricePerCredit(listing.getPricePerCredit())
                .sellerCompanyName(listing.getCompany().getCompanyName())
                .projectId(project != null ? project.getId() : null)
                .projectTitle(project != null ? project.getTitle() : null)
                .expiresAt(listing.getExpiresAt())
                .logo(project != null ? project.getLogo() : null)
                .build();
    }
    private BigDecimal getAvailableCreditAmount(CarbonCredit credit) {
        if (credit == null) {
            return BigDecimal.ZERO;
        }

        // carbonCredit là số lượng available
        BigDecimal directBalance = credit.getCarbonCredit();
        if (directBalance != null && directBalance.compareTo(BigDecimal.ZERO) > 0) {
            return directBalance;
        }

        // lấy số lượng
        BigDecimal amount = credit.getAmount();

        //Số lượng đã list trên sàn
        BigDecimal listedAmount = credit.getListedAmount() != null ? credit.getListedAmount() : BigDecimal.ZERO;

        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal remaining = amount.subtract(listedAmount);
            return remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO;
        }


        return BigDecimal.ZERO;
    }

    private boolean verifyOwnership(CarbonCredit credit, Company company, User user) {
        return verifyOwnership(credit, company, user, new HashSet<>());
    }

    private boolean verifyOwnership(CarbonCredit credit, Company company, User user, Set<Long> visited) {
        if (credit == null || company == null || user == null) {
            return false;
        }

        Long creditId = credit.getId();
        if (creditId != null && !visited.add(creditId)) {
            return false;
        }

        Company creditCompany = credit.getCompany();
        if (creditCompany != null && Objects.equals(creditCompany.getId(), company.getId())) {
            return true; // Công ty sở hữu trực tiếp tín chỉ
        }

        Wallet wallet = walletRepository.findByCompany(company).orElse(null);
        if (wallet == null) {
            wallet = walletRepository.findByUserId(user.getId());
        }

        if (wallet != null) {
            CreditBatch creditBatch = credit.getBatch();
            if (creditBatch != null) {
                List<WalletTransaction> batchTransactions = walletTransactionRepository
                        .findByWalletAndCreditBatchOrderByCreatedAtDesc(wallet, creditBatch);
                if (!batchTransactions.isEmpty()) {
                    return true;
                }
            }

            List<WalletTransaction> buyTransactions = walletTransactionRepository
                    .findByWalletAndTransactionTypeOrderByCreatedAtDesc(wallet, WalletTransactionType.BUY_CARBON_CREDIT);
            for (WalletTransaction transaction : buyTransactions) {
                Order order = transaction.getOrder();
                if (order != null) {
                    CarbonCredit orderCredit = order.getCarbonCredit();
                    if (orderCredit != null && Objects.equals(orderCredit.getId(), creditId)) {
                        return true;
                    }
                }
            }
        }

        // Kiểm tra đệ quy trên tín chỉ nguồn
        CarbonCredit sourceCredit = credit.getSourceCredit();
        if (sourceCredit != null) {
            return verifyOwnership(sourceCredit, company, user, visited);
        }

        return false;
    }
    private CarbonCredit resolveOwnedCredit(Long creditId, Company company, BigDecimal requiredQuantity) {
        if (creditId == null) {
            throw new ResourceNotFoundException("Carbon credit block not found");
        }

        // B1 ìm trực tiếp credit với credit id và companyID
        CarbonCredit directCredit = carbonCreditRepository.findByIdAndCompanyId(creditId, company.getId())
                .orElse(null);

        CarbonCredit bestMatch = null;
        BigDecimal bestAvailable = BigDecimal.ZERO;

        if (directCredit != null) {
            BigDecimal available = getAvailableCreditAmount(directCredit);
            if (requiredQuantity == null || available.compareTo(requiredQuantity) >= 0) {
                return directCredit;
            }
        }

        List<CarbonCredit> candidateCredits = carbonCreditRepository
                .findCreditsBatchOrChainLinkedToIdWithSufficientAmount(
                        creditId, company.getId(), requiredQuantity);

        if (!candidateCredits.isEmpty()) {
            return candidateCredits.get(0);
        }

        throw new ResourceNotFoundException("Carbon credit block not found");
    }

    private boolean isLinkedToCreditChain(CarbonCredit credit, Long targetId, Set<Long> visited) {
        if (credit == null || targetId == null) {
            return false;
        }

        Long currentId = credit.getId();
        if (currentId != null && !visited.add(currentId)) {
            return false;
        }

        if (Objects.equals(currentId, targetId)) {
            return true;
        }

        return isLinkedToCreditChain(credit.getSourceCredit(), targetId, visited);
    }

}

