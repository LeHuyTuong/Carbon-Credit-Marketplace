package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.dto.request.WalletTransactionRequest;
import com.carbonx.marketcarbon.dto.response.WalletCarbonCreditResponse;
import com.carbonx.marketcarbon.dto.response.WalletResponse;
import com.carbonx.marketcarbon.dto.response.WalletTransactionResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.exception.WalletException;
import com.carbonx.marketcarbon.model.*;
import com.carbonx.marketcarbon.repository.*;
import com.carbonx.marketcarbon.service.WalletService;
import com.carbonx.marketcarbon.service.WalletTransactionService;
import com.carbonx.marketcarbon.utils.CurrencyConverter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final WalletTransactionService walletTransactionService;
    private final WalletTransactionRepository walletTransactionRepository;
    private final CarbonCreditRepository carbonCreditRepository;
    private final CompanyRepository companyRepository;
    // Thêm EntityManager để quản lý locking
    private final EntityManager entityManager;

    private User currentUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if(user == null){
            throw new ResourceNotFoundException("User not found with email: " + email);
        }
        return user;
    }


    @Transactional
    public Wallet generateWallet(User user) {
        //B1 create wallet
        Wallet wallet = new  Wallet();
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setCarbonCreditBalance(BigDecimal.ZERO);
        //B2 Set user , moi vi duoc 1 user
        wallet.setUser(user);
        companyRepository.findByUserId(user.getId()).ifPresent(wallet::setCompany);
        //B3 luu lai vi theo user
        return walletRepository.save(wallet);
    }

    @Override
    @Transactional
    public WalletResponse getUserWallet() throws WalletException {
        // B1 Tim wallet
        User user = currentUser();
        Wallet wallet = walletRepository.findByUserId(user.getId());
        //B2 nếu không có ví thì sẽ tự gen ra ví
        if (wallet == null) {
            wallet = generateWallet(user);
        }
        //B3 khong co thi gen wallet
        // Fetch updated transactions DTOs
        List<WalletTransactionResponse> transactionDtos = walletTransactionService.getTransactionDtosForWallet(wallet.getId());
        // Map entity to DTO
        return mapToWalletResponse( transactionDtos);
    }

    @Override
    @Transactional
    public WalletResponse addBalanceToWallet( Long money) throws WalletException {
        // 1lấy số tiền hiện tại đang có trong ví
        User user = currentUser();
        Long id = user.getId();

        Wallet wallet = walletRepository.findByUserId(id);
        if(wallet == null){
            wallet = generateWallet(user);
        }

        BigDecimal amountUsd = BigDecimal.valueOf(money)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal amountToAddVnd = CurrencyConverter.usdToVnd(amountUsd); // Convert USD to VND

        String description = String.format("Add money to wallet (USD %s -> VND %s)",
                amountUsd.toPlainString(), amountToAddVnd.toPlainString());

        walletTransactionService.createTransaction(WalletTransactionRequest.builder()
                .wallet(wallet)
                .amount(amountUsd)
                .type(WalletTransactionType.ADD_MONEY)
                .description(description)
                .build());

        Wallet updatedWallet = walletRepository.findById(wallet.getId())
                .orElseThrow(() -> new WalletException("Wallet disappeared after transaction"));

        log.info("Balance added to wallet {}. Amount USD: {}, Amount VND: {}. New Balance: {}",
                wallet.getId(), amountUsd, amountToAddVnd, updatedWallet.getBalance());

        List<WalletTransactionResponse> transactionDtos = walletTransactionService.getTransactionDtosForWallet(updatedWallet.getId());
        // Map updated entity to DTO
        return mapToWalletResponse( transactionDtos);
    }


    @Override
    @Transactional
    public WalletResponse findWalletById(Long id) throws WalletException {
        Wallet wallet = walletRepository.findById(id)
                .orElseThrow(() -> new WalletException("Wallet not found with id: " + id));
        // Fetch transaction DTOs for the wallet
        List<WalletTransactionResponse> transactionDtos = walletTransactionService.getTransactionDtosForWallet(wallet.getId());
        // Map entity to DTO
        return mapToWalletResponse( transactionDtos);
    }

    @Override
    @Transactional // Read-only transaction for finding entity
    public Wallet findWalletEntityById(Long id) throws WalletException {
        return walletRepository.findById(id)
                .orElseThrow(() -> new WalletException("Wallet entity not found with id: " + id));
    }

    @Override
    public Wallet findWalletByUser(User user) {
        Wallet wallet = walletRepository.findByUserId(user.getId());
        return wallet;
    }

    // Helper method to map Wallet entity to WalletResponse DTO
    private WalletResponse mapToWalletResponse( List<WalletTransactionResponse> transactions) {
        User user = currentUser();
        Long id = user.getId();

        Wallet wallet = walletRepository.findByUserId(id);

        if (wallet == null) {
            return null;
        }
        List<WalletCarbonCreditResponse> creditSummaries = resolveCarbonCreditSummaries();

        return WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUser() != null ? wallet.getUser().getId() : null)
                .balance(wallet.getBalance())
                .carbonCreditBalance(wallet.getCarbonCreditBalance())
                .walletTransactions(transactions) // Use the passed DTO list
                .carbonCredits(creditSummaries)
                .build();
    }

    private List<WalletCarbonCreditResponse> resolveCarbonCreditSummaries() {
        User user = currentUser();
        Long id = user.getId();

        Wallet wallet = walletRepository.findByUserId(id);
        if (wallet == null) {
            return Collections.emptyList();
        }

        Company company = wallet.getCompany();
        if (company == null) {
            company = companyRepository.findByUserId(id).orElse(null);
            if (company == null) {
                return Collections.emptyList();
            }
            wallet.setCompany(company);
            walletRepository.save(wallet);
        }

        List<CarbonCredit> credits = carbonCreditRepository.findByCompanyId(company.getId());

        List<WalletCarbonCreditResponse> response = new ArrayList<>();

        for(CarbonCredit credit : credits) {
            CarbonCredit sourceCredit = credit.getSourceCredit();
            Company seller = sourceCredit != null ? sourceCredit.getCompany() : null;

            CarbonCredit originCredit = resolveRootCredit(credit);
            Company originCompany = null;

            if (originCredit != null) {
                originCompany = originCredit.getCompany();
            }
            if (originCompany == null && credit.getBatch() != null) {
                originCompany = credit.getBatch().getCompany();
            }

            // Ưu tiên lấy batch trực tiếp, nếu thiếu thì truy ngược theo chuỗi nguồn để tránh null
            CreditBatch batch = resolveEffectiveBatch(credit,sourceCredit,originCredit);
            Company batchCompany = null;
            if(batch != null) {
                batchCompany = batch.getCompany();
            }
            if(batchCompany == null && originCompany != null) {
                // fallback cuối cùng: dùng công ty gốc nếu batch không có thông tin
                batchCompany = originCompany;
            }

            // Số lượng tín chỉ thực sự thuộc về ví (đã mua + tự phát hành)
            BigDecimal ownedQuantity = credit.getAmount() != null
                    ? credit.getAmount()
                    : BigDecimal.ZERO;

            // Số lượng đang được niêm yết trên sàn bởi chính doanh nghiệp này
            BigDecimal listedQuantity = credit.getListedAmount() != null
                    ? credit.getListedAmount()
                    : BigDecimal.ZERO;

            // Lượng tín chỉ còn lại có thể sử dụng/niêm yết (không bao gồm phần đã list)
            BigDecimal availableQuantity = ownedQuantity.subtract(listedQuantity);
            if (availableQuantity.compareTo(BigDecimal.ZERO) < 0) {
                availableQuantity = BigDecimal.ZERO;
            }


            response.add(WalletCarbonCreditResponse.builder()
                    .creditId(credit.getId())
                    .creditCode(credit.getCreditCode())
                    .ownedQuantity(ownedQuantity)
                    .availableQuantity(availableQuantity)
                    .listedQuantity(listedQuantity)
                    .status(credit.getStatus())
                    .sellerCompanyId(seller != null ? seller.getId() : null)
                    .sellerCompanyName(seller != null ? seller.getCompanyName() : null)
                    .sourceCreditId(sourceCredit != null ? sourceCredit.getId() : null)
                    .sourceCreditCode(sourceCredit != null ? sourceCredit.getCreditCode() : null)
                    .originCreditId(originCredit != null ? originCredit.getId() : null)
                    .originCreditCode(originCredit != null ? originCredit.getCreditCode() : null)
                    .originCompanyId(originCompany != null ? originCompany.getId() : null)
                    .originCompanyName(originCompany != null ? originCompany.getCompanyName() : null)
                    .expirationDate(credit.getExpiryDate())
                    .batchId(batch != null ? batch.getId() : null)
                    .batchCode(batch != null ? batch.getBatchCode() : null)
                    .batchCompanyId(batch != null ? batch.getCompany().getId() : null)
                    .batchCompanyName(batch != null ? batch.getCompany().getCompanyName() : null)
                    .build());
        }
        return response;
    }

    /**
     * Lấy ra batch phù hợp nhất để giảm thiểu tình trạng dữ liệu null trả về cho FE.
     * Ưu tiên batch hiện tại, nếu không có thì dùng batch của credit nguồn hoặc credit gốc.
     */
    private CreditBatch resolveEffectiveBatch(CarbonCredit credit, CarbonCredit sourceCredit, CarbonCredit originCredit) {
        // batch trực tiếp
        CreditBatch batch = credit.getBatch();
        if (batch != null) {
            return batch;
        }

        // nếu credit phát sinh từ giao dịch, sử dụng batch của credit nguồn
        if (sourceCredit != null && sourceCredit.getBatch() != null) {
            return sourceCredit.getBatch();
        }

        // truy ngược tới credit gốc (trường hợp chuỗi nhiều cấp)
        if (originCredit != null && originCredit.getBatch() != null) {
            return originCredit.getBatch();
        }

        return null;
    }

    private CarbonCredit resolveRootCredit(CarbonCredit credit) {
        if (credit == null) {
            return null;
        }

        CarbonCredit current = credit;
        int depth = 0;

        while (current.getSourceCredit() != null) {
            current = current.getSourceCredit();
            depth++;
            if (depth > 20) {
                log.warn("Detected unusually deep carbon credit chain starting from credit {}",credit.getId());
                break;
            }
        }
        return current;
    }

    /**
     * Chuyển tiền (VND) giữa hai ví một cách an toàn (banking-grade).
     * Hàm này sử dụng PESSIMISTIC_WRITE (Khóa bi quan) để khóa cả hai ví
     * trong suốt quá trình giao dịch, ngăn chặn tuyệt đối lỗi race condition.
     *
     * @param fromWallet  Ví nguồn (chỉ dùng để lấy ID)
     * @param toWallet    Ví đích (chỉ dùng để lấy ID)
     * @param amount      Số tiền (luôn là số dương)
     * @param type        Loại giao dịch (dưới dạng String)
     * @param description Mô tả
     * @throws WalletException Nếu có lỗi (VD: không đủ tiền, khóa thất bại)
     */
    @Override
    @Transactional(rollbackOn = WalletException.class)
    public void transferFunds(Wallet fromWallet, Wallet toWallet, BigDecimal amount, String type, String description) throws WalletException {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Transfer amount must be positive. Amount: {}", amount);
            throw new AppException(ErrorCode.MONEY_MUST_POSITIVE);
        }

        try {
            // 1. Khóa và Tải ví nguồn (Sử dụng PESSIMISTIC_WRITE lock)
            // Tương đương "SELECT ... FOR UPDATE" trong SQL.
            // Luồng khác sẽ phải đợi nếu muốn tác động vào ví này.
            Wallet lockedFromWallet = entityManager.find(Wallet.class, fromWallet.getId(), LockModeType.PESSIMISTIC_WRITE);
            if (lockedFromWallet == null) {
                throw new AppException(ErrorCode.WALLET_NOT_FOUND);
            }

            // 2. Khóa và Tải ví đích
            Wallet lockedToWallet = entityManager.find(Wallet.class, toWallet.getId(), LockModeType.PESSIMISTIC_WRITE);
            if (lockedToWallet == null) {
                throw new AppException(ErrorCode.WALLET_NOT_FOUND);
            }

            // 3. Kiểm tra số dư trên ví đã bị khóa
            if (lockedFromWallet.getBalance().compareTo(amount) < 0) {
                log.warn("Insufficient funds for transfer. Wallet {} has {}, but {} is required.",
                        lockedFromWallet.getId(), lockedFromWallet.getBalance(), amount);
                throw new AppException(ErrorCode.WALLET_INSUFFICIENT_FUNDS);
            }

            // 4. Trừ tiền ví nguồn (Debit)
            lockedFromWallet.setBalance(lockedFromWallet.getBalance().subtract(amount));

            // 5. Cộng tiền ví đích (Credit)
            lockedToWallet.setBalance(lockedToWallet.getBalance().add(amount));

            // 6. Ghi log giao dịch (ví nguồn) - Tạo 2 bản ghi cho 2 bên
            WalletTransaction fromTransaction = WalletTransaction.builder()
                    .wallet(lockedFromWallet)
                    .amount(amount.negate()) // Ghi số âm cho giao dịch rút
                    // .currency(lockedFromWallet.getCurrency()) // Bỏ qua nếu entity WalletTransaction không có
                    .transactionType(WalletTransactionType.valueOf(type)) // Chuyển String sang Enum
                    .description(description)
                    // .status("COMPLETED") // Bỏ qua nếu entity không có
                    .createdAt(LocalDateTime.now()) // Dùng createdAt nếu entity có
                    .build();
            walletTransactionRepository.save(fromTransaction);

            // 7. Ghi log giao dịch (ví đích)
            WalletTransaction toTransaction = WalletTransaction.builder()
                    .wallet(lockedToWallet)
                    .amount(amount) // Ghi số dương cho giao dịch nạp
                    // .currency(lockedToWallet.getCurrency())
                    .transactionType(WalletTransactionType.valueOf(type))
                    .description(description)
                    // .status("COMPLETED")
                    .createdAt(LocalDateTime.now())
                    .build();
            walletTransactionRepository.save(toTransaction);

            // 8. Tự động commit
            // Khi phương thức kết thúc, @Transactional sẽ tự động commit các thay đổi
            // (save `lockedFromWallet`, `lockedToWallet`, và 2 transaction)
            // và giải phóng lock.
            log.info("Successfully transferred {} from wallet {} to wallet {}", amount, lockedFromWallet.getId(), lockedToWallet.getId());

        } catch (jakarta.persistence.PersistenceException e) {
            log.error("Database lock or persistence error during transfer: {}", e.getMessage(), e);
            throw new WalletException("Failed to acquire lock or persist transaction, transfer rolled back. " + e.getMessage());
        } catch (AppException e) {
            // Đẩy AppException (ví dụ: WALLET_INSUFFICIENT_FUNDS) ra ngoài
            throw e;
        } catch (Exception e) {
            // Bắt các lỗi khác (ví dụ: `WalletTransactionType.valueOf(type)` thất bại)
            log.error("Unexpected error during transfer: {}", e.getMessage(), e);
            throw new WalletException("Unexpected error during transfer: " + e.getMessage());
        }
    }
}
