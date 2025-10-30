package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.certificate.CertificateData;
import com.carbonx.marketcarbon.certificate.CertificatePdfService;
import com.carbonx.marketcarbon.common.CreditStatus;
import com.carbonx.marketcarbon.common.EmissionStatus;
import com.carbonx.marketcarbon.common.WalletTransactionType;
import com.carbonx.marketcarbon.dto.response.CreditBatchResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.*;
import com.carbonx.marketcarbon.repository.*;
import com.carbonx.marketcarbon.service.CreditIssuanceService;
import com.carbonx.marketcarbon.service.EmailService;
import com.carbonx.marketcarbon.service.SseService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final CompanyRepository companyRepository;
    private final ProjectRepository projectRepository;
    private final CreditCertificateRepository certificateRepo;
    private final CertificatePdfService certificatePdfService;
    private final EmailService emailService;
    private final CreditFormula creditFormula;
    private final SerialNumberService serialSvc;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final SseService sseService;

    @Transactional
    @Override
    public CreditBatchResponse issueForReport(Long reportId) {
        EmissionReport report = reportRepo.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));

        if (report.getStatus() != EmissionStatus.ADMIN_APPROVED)
            throw new AppException(ErrorCode.REPORT_NOT_APPROVED);

        batchRepo.findByReportId(reportId)
                .ifPresent(b -> { throw new AppException(ErrorCode.CREDIT_ALREADY_ISSUED); });

        Company company = companyRepository.findById(report.getSeller().getId())
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
        Project project = projectRepository.findById(report.getProject().getId())
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        var result = creditFormula.compute(report, project);
        if (result.getCreditsCount() <= 0)
            throw new AppException(ErrorCode.CREDIT_QUANTITY_INVALID);

        int year = Integer.parseInt(report.getPeriod().substring(0, 4));
        String companyCode = CodeGenerator.slug3WithId(company.getCompanyName(), "COMP", company.getId());
        String projectCode = CodeGenerator.slug3WithId(project.getTitle(), "PRJ", project.getId());

        SerialRange range = serialSvc.allocate(project, company, year, result.getCreditsCount());
        String prefix = year + "-" + companyCode + "-" + projectCode + "-";
        String batchCode = prefix + String.format("%06d", range.from()) + "_" + String.format("%06d", range.to());

        String issuedBy = java.util.Optional
                .ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getName)
                .orElse("system@carbonx.com");

        LocalDateTime issuedAt = LocalDateTime.now();
        LocalDate expiresAt = issuedAt.toLocalDate().plusYears(1);

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
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();

        batch = batchRepo.save(batch);

        // Giai đoạn 1: Sinh các tín chỉ ở trạng thái ISSUED
        List<CarbonCredit> credits = new ArrayList<>(result.getCreditsCount());
        for (long s = range.from(); s <= range.to(); s++) {
            String code = serialSvc.buildCode(year, companyCode, projectCode, s);
            credits.add(CarbonCredit.builder()
                    .batch(batch)
                    .company(company)
                    .project(project)
                    .creditCode(code)
                    .vintageYear(year)
                    .status(CreditStatus.ISSUED)
                    .issuedBy(issuedBy)
                    .issuedAt(OffsetDateTime.now())
                    .expiryDate(LocalDate.now().plusYears(1))
                    .amount(BigDecimal.ONE)
                    .carbonCredit(BigDecimal.ONE)
                    .tCo2e(BigDecimal.ONE)
                    .name("Carbon Credit")
                    .currentPrice(0.0)
                    .build());
        }
        creditRepo.saveAll(credits);

        // Giai đoạn 2: Sau khi nạp vào ví, chuyển sang AVAILABLE
        credits.forEach(c -> c.setStatus(CreditStatus.AVAILABLE));
        creditRepo.saveAll(credits);

        // Cập nhật ví và tạo giao dịch
        Wallet wallet = walletRepository.findByCompany(company)
                .orElseGet(() -> {
                    Wallet existing = walletRepository.findByUserId(company.getUser().getId());
                    if (existing != null) return existing;

                    Wallet newWallet = Wallet.builder()
                            .company(company)
                            .user(company.getUser())
                            .balance(BigDecimal.ZERO)
                            .carbonCreditBalance(BigDecimal.ZERO)
                            .build();
                    return walletRepository.save(newWallet);
                });

        BigDecimal issuedCredits = BigDecimal.valueOf(result.getCreditsCount());
        BigDecimal before = wallet.getCarbonCreditBalance();
        BigDecimal after = before.add(issuedCredits);
        wallet.setCarbonCreditBalance(after);
        walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .transactionType(WalletTransactionType.ISSUE_CREDIT)
                .amount(issuedCredits)
                .balanceBefore(before)
                .balanceAfter(after)
                .creditBatch(batch)
                .description("Issued " + result.getCreditsCount() + " Carbon Credits for project " + project.getTitle())
                .createdAt(LocalDateTime.now())
                .build();
        walletTransactionRepository.save(tx);

        report.setStatus(EmissionStatus.CREDIT_ISSUED);
        reportRepo.save(report);

        log.info("Issued {} credits for company {} (project {})",
                result.getCreditsCount(), company.getCompanyName(), project.getTitle());


        String message = "Admin issue " +  result.getCreditsCount() + " to your company wallet" ;
        sseService.sendNotificationToUser(company.getUser().getId(), message);

        String certificateCode = "CERT-" + batch.getBatchCode().replace("-", "") + "-" + System.currentTimeMillis();

        CreditCertificate cert = CreditCertificate.builder()
                .batch(batch)
                .certificateCode(certificateCode)
                .issuedTo(company.getCompanyName())
                .issuedEmail(company.getUser().getEmail())
                .verifyUrl("https://verify.carbonx.io/" + certificateCode)
                .qrCodeUrl("https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=" +
                        java.net.URLEncoder.encode("https://verify.carbonx.io/" + certificateCode,
                                java.nio.charset.StandardCharsets.UTF_8))
                .registry("CarbonX Internal Registry")
                .standard("ISO 14064-2 aligned")
                .methodology("EV Charging Emission Reduction Methodology v1.0")
                .build();
        certificateRepo.save(cert);

        String validatedBy = "CVA Organization";
        if (report.getVerifiedAt() != null && report.getVerifiedByCva() != null)
            validatedBy = report.getVerifiedByCva().getDisplayName();

        CertificateData data = CertificateData.builder()
                .creditsCount(batch.getCreditsCount())
                .totalTco2e(result.getTotalTco2e().doubleValue())
                .retired(false)
                .projectTitle(project.getTitle())
                .companyName(company.getCompanyName())
                .status("ISSUED")
                .vintageYear(year)
                .batchCode(batchCode)
                .serialPrefix(prefix)
                .serialFrom(String.format("%06d", range.from()))
                .serialTo(String.format("%06d", range.to()))
                .certificateCode(certificateCode)
                .standard("CarbonX Internal Registry • ISO 14064-2 & GHG Protocol")
                .methodology("EV Charging Emission Reduction Methodology v1.0")
                .projectId("PRJ-" + project.getId())
                .issuedAt(batch.getIssuedAt().toLocalDate().toString())
                .issuerName("CarbonX Marketplace")
                .issuerTitle("Authorized Signatory")
                .issuerSignatureUrl("https://carbonx-storagee.s3.ap-southeast-2.amazonaws.com/ch%E1%BB%AF+k%C3%AD+CarbonX.jpg")
                .leftLogoUrl("https://carbonx-storagee.s3.ap-southeast-2.amazonaws.com/carbonlogooo.jpg")
                .rightLogoUrl("https://carbonx-storagee.s3.ap-southeast-2.amazonaws.com/carbonlogooo.jpg")
                .verifiedBy(validatedBy)
                .qrCodeUrl(cert.getQrCodeUrl())
                .verifyUrl(cert.getVerifyUrl())
                .build();

        byte[] pdf = certificatePdfService.generatePdf(data);

        try {
            String subject = "Your Carbon Credit Certificate is Ready!";
            String htmlBody = """
                    <div style='font-family:Arial,sans-serif;color:#333;'>
                      <h2 style='color:#16a34a;'>Congratulations, %s!</h2>
                      <p>Your company has been issued <b>%d Carbon Credits</b> for project <b>%s</b>.</p>
                      <p>Certificate Code: <b>%s</b></p>
                      <p>Best regards,<br><b>CarbonX Marketplace</b></p>
                    </div>
                    """.formatted(company.getCompanyName(), batch.getCreditsCount(),
                    project.getTitle(), certificateCode);

            emailService.sendEmailWithAttachment(
                    company.getUser().getEmail(),
                    subject,
                    htmlBody,
                    pdf,
                    "CarbonX_Certificate.pdf"
            );
        } catch (Exception e) {
            log.error("Failed to send certificate email: {}", e.getMessage());
        }

        return CreditBatchResponse.from(batch);
    }

    @Override
    public Page<CreditBatchResponse> listAllBatches(Pageable pageable) {
        return batchRepo.findAll(pageable).map(CreditBatchResponse::from);
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
        if (sourceCredit == null || sourceCredit.getId() == null)
            throw new AppException(ErrorCode.CREDIT_BATCH_NOT_FOUND);
        if (buyerCompany == null || buyerCompany.getId() == null)
            throw new AppException(ErrorCode.COMPANY_NOT_FOUND);
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0)
            throw new AppException(ErrorCode.CREDIT_QUANTITY_INVALID);

        Project project = sourceCredit.getProject();
        if (project == null || project.getId() == null)
            throw new AppException(ErrorCode.PROJECT_NOT_FOUND);

        int year = sourceCredit.getIssuedYear() != null ? sourceCredit.getIssuedYear() : OffsetDateTime.now().getYear();

        String companyCode = CodeGenerator.slug3WithId(buyerCompany.getCompanyName(), "COMP", buyerCompany.getId());
        String projectCode = CodeGenerator.slug3WithId(project.getTitle(), "PRJ", project.getId());

        SerialRange range = serialSvc.allocate(project, buyerCompany, year, 1);
        String creditCode = serialSvc.buildCode(year, companyCode, projectCode, range.from());
        String issuer = (issuedBy == null || issuedBy.isBlank()) ? "system@carbonx.com" : issuedBy;

        CarbonCredit newCredit = CarbonCredit.builder()
                .batch(sourceCredit.getBatch())
                .company(buyerCompany)
                .project(project)
                .sourceCredit(sourceCredit)
                .creditCode(creditCode)
                .status(CreditStatus.TRADED)
                .carbonCredit(quantity)
                .tCo2e(sourceCredit.getTCo2e())
                .amount(quantity)
                .name(sourceCredit.getName())
                .currentPrice(pricePerUnit != null ? pricePerUnit.doubleValue() : sourceCredit.getCurrentPrice())
                .vintageYear(sourceCredit.getVintageYear())
                .issuedAt(OffsetDateTime.now())
                .issuedBy(issuer)
                .build();

        return creditRepo.save(newCredit);
    }
}
