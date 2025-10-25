package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.CreditStatus;
import com.carbonx.marketcarbon.common.EmissionStatus;
import com.carbonx.marketcarbon.dto.response.CreditBatchResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.*;
import com.carbonx.marketcarbon.repository.*;
import com.carbonx.marketcarbon.service.CreditIssuanceService;
import com.carbonx.marketcarbon.service.credit.SerialNumberService;
import com.carbonx.marketcarbon.service.credit.SerialNumberService.SerialRange;
import com.carbonx.marketcarbon.service.credit.formula.CreditFormula;
import com.carbonx.marketcarbon.utils.CodeGenerator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditIssuanceServiceImpl implements CreditIssuanceService {

    private final EmissionReportRepository reportRepo;
    private final CreditBatchRepository batchRepo;
    private final CarbonCreditRepository creditRepo;
    private final UserRepository userRepo;

    private final CreditFormula creditFormula;
    private final SerialNumberService serialSvc;

    @Transactional
    @Override
    public CreditBatchResponse issueForReport(Long reportId) {
        EmissionReport report = reportRepo.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));

        if (report.getStatus() != EmissionStatus.ADMIN_APPROVED) {
            throw new AppException(ErrorCode.REPORT_NOT_APPROVED);
        }

        batchRepo.findByReportId(reportId).ifPresent(b -> { throw new AppException(ErrorCode.CREDIT_ALREADY_ISSUED); });

        // Tải lại company/project để chắc chắn row tồn tại trong schema hiện hành
        Company company = report.getSeller();
        Project project = report.getProject();

        // Nếu bạn từng gặp lỗi FK, re-load từ repo để đảm bảo attached + đúng id:
        // company = companyRepository.findById(company.getId()).orElseThrow(...);
        // project = projectRepository.findById(project.getId()).orElseThrow(...);

        var result = creditFormula.compute(report, project);
        if (result.getCreditsCount() <= 0) throw new AppException(ErrorCode.CREDIT_QUANTITY_INVALID);

        int year = Integer.parseInt(report.getPeriod().substring(0, 4));

        // Dùng slug chuyên nghiệp từ name/title + ID
        String companyCode = com.carbonx.marketcarbon.utils.CodeGenerator.slug3WithId(
                company.getCompanyName(), "COMP", company.getId());
        String projectCode = com.carbonx.marketcarbon.utils.CodeGenerator.slug3WithId(
                project.getTitle(), "PRJ", project.getId());

        SerialRange range = serialSvc.allocate(project, company, year, result.getCreditsCount());
        String prefix = year + "-" + companyCode + "-" + projectCode + "-";
        String batchCode = prefix + String.format("%06d", range.from()) + "_" + String.format("%06d", range.to());

        String issuedBy = java.util.Optional
                .ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getName)
                .orElse("system@carbonx.com");

        CreditBatch batch = CreditBatch.builder()
                .report(report)
                .company(company)
                .project(project)
                .batchCode(batchCode)
                .totalTco2e(result.getTotalTco2e())
                .creditsCount(result.getCreditsCount())
                .residualTco2e(result.getResidualTco2e())
                .vintageYear(year)
                .serialPrefix(prefix)
                .serialFrom(range.from())
                .serialTo(range.to())
                .status("ISSUED")
                .issuedBy(issuedBy)
                .issuedAt(OffsetDateTime.now())
                .build();

        batch = batchRepo.save(batch);

        // Sinh credits (các default đã set ở entity)
        List<CarbonCredit> credits = new ArrayList<>(result.getCreditsCount());
        for (long s = range.from(); s <= range.to(); s++) {
            String code = serialSvc.buildCode(year, companyCode, projectCode, s);
            credits.add(CarbonCredit.builder()
                    .batch(batch)
                    .company(company)
                    .project(project)
                    .creditCode(code)
                    .status(CreditStatus.AVAILABLE)
                    .issuedBy(issuedBy)
                    .issuedAt(OffsetDateTime.now())
                    .build());
        }
        creditRepo.saveAll(credits);

        report.setStatus(EmissionStatus.CREDIT_ISSUED);
        reportRepo.save(report);

        log.info("Issued {} credits for company {} (project {})",
                result.getCreditsCount(), company.getCompanyName(), project.getTitle());

        return CreditBatchResponse.from(batch);
    }

    @Override
    public Page<CreditBatchResponse> listAllBatches(Pageable pageable) {
        return batchRepo.findAll(pageable)
                .map(CreditBatchResponse::from);
    }

    @Override
    public CreditBatchResponse getBatchById(Long batchId) {
        CreditBatch b = batchRepo.findById(batchId)
                .orElseThrow(() -> new AppException(ErrorCode.CREDIT_BATCH_NOT_FOUND));
        return CreditBatchResponse.from(b);
    }

    @Override
    @Transactional
    public CarbonCredit issueTradeCredit(CarbonCredit sourceCredit, Company buyerCompany, BigDecimal quantity, BigDecimal pricePerUnit, String issuedBy) {
        if(sourceCredit == null || sourceCredit.getId() == null){
            throw new AppException(ErrorCode.CREDIT_BATCH_NOT_FOUND);
        }
        if(buyerCompany == null || buyerCompany.getId() == null){
            throw new AppException(ErrorCode.COMPANY_NOT_FOUND);
        }
        if(quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0){
            throw new AppException(ErrorCode.CREDIT_QUANTITY_INVALID);
        }

        // lấy project từ credit
        Project project = sourceCredit.getProject();
        if(project == null || project.getId() == null){
            throw new AppException(ErrorCode.PROJECT_NOT_FOUND);
        }

        int year = sourceCredit.getIssuedYear() != null ? sourceCredit.getIssuedYear() : OffsetDateTime.now().getYear();

        // gen companyCode với slug3
        String companyCode = CodeGenerator.slug3WithId(
                buyerCompany.getCompanyName(),"COMP",buyerCompany.getId());
        // gen projectCode với slug3
        String projectCode = CodeGenerator.slug3WithId(
                project.getTitle(),"PRJ",project.getId());

        SerialRange range = serialSvc.allocate(project, buyerCompany, year,1);

        String creditCode = serialSvc.buildCode(year,companyCode,projectCode,range.from());

        String issuer = (issuedBy == null || issuedBy.isBlank()) ? "system@carbon.com" : issuedBy;

        CarbonCredit newCredit = CarbonCredit.builder()
                .batch(sourceCredit.getBatch())
                .company(buyerCompany)
                .project(project)
                .sourceCredit(sourceCredit)
                .creditCode(creditCode)
                .status(CreditStatus.ISSUE)
                .carbonCredit(quantity)
                .tCo2e(sourceCredit.getTCo2e())
                .amount(quantity)
                .name(sourceCredit.getName())
                .currentPrice(pricePerUnit != null ? pricePerUnit.doubleValue()
                        :sourceCredit.getCurrentPrice())
                .vintageYear(sourceCredit.getVintageYear())
                .issuedAt(OffsetDateTime.now())
                .issuedBy(issuer)
                .build();

        return creditRepo.save(newCredit);

    }
}
