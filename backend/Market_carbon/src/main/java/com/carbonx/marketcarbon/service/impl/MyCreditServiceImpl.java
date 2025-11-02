package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.CreditStatus;
import com.carbonx.marketcarbon.dto.response.CarbonCreditResponse;
import com.carbonx.marketcarbon.dto.response.CreditBatchLiteResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.CarbonCredit;
import com.carbonx.marketcarbon.model.CreditBatch;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.*;
import com.carbonx.marketcarbon.service.CreditQuery;
import com.carbonx.marketcarbon.service.MyCreditService;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MyCreditServiceImpl implements MyCreditService {

    private final CarbonCreditRepository creditRepo;
    private final CreditBatchRepository batchRepo;
    private final CompanyRepository companyRepo;
    private final UserRepository userRepo;
    private final WalletRepository walletRepo;

    private Long currentCompanyId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("[DEBUG] Authenticated email = {}", email);

        User user = userRepo.findByEmail(email);
        if (user == null) {
            log.error("[DEBUG] User not found for email {}", email);
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        log.info("[DEBUG] Found user.id = {}", user.getId());

        var companyOpt = companyRepo.findByUserId(user.getId());
        if (companyOpt.isEmpty()) {
            log.error("[DEBUG] No company found for user.id = {}", user.getId());
            throw new AppException(ErrorCode.COMPANY_NOT_FOUND);
        }

        var company = companyOpt.get();
        log.info("[DEBUG] Found company.id = {}, name = {}", company.getId(), company.getCompanyName());
        return company.getId();
    }

    private void checkAndMarkExpired(CarbonCredit credit) {
        if (credit.getExpiryDate() != null &&
                credit.getExpiryDate().isBefore(LocalDate.now()) &&
                credit.getStatus() != CreditStatus.EXPIRED) {

            credit.setStatus(CreditStatus.EXPIRED);
            creditRepo.save(credit);
            log.info("[AUTO-EXPIRE] Credit {} marked as EXPIRED (expiryDate={})",
                    credit.getCreditCode(), credit.getExpiryDate());
        }
    }

    @Override
    @PreAuthorize("hasRole('COMPANY')")
    public List<CarbonCreditResponse> listMyCredits(CreditQuery q) {
        Long companyId = currentCompanyId();
        log.info("[DEBUG] listMyCredits() - companyId={}, q={}", companyId, q);

        Specification<CarbonCredit> spec = (root, cq, cb) -> {
            cq.distinct(true); // tránh nhân bản bản ghi do JOIN

            var predicates = new ArrayList<Predicate>();

            // JOIN hợp lệ
            var companyJoin = root.join("company", JoinType.LEFT);
            var sourceJoin  = root.join("sourceCredit", JoinType.LEFT);

            // Quyền sở hữu: trực tiếp hoặc thông qua sourceCredit
            Predicate ownsDirectly  = cb.equal(companyJoin.get("id"), companyId);
            Predicate ownsViaSource = cb.equal(sourceJoin.get("company").get("id"), companyId);
            predicates.add(cb.or(ownsDirectly, ownsViaSource));

            // --- Bộ lọc tùy chọn ---
            if (q != null) {
                if (q.projectId() != null) {
                    predicates.add(cb.equal(root.get("project").get("id"), q.projectId()));
                }
                if (q.vintageYear() != null) {
                    predicates.add(cb.equal(root.join("batch", JoinType.LEFT).get("vintageYear"), q.vintageYear()));
                }
                if (q.status() != null) {
                    predicates.add(cb.equal(root.get("status"), q.status()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 🔹 Lấy toàn bộ danh sách (không phân trang)
        List<CarbonCredit> credits = creditRepo.findAll(spec);
        log.info("[DEBUG] Credits found = {}", credits.size());

        credits.forEach(this::checkAndMarkExpired);
        return credits.stream()
                .map(CarbonCreditResponse::from)
                .toList();
    }



    @Override
    @PreAuthorize("hasRole('COMPANY')")
    public Page<CreditBatchLiteResponse> listMyBatches(Long projectId, Integer vintageYear, Pageable pageable) {
        Long companyId = currentCompanyId();
        log.info("[DEBUG] listMyBatches() - companyId={}, projectId={}, vintageYear={}",
                companyId, projectId, vintageYear);

        Specification<CreditBatch> spec = (root, cq, cb) -> {
            cq.distinct(true);
            var predicates = new ArrayList<Predicate>();

            var companyJoin = root.join("company", JoinType.INNER);
            predicates.add(cb.equal(companyJoin.get("id"), companyId));

            if (projectId != null) {
                predicates.add(cb.equal(root.join("project", JoinType.INNER).get("id"), projectId));
            }
            if (vintageYear != null) {
                predicates.add(cb.equal(root.get("vintageYear"), vintageYear));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<CreditBatch> batches = batchRepo.findAll(spec, pageable);
        log.info("[DEBUG] Batches found = {}", batches.getTotalElements());

        batches.getContent().forEach(b ->
                log.debug("[DEBUG] BatchRow => id={}, projectId={}, companyId={}",
                        b.getId(),
                        b.getProject() != null ? b.getProject().getId() : null,
                        b.getCompany() != null ? b.getCompany().getId() : null)
        );

        return batches.map(CreditBatchLiteResponse::from);
    }

    @Override
    public CarbonCreditResponse getMyCreditById(Long creditId) {
        Long companyId = currentCompanyId();
        log.info("[DEBUG] getMyCreditById() - creditId={}, companyId={}", creditId, companyId);

        CarbonCredit credit = creditRepo.findById(creditId)
                .orElseThrow(() -> new AppException(ErrorCode.CREDIT_NOT_FOUND));

        if (!credit.getCompany().getId().equals(companyId)) {
            log.error("[DEBUG] Access denied! Credit belongs to company {} not {}", credit.getCompany().getId(), companyId);
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        checkAndMarkExpired(credit);
        return CarbonCreditResponse.from(credit);
    }

    @Override
    public CarbonCreditResponse getMyCreditByCode(String creditCode) {
        Long companyId = currentCompanyId();
        log.info("[DEBUG] getMyCreditByCode() - code={}, companyId={}", creditCode, companyId);

        CarbonCredit credit = creditRepo.findByCreditCodeAndCompany_Id(creditCode, companyId)
                .orElseThrow(() -> new AppException(ErrorCode.CREDIT_NOT_FOUND));

        checkAndMarkExpired(credit);
        return CarbonCreditResponse.from(credit);
    }

    @Override
    @PreAuthorize("hasRole('COMPANY')")
    public List<CarbonCreditResponse> getMyCreditsByBatchId(Long batchId) {
        Long companyId = currentCompanyId();
        log.info("[DEBUG] getMyCreditsByBatchId() - batchId={}, companyId={}", batchId, companyId);

        var credits = creditRepo.findByBatch_IdAndCompany_Id(batchId, companyId);
        if (credits.isEmpty()) {
            log.warn("[DEBUG] No credits found for batchId={} and companyId={}", batchId, companyId);
        }

        credits.forEach(this::checkAndMarkExpired);
        return credits.stream()
                .map(CarbonCreditResponse::from)
                .toList();
    }

    private BigDecimal getAvailableAmount(CarbonCredit credit) {
        if (credit == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal available = credit.getCarbonCredit();
        if (available != null && available.compareTo(BigDecimal.ZERO) > 0) {
            return available;
        }

        BigDecimal amount = credit.getAmount();
        BigDecimal listed = credit.getListedAmount() != null ? credit.getListedAmount() : BigDecimal.ZERO;
        if (amount != null) {
            BigDecimal remaining = amount.subtract(listed);
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                return remaining;
            }
        }

        return BigDecimal.ZERO;
    }

    @Override
    @PreAuthorize("hasRole('COMPANY')")
    public List<CarbonCreditResponse> getMyRetirableCredits() {
        Long companyId = currentCompanyId();
        log.info("[DEBUG] getMyRetirableCredits() - companyId={}", companyId);

        // B1 Tìm carbon theo compandy id
        return creditRepo.findByCompanyId(companyId).stream()
                .filter(credit -> {
                    checkAndMarkExpired(credit);

                    // 1.1 lọc những tín chỉ hết han hoặc đã retire
                    if (credit.getStatus() == CreditStatus.EXPIRED || credit.getStatus() == CreditStatus.RETIRED) {
                        return false;
                    }

                    // lọc nhuwnxg thằng nào đã list
                    BigDecimal listed = credit.getListedAmount() != null ? credit.getListedAmount() : BigDecimal.ZERO;
                    // nếu list nhỏ hơn = không và hiện ang có lớn hơn 0 thì return true
                    return listed.compareTo(BigDecimal.ZERO) <= 0 && getAvailableAmount(credit).compareTo(BigDecimal.ZERO) > 0;
                })
                .map(CarbonCreditResponse::from)
                .toList();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('COMPANY')")
    public CarbonCreditResponse retireCredit(Long creditId, BigDecimal quantity) {
        Long companyId = currentCompanyId();
        log.info("[DEBUG] retireCredit() - creditId={}, companyId={}, quantity={}",
                creditId, companyId, quantity);

        if (creditId == null || creditId.longValue() <= 0) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.AMOUNT_IS_NOT_VALID);
        }

        var creditOpt = creditRepo.findByIdAndCompanyIdWithLock(creditId, companyId);
        if (creditOpt.isEmpty()) {
            if (creditRepo.existsById(creditId)) {
                throw new AppException(ErrorCode.COMPANY_NOT_OWN);
            }
            throw new AppException(ErrorCode.CREDIT_NOT_FOUND);
        }
        CarbonCredit credit = creditOpt.get();

        checkAndMarkExpired(credit);

        if (credit.getStatus() == CreditStatus.EXPIRED) {
            throw new AppException(ErrorCode.CREDIT_EXPIRED);
        }
        if (credit.getStatus() == CreditStatus.RETIRED) {
            throw new AppException(ErrorCode.CREDIT_ALREADY_RETIRED);
        }

        BigDecimal listed = credit.getListedAmount() != null ? credit.getListedAmount() : BigDecimal.ZERO;
        if (listed.compareTo(BigDecimal.ZERO) > 0) {
            throw new AppException(ErrorCode.CREDIT_HAS_ACTIVE_LISTING);
        }

        BigDecimal available = getAvailableAmount(credit);
        if (available.compareTo(quantity) < 0) {
            throw new AppException(ErrorCode.AMOUNT_IS_NOT_ENOUGH);
        }

        BigDecimal remaining = available.subtract(quantity);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }

        credit.setCarbonCredit(remaining);
        credit.setAmount(remaining.add(listed));

        if (remaining.compareTo(BigDecimal.ZERO) == 0) {
            credit.setStatus(CreditStatus.RETIRED);
            credit.setListedAmount(BigDecimal.ZERO);
        }

        CarbonCredit saved = creditRepo.save(credit);
        log.info("[DEBUG] retireCredit() - creditId={} retiredQuantity={}, remaining={}",
                saved.getId(), quantity, remaining);

        return CarbonCreditResponse.from(saved);
    }
}
