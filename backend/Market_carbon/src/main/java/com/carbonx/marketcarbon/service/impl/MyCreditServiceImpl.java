package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.dto.response.CarbonCreditResponse;
import com.carbonx.marketcarbon.dto.response.CreditBatchLiteResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.CarbonCredit;
import com.carbonx.marketcarbon.model.CreditBatch;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.CarbonCreditRepository;
import com.carbonx.marketcarbon.repository.CompanyRepository;
import com.carbonx.marketcarbon.repository.CreditBatchRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.service.CreditQuery;
import com.carbonx.marketcarbon.service.MyCreditService;

import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service cho phép COMPANY xem các tín chỉ đã được cấp, lọc theo dự án, vintage, trạng thái.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyCreditServiceImpl implements MyCreditService {

    private final CarbonCreditRepository creditRepo;
    private final CreditBatchRepository batchRepo;
    private final CompanyRepository companyRepo;
    private final UserRepository userRepo;


    private Long currentCompanyId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepo.findByEmail(email);
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return companyRepo.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND))
                .getId();
    }


    @Override
    @PreAuthorize("hasRole('COMPANY')")
    public Page<CarbonCreditResponse> listMyCredits(CreditQuery q, Pageable pageable) {
        Long companyId = currentCompanyId();

        Specification<CarbonCredit> spec = (root, cq, cb) -> {
            var predicate = cb.conjunction();
            predicate.getExpressions().add(cb.equal(root.get("company").get("id"), companyId));

            if (q != null) {
                if (q.projectId() != null) {
                    predicate.getExpressions().add(cb.equal(root.get("project").get("id"), q.projectId()));
                }
                if (q.vintageYear() != null) {
                    predicate.getExpressions().add(
                            cb.equal(root.join("batch", JoinType.LEFT).get("vintageYear"), q.vintageYear())
                    );
                }
                if (q.status() != null) {
                    predicate.getExpressions().add(cb.equal(root.get("status"), q.status()));
                }
            }

            return predicate;
        };

        return creditRepo.findAll(spec, pageable).map(CarbonCreditResponse::from);
    }


    @Override
    @PreAuthorize("hasRole('COMPANY')")
    public Page<CreditBatchLiteResponse> listMyBatches(Long projectId, Integer vintageYear, Pageable pageable) {
        Long companyId = currentCompanyId();

        Specification<CreditBatch> spec = (root, cq, cb) -> {
            var predicate = cb.conjunction();
            predicate.getExpressions().add(cb.equal(root.get("company").get("id"), companyId));

            if (projectId != null) {
                predicate.getExpressions().add(cb.equal(root.get("project").get("id"), projectId));
            }
            if (vintageYear != null) {
                predicate.getExpressions().add(cb.equal(root.get("vintageYear"), vintageYear));
            }

            return predicate;
        };

        return batchRepo.findAll(spec, pageable).map(CreditBatchLiteResponse::from);
    }

    @Override
    @PreAuthorize("hasRole('COMPANY')")
    public CarbonCreditResponse getMyCreditById(Long creditId) {
        Long companyId = currentCompanyId();

        CarbonCredit credit = creditRepo.findById(creditId)
                .orElseThrow(() -> new AppException(ErrorCode.CREDIT_NOT_FOUND));

        if (!credit.getCompany().getId().equals(companyId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return CarbonCreditResponse.from(credit);
    }

    @Override
    @PreAuthorize("hasRole('COMPANY')")
    public CarbonCreditResponse getMyCreditByCode(String creditCode) {
        Long companyId = currentCompanyId();

        CarbonCredit credit = creditRepo.findByCreditCodeAndCompany_Id(creditCode, companyId)
                .orElseThrow(() -> new AppException(ErrorCode.CREDIT_NOT_FOUND));

        return CarbonCreditResponse.from(credit);
    }
}
