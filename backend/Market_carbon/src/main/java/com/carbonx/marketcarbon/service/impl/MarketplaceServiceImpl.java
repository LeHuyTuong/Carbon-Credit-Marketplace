package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.ListingStatus;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    public MarketplaceListingResponse listCreditsForSale(CreditListingRequest request) {
        User currentUser = currentUser();
        Company sellerCompany = currentCompany(currentUser);
//
//        // B1 tìm ví của company
//        Wallet companyWallet = walletRepository.findByUserId(currentUser.getId());
//        // B2 vào trong ví để tìm wallet transaction để tìm credit batch
//        WalletTransaction walletTransaction = walletTransactionRepository.findByWalletId(companyWallet.getId());
//
//        //B3 tìm ID credit batch muốn list
//        CreditBatch batch = walletTransaction.getCreditBatch();


        // 1 check infor input
        CarbonCredit creditToSell = carbonCreditRepository.findById(request.getCarbonCreditId())
                .orElseThrow(() -> new ResourceNotFoundException("Carbon credit block not found"));

        // 2 check Authorized and credit quantity available
        if(!Objects.equals(creditToSell.getCompany().getId(), sellerCompany.getId())){
            throw new AppException(ErrorCode.COMPANY_NOT_OWN);
        }

        BigDecimal availableQuantity = creditToSell.getCarbonCredit() != null
                ? creditToSell.getCarbonCredit()
                : BigDecimal.ZERO; ;
        if(availableQuantity.compareTo(request.getQuantity()) < 0){
            throw new AppException(ErrorCode.AMOUNT_IS_NOT_ENOUGH);
        }

        // 3 update amount in carbon credit block
        creditToSell.setCarbonCredit(availableQuantity.subtract(request.getQuantity()));
        BigDecimal currentListedAmount = creditToSell.getListedAmount() != null
                ? creditToSell.getListedAmount()
                : BigDecimal.ZERO;
        creditToSell.setListedAmount(currentListedAmount.add(request.getQuantity()));

        carbonCreditRepository.save(creditToSell);

        //4 create a new listing to marketplace
        MarketPlaceListing newListing = MarketPlaceListing.builder()
                .company(sellerCompany)
                .carbonCredit(creditToSell)
                .quantity(request.getQuantity())
                .pricePerCredit(request.getPricePerCredit())
                .originalQuantity(request.getQuantity())
                .soldQuantity(BigDecimal.ZERO)
                .status(ListingStatus.AVAILABLE)
                .createdAt(LocalDateTime.now(VIETNAM_ZONE))
                .expiresAt(request.getExpirationDate())
                .build();


        MarketPlaceListing savedListing = marketplaceListingRepository.save(newListing);

        BigDecimal remainingQuantity = savedListing.getQuantity() != null ? savedListing.getQuantity() : BigDecimal.ZERO;
        BigDecimal soldQuantity = savedListing.getSoldQuantity() != null ? savedListing.getSoldQuantity() : BigDecimal.ZERO;

        // Tính lại originalQuantity đề phòng dữ liệu cũ chưa được set giá trị
        BigDecimal originalQuantity = savedListing.getOriginalQuantity();
        if (originalQuantity == null || originalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            originalQuantity = remainingQuantity.add(soldQuantity);
        }

        return  MarketplaceListingResponse.builder()
                .listingId(savedListing.getId())
                .quantity(remainingQuantity)
                .availableQuantity(remainingQuantity)
                .originalQuantity(originalQuantity)
                .soldQuantity(soldQuantity)
                .pricePerCredit(savedListing.getPricePerCredit())
                .sellerCompanyName(savedListing.getCompany().getCompanyName())
                .projectId(savedListing.getCarbonCredit().getProject().getId())
                .projectTitle(savedListing.getCarbonCredit().getProject().getTitle())
                .expiresAt(savedListing.getExpiresAt())
                .build();
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
        if (request == null) {
            throw new AppException(ErrorCode.LISTING_IS_NOT_AVAILABLE);
        }

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
}

