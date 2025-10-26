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

import java.util.ArrayList;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
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
        Long id = companyRepo.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND))
                .getId();
        return id;
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
        log.info("[DEBUG] listMyBatches START - projectId={}, vintageYear={}, pageable={}", projectId, vintageYear, pageable);

        Long companyId = currentCompanyId();
        log.info("[DEBUG] Authenticated companyId = {}", companyId);

        Specification<CreditBatch> spec = (root, cq, cb) -> {
            cq.distinct(true);

            // JOIN tường minh để chắc chắn ràng buộc dính vào SQL
            var companyJoin  = root.join("company", JoinType.INNER);
            var projectJoin  = root.join("project", JoinType.INNER);

            var predicates = new ArrayList<Predicate>();
            //chỉ batch của công ty đang đăng nhập
            predicates.add(cb.equal(companyJoin.get("id"), companyId));
            log.info("[DEBUG] Applied filter companyId = {}", companyId);

            if (projectId != null) {
                predicates.add(cb.equal(projectJoin.get("id"), projectId));
                log.info("[DEBUG] Applied filter projectId = {}", projectId);
            }
            if (vintageYear != null) {
                predicates.add(cb.equal(root.get("vintageYear"), vintageYear));
                log.info("[DEBUG] Applied filter vintageYear = {}", vintageYear);
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<CreditBatch> batches = batchRepo.findAll(spec, pageable);

        log.info("[DEBUG] Result: totalElements={}, totalPages={}, page={}",
                batches.getTotalElements(), batches.getTotalPages(), batches.getNumber());

        // Log kiểm chứng từng record thật sự khớp filter
        batches.getContent().forEach(b ->
                log.debug("[DEBUG] Row => id={}, projectId={}, companyId={}",
                        b.getId(), b.getProject().getId(), b.getCompany().getId())
        );

        return batches.map(CreditBatchLiteResponse::from);
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
