package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.CreditStatus;
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
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final CreditBatchRepository creditBatchRepository;

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

        // B2 Nếu request có batchId, xử lý theo batch (TRỪ availableQuantity trên toàn bộ credits trong batch)
        if (request.getBatchId() != null) {
            CreditBatch batch = creditBatchRepository.findById(request.getBatchId())
                    .orElseThrow(() -> new AppException(ErrorCode.CREDIT_BATCH_NOT_FOUND));

            if (!batch.getCompany().getId().equals(sellerCompany.getId())) {
                throw new AppException(ErrorCode.COMPANY_NOT_OWN);
            }

            // 2.1 Lấy các credit còn AVAILABLE và còn available > 0
            List<CarbonCredit> credits = batch.getCarbonCredit().stream()
                    .filter(c -> c.getStatus() == CreditStatus.AVAILABLE)
                    .filter(c -> availableOf(c).compareTo(BigDecimal.ZERO) > 0)
                    .toList();

            if (credits.isEmpty()) {
                throw new AppException(ErrorCode.AMOUNT_IS_NOT_ENOUGH);
            }

            // 2.2 Check tổng available trong batch có đủ không (tính theo availableOf)
            BigDecimal totalAvailable = credits.stream()
                    .map(this::availableOf)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalAvailable.compareTo(request.getQuantity()) < 0) {
                throw new AppException(ErrorCode.AMOUNT_IS_NOT_ENOUGH);
            }

            // 2.3 Trừ dần available trên từng credit, đồng thời cộng vào listedAmount
            BigDecimal remaining = request.getQuantity();

            for (CarbonCredit c : credits) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

                BigDecimal availBefore = availableOf(c);
                if (availBefore.compareTo(BigDecimal.ZERO) <= 0) continue;

                BigDecimal deduct = availBefore.min(remaining);

                // update listed
                BigDecimal listed = safe(c.getListedAmount());
                BigDecimal availAfter = availBefore.subtract(deduct);
                c.setListedAmount(listed.add(deduct));

                // quy ước: carbonCredit = availableQuantity
                c.setCarbonCredit(availAfter);

                // giữ total nhất quán: amount = available + listed
                c.setAmount(availAfter.add(c.getListedAmount()));

                // hết available => coi như đã list hết credit này
                if (availAfter.compareTo(BigDecimal.ZERO) == 0) {
                    c.setStatus(CreditStatus.LISTED);
                }

                remaining = remaining.subtract(deduct);
            }

            carbonCreditRepository.saveAll(credits);

            // 2.4 Nếu đã có listing cho batch này, cập nhật thay vì tạo mới
            List<MarketPlaceListing> existingBatchListings = marketplaceListingRepository
                    .findByCompanyIdAndCarbonCredit_Batch_IdAndStatus(
                            sellerCompany.getId(),
                            batch.getId(),
                            ListingStatus.AVAILABLE);

            if (!existingBatchListings.isEmpty()) {
                MarketPlaceListing existingListing = existingBatchListings.get(0);

                BigDecimal updatedQuantity = safe(existingListing.getQuantity()).add(request.getQuantity());
                BigDecimal updatedOriginalQuantity = safe(existingListing.getOriginalQuantity()).add(request.getQuantity());

                existingListing.setQuantity(updatedQuantity);
                existingListing.setOriginalQuantity(updatedOriginalQuantity);
                existingListing.setPricePerCredit(request.getPricePerCredit());
                existingListing.setStatus(ListingStatus.AVAILABLE);

                MarketPlaceListing updatedListing = marketplaceListingRepository.save(existingListing);
                log.info("Updated existing batch listing ID: {} with additional quantity: {}",
                        updatedListing.getId(), request.getQuantity());

                return buildListingResponse(updatedListing);
            }

            // 2.5 Tạo listing tổng theo SỐ LƯỢNG YÊU CẦU (không đếm số record)
            MarketPlaceListing listing = MarketPlaceListing.builder()
                    .company(sellerCompany)
                    .carbonCredit(credits.get(0)) // link đại diện theo schema hiện tại
                    .quantity(request.getQuantity())
                    .pricePerCredit(request.getPricePerCredit())
                    .originalQuantity(request.getQuantity())
                    .soldQuantity(BigDecimal.ZERO)
                    .status(ListingStatus.AVAILABLE)
                    .createdAt(LocalDateTime.now(VIETNAM_ZONE))
                    .expiresAt(batch.getExpiresAt())
                    .build();

            MarketPlaceListing saved = marketplaceListingRepository.save(listing);
            log.info("Listed {} credits across {} CarbonCredit rows in batch {}",
                    request.getQuantity(), credits.size(), batch.getBatchCode());
            return buildListingResponse(saved);
        }

        // B3 Tìm tín chir carbon mà company đang sở hữu , lấy id carbon , conpany , và số lượng muốn bán
        CarbonCredit creditToSell = resolveOwnedCredit(request.getCarbonCreditId(), sellerCompany, request.getQuantity());

        // B 4.1 Tính toán số lượng tín chỉ khả dụng
        BigDecimal availableQuantity = getAvailableCreditAmount(creditToSell);

        if (availableQuantity.compareTo(request.getQuantity()) < 0){
            throw new AppException(ErrorCode.AMOUNT_IS_NOT_ENOUGH);
        }

        // 4.2 Kiểm tra xem đã có listing nào hiện có cho cùng loại tín chỉ carbon chưa
        List<MarketPlaceListing> existingListings = marketplaceListingRepository
                .findByCompanyIdAndCarbonCreditIdAndStatus(
                        sellerCompany.getId(),
                        creditToSell.getId(),
                        ListingStatus.AVAILABLE);

        // B5 Nếu tìm thấy listing hiện có, cập nhật nó thay vì tạo mới
        if (!existingListings.isEmpty()) {
            MarketPlaceListing existingListing = existingListings.get(0);
            log.info("Found existing listing ID: {}. Merging new request with existing listing.",
                    existingListing.getId());

            // B5.1 Cập nhật số lượng tín chỉ niêm yết và số lượng còn lại
            BigDecimal currentListedAmount = creditToSell.getListedAmount() != null
                    ? creditToSell.getListedAmount()
                    : BigDecimal.ZERO;

            BigDecimal updatedAvailableQuantity = availableQuantity.subtract(request.getQuantity());
            if (updatedAvailableQuantity.compareTo(BigDecimal.ZERO) < 0) {
                throw new AppException(ErrorCode.AMOUNT_IS_NOT_ENOUGH);
            }

            // B5.2 Cập nhập số lượng tín chỉ thêm mới
            BigDecimal updatedListedAmount = currentListedAmount.add(request.getQuantity());
            creditToSell.setCarbonCredit(updatedAvailableQuantity);
            creditToSell.setListedAmount(updatedListedAmount);

            // 5.3 cập nhập tổng số tín chỉ
            BigDecimal recalculatedTotal = updatedAvailableQuantity.add(updatedListedAmount);
            creditToSell.setAmount(recalculatedTotal);

            carbonCreditRepository.save(creditToSell);

            //B5.4 Cập nhật listing hiện có thay vì tạo mới
            BigDecimal newQuantity = existingListing.getQuantity().add(request.getQuantity());
            BigDecimal newOriginalQuantity = existingListing.getOriginalQuantity().add(request.getQuantity());

            // Cập nhật số lượng
            existingListing.setQuantity(newQuantity);
            existingListing.setOriginalQuantity(newOriginalQuantity);

            // Cập nhật giá nếu có request thay đổi
            existingListing.setPricePerCredit(request.getPricePerCredit());
            existingListing.setStatus(ListingStatus.AVAILABLE);

            MarketPlaceListing updatedListing = marketplaceListingRepository.save(existingListing);
            log.info("Updated existing listing ID: {} with additional quantity: {}",
                    updatedListing.getId(), request.getQuantity());

            return buildListingResponse(updatedListing);

        }

        // Nếu không tìm thấy listing hiện có, tạo mới
        // B6 : Cập nhật số lượng tín chỉ niêm yết và số lượng còn lại (giữ nguyên code cũ)
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
        creditToSell.setStatus(CreditStatus.LISTED);

        carbonCreditRepository.save(creditToSell);

        // B7: Tạo mới một listing trên sàn giao dịch
        MarketPlaceListing newListing = MarketPlaceListing.builder()
                .company(sellerCompany)
                .carbonCredit(creditToSell)
                .quantity(request.getQuantity())
                .pricePerCredit(request.getPricePerCredit())
                .originalQuantity(request.getQuantity())
                .soldQuantity(BigDecimal.ZERO)
                .status(ListingStatus.AVAILABLE)
                .createdAt(LocalDateTime.now(VIETNAM_ZONE))
                .expiresAt(creditToSell.getExpiryDate())
                .build();

        MarketPlaceListing savedListing = marketplaceListingRepository.save(newListing);
        log.info("Created new listing ID: {}", savedListing.getId());

        return buildListingResponse(savedListing);

    }

    private BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * availableOf ưu tiên field carbonCredit (được dùng như availableQuantity).
     * Nếu carbonCredit không có giá trị dương, fallback = amount - listedAmount.
     */
    private BigDecimal availableOf(CarbonCredit c) {
        BigDecimal direct = safe(c.getCarbonCredit());
        if (direct.compareTo(BigDecimal.ZERO) > 0) return direct;

        return safe(c.getAmount())
                .subtract(safe(c.getListedAmount()))
                .max(BigDecimal.ZERO);
    }


    @Override
    public List<MarketplaceListingResponse> getActiveListing() {
        List<MarketPlaceListing> activeListings = marketplaceListingRepository.findByStatusAndExpiresAtAfter(ListingStatus.AVAILABLE, LocalDate.now());

        return activeListings.stream()
                .map(this::buildListingResponse)
                .collect(Collectors.toList());
    }


    @Override
    public List<MarketplaceListingResponse> getALlCreditListingsByCompanyID() {

        User currentUser = currentUser();
        Company sellerCompany = currentCompany(currentUser);

        List<MarketPlaceListing> companyListings = marketplaceListingRepository.findByCompanyId(sellerCompany.getId());

        return companyListings.stream()
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

            BigDecimal updatedAvailable = creditBalance.add(remainingQuantity);
            carbonCredit.setCarbonCredit(updatedAvailable);

            BigDecimal updatedListed = listedAmount.subtract(remainingQuantity);
            if (updatedListed.compareTo(BigDecimal.ZERO) < 0) {
                updatedListed = BigDecimal.ZERO;
            }
            carbonCredit.setListedAmount(updatedListed);
            carbonCredit.setAmount(updatedAvailable.add(updatedListed));

            if (updatedListed.compareTo(BigDecimal.ZERO) > 0) {
                carbonCredit.setStatus(CreditStatus.LISTED);
            } else if (updatedAvailable.compareTo(BigDecimal.ZERO) > 0) {
                carbonCredit.setStatus(CreditStatus.AVAILABLE);
            } else {
                carbonCredit.setStatus(CreditStatus.RETIRED);
            }
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

        CarbonCredit carbonCredit = listing.getCarbonCredit();
        Project project = carbonCredit != null ? carbonCredit.getProject() : null;
        CreditBatch batch = carbonCredit != null ? carbonCredit.getBatch() : null;

        // Lấy thông tin số dư credit hiện tại
        CarbonCredit credit = listing.getCarbonCredit();
        BigDecimal remainingBalance = credit.getCarbonCredit() != null ?
                credit.getCarbonCredit() : BigDecimal.ZERO;
        BigDecimal totalListed = credit.getListedAmount() != null ?
                credit.getListedAmount() : BigDecimal.ZERO;

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
                // Thêm thông tin số dư
                .remainingCreditBalance(remainingBalance)
                .totalListedAmount(totalListed)
                .carbonCreditId(carbonCredit != null ? carbonCredit.getId() : null)
                .batchId(batch != null ? batch.getId() : null)
                .batchCode(batch != null ? batch.getBatchCode() : null)
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

    private CarbonCredit resolveOwnedCredit(Long creditId, Company company, BigDecimal requiredQuantity) {
        if (creditId == null) {
            throw new ResourceNotFoundException("Carbon credit block not found");
        }

        try {
            // B1: Tìm trực tiếp credit với credit id và companyID
            CarbonCredit directCredit = carbonCreditRepository.findByIdAndCompanyId(creditId, company.getId())
                    .orElse(null);

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

            // Nếu không tìm thấy credits nào, trả về lỗi cụ thể
            throw new AppException(ErrorCode.NO_AVAILABLE_CREDITS);

        } catch (DataAccessException e) {
            // Xử lý lỗi cơ sở dữ liệu
            log.error("Database error while searching for carbon credits: {}", e.getMessage());
            throw new AppException(ErrorCode.NO_AVAILABLE_CREDITS);
        }
    }

}

