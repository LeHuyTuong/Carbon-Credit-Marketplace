package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.certificate.CertificateData;
import com.carbonx.marketcarbon.certificate.CertificatePdfService;
import com.carbonx.marketcarbon.common.CreditStatus;
import com.carbonx.marketcarbon.dto.request.RetireBatchRequest;
import com.carbonx.marketcarbon.dto.response.CarbonCreditResponse;
import com.carbonx.marketcarbon.dto.response.CreditBatchLiteResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.*;
import com.carbonx.marketcarbon.repository.*;
import com.carbonx.marketcarbon.service.*;

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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;

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
    private final CreditCertificateRepository certificateRepo;
    private final CertificatePdfService certificatePdfService;
    private final EmailService emailService;
    private final SseService sseService;

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

    // hàm này giúp hệ thống check xem credit này đã hết hạn chưa
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

    /**
     * [COMPANY] Lấy danh sách tín chỉ có thể retire.
     * Điều kiện lọc:
     * - Không EXPIRED, không RETIRED
     * - Không đang niêm yết (listedAmount == 0)
     * - available (getAvailableAmount(credit)) > 0
     *
     * Invariant: amount = carbonCredit(available) + listedAmount.
     */
    @Override
    @PreAuthorize("hasRole('COMPANY')")
    public List<CarbonCreditResponse> getMyRetirableCredits() {
        Long companyId = currentCompanyId();
        log.info("[DEBUG] getMyRetirableCredits() - companyId={}", companyId);

        // B1 Tìm carbon theo compandy id
        return creditRepo.findByCompanyId(companyId).stream()
                .filter(credit -> {
                    // luôn phải cập nhật xem credit đã hết hạn chưa
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

    /**
     * [COMPANY] Retire một phần/hoặc toàn bộ quantity của credit theo id.
     *
     * - Không cho retire tín chỉ EXPIRED (CREDIT_EXPIRED)
     * - Không cho retire tín chỉ đang niêm yết (listedAmount > 0) (CREDIT_HAS_ACTIVE_LISTING)
     * - Không cho retire vượt quá available (AMOUNT_IS_NOT_ENOUGH)
     * - Khi remaining == 0, set status = RETIRED và listedAmount = 0 để đóng credit
     * - Ngược lại, giữ status = AVAILABLE (không chạm tới listedAmount)
     *
     * đồng bộ: dùng findByIdAndCompanyIdWithLock để khóa hàng, tránh race-condition khi nhiều request tới cùng credit.
     * Invariant: amount = carbonCredit(available) + listedAmount.
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('COMPANY')")
    public CarbonCreditResponse retireCredit(Long creditId, BigDecimal quantity) {
        //B1 tìm công ty hiện tại
        Long companyId = currentCompanyId();
        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
        log.info("[DEBUG] retireCredit() - creditId={}, companyId={}, quantity={}",
                creditId, companyId, quantity);

        //B2 check id credit hợp lệ không
        if (creditId == null || creditId.longValue() <= 0) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.AMOUNT_IS_NOT_VALID);
        }

        //Var cho phép trình biên dịch tự suy luận kiểu dữ liệu dựa trên giá trị được gán. java 10 >. đoán
        //Compile-time (chỉ trong lúc biên dịch)
        // optional Runtime (trong khi chương trình chạy)

        //B3 Tìm credit của công ty hiện tại (kèm lock ghi để đảm bảo tính nhất quán khi trừ số lượng)
        var creditOpt = creditRepo.findByIdAndCompanyIdWithLock(creditId, companyId);

        //B4 check credit
        if (creditOpt.isEmpty()) {
            if (creditRepo.existsById(creditId)) {  // tồn tại nhưng ko trong công ty
                throw new AppException(ErrorCode.COMPANY_NOT_OWN);
            }
            // không tồn lại
            throw new AppException(ErrorCode.CREDIT_NOT_FOUND);
        }

        //B5 Lấy ra credit từ optional và check ngày hết hạn
        CarbonCredit credit = creditOpt.get();
        checkAndMarkExpired(credit);

        // 5.1 nếu mà credit hết hạn thì lỗi
        if (credit.getStatus() == CreditStatus.EXPIRED) {
            throw new AppException(ErrorCode.CREDIT_EXPIRED);
        }

        // 5.2 nếu mà credit retired hoặc là số lượng nhỏ hơn bằng 0 thì lỗi
        if (credit.getStatus() == CreditStatus.RETIRED && credit.getCarbonCredit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.CREDIT_ALREADY_RETIRED);
        }

        //  5.3 Kiểm tra xem có đang niêm yết không
        BigDecimal listed = credit.getListedAmount() != null ? credit.getListedAmount() : BigDecimal.ZERO;
        if (listed.compareTo(BigDecimal.ZERO) > 0) {
            throw new AppException(ErrorCode.CREDIT_HAS_ACTIVE_LISTING);
        }

        // 5.4 kiểm tra phần khả dụng vaf trừ số lượng retire
        BigDecimal available = getAvailableAmount(credit);
        if (available.compareTo(quantity) < 0) {
            throw new AppException(ErrorCode.AMOUNT_IS_NOT_ENOUGH);
        }

        // 5.5 tính xem phần còn lại sau khi đã retire
        BigDecimal remaining = available.subtract(quantity);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }

        // B6 cập nhật lại credit
        credit.setCarbonCredit(remaining); // còn lại mới
        credit.setAmount(remaining.add(listed)); // tổng mới

        if (remaining.compareTo(BigDecimal.ZERO) == 0) {
            credit.setStatus(CreditStatus.RETIRED);
            credit.setListedAmount(BigDecimal.ZERO);
        } else {
            credit.setStatus(CreditStatus.AVAILABLE);
        }

        CarbonCredit saved = creditRepo.save(credit);

        // Gom toàn bộ xử lý chứng chỉ + thông báo sau khi retire thành helper riêng
        handleRetirementSuccess(saved, company, quantity);

        log.info("[DEBUG] retireCredit() - creditId={} retiredQuantity={}, remaining={}",
                saved.getId(), quantity, remaining);

        return CarbonCreditResponse.from(saved);
    }

    @Override
    public List<CarbonCreditResponse> retireCreditsFromBatch(RetireBatchRequest request) {
        return List.of();
    }

    /**
     * Hậu xử lý sau khi retire thành công:
     * - Gửi SSE thông báo tới user công ty (vẫn đang lỗi)
     * - Đảm bảo/tạo certificate cho batch tương ứng
     * - Render PDF certificate + upload, lưu URL
     * - Gửi email xác nhận kèm file PDF
     *
     * không roll back giao dịch chính nếu email/PDF fail
     */    private void handleRetirementSuccess(CarbonCredit credit, Company company, BigDecimal retiredQuantity) {
        if (credit == null || retiredQuantity == null) {
            return;
        }

        if (company != null && company.getUser() != null) {
            // Gửi thông báo realtime tới tài khoản doanh nghiệp
            String retiredAmount = retiredQuantity.stripTrailingZeros().toPlainString();
            String message = "You retired " + retiredAmount + " Carbon Credit" +
                    (retiredQuantity.compareTo(BigDecimal.ONE) > 0 ? "s" : "") +
                    " from credit " + (credit.getCreditCode() != null ? credit.getCreditCode() : credit.getId());
            sseService.sendNotificationToUser(company.getUser().getId(), message);
        }

        // Cần batch để có metadata chứng chỉ; nếu thiếu thì bỏ qua
        CreditBatch batch = credit.getBatch();
        if (batch == null) {
            log.warn("[RETIRE] Credit {} has no batch associated. Skipping certificate generation.", credit.getId());
            return;
        }

        try {
            // Đảm bảo luôn có chứng chỉ tương ứng với batch (tạo mới nếu chưa có)
            CreditCertificate cert = ensureCertificateForBatch(batch, company);
            Project project = credit.getProject() != null ? credit.getProject() : batch.getProject();
            EmissionReport report = batch.getReport();

            String validatedBy = "CVA Organization";
            if (report != null && report.getVerifiedAt() != null && report.getVerifiedByCva() != null) {
                validatedBy = report.getVerifiedByCva().getDisplayName();
            }

            // Số tín chỉ retire để hiển thị lên certificate
            int retiredCredits = safeIntValue(retiredQuantity);

            // Mặc định 1 tín chỉ ~ 1 tấn nếu thiếu dữ liệu
            BigDecimal perCreditTons = credit.getTCo2e() != null ? credit.getTCo2e() : BigDecimal.ONE;

            // Tổng tCO2e tương ứng lượng retire
            double totalTco2e = perCreditTons.multiply(retiredQuantity).doubleValue();

            // Lấy thông tin dải serial để render vào chứng chỉ
            String[] serialParts = resolveSerialRange(batch, credit);

            String projectTitle = project != null ? project.getTitle() : "Carbon Credit Project";
            int vintageYear = credit.getVintageYear() != null ? credit.getVintageYear()
                    : (batch.getVintageYear() != null ? batch.getVintageYear() : LocalDate.now().getYear());
            String batchCode = batch.getBatchCode() != null ? batch.getBatchCode()
                    : (credit.getCreditCode() != null ? credit.getCreditCode() : "N/A");
            String projectId = (project != null && project.getId() != null) ? "PRJ-" + project.getId() : null;

            // Build payload cho template PDF chứng chỉ retire
            CertificateData data = CertificateData.builder()
                    .creditsCount(retiredCredits)
                    .totalTco2e(totalTco2e)
                    .retired(true)
                    .projectTitle(projectTitle)
                    .companyName(company != null ? company.getCompanyName() : "")
                    .status("RETIRED")
                    .vintageYear(vintageYear)
                    .batchCode(batchCode)
                    .serialPrefix(serialParts[0])
                    .serialFrom(serialParts[1])
                    .serialTo(serialParts[2])
                    .certificateCode(cert.getCertificateCode())
                    .standard(cert.getStandard() != null ? cert.getStandard()
                            : "CarbonX Internal Registry • ISO 14064-2 & GHG Protocol")
                    .methodology(cert.getMethodology() != null ? cert.getMethodology()
                            : "EV Charging Emission Reduction Methodology v1.0")
                    .projectId(projectId)
                    .issuedAt(LocalDate.now().toString())
                    .issuerName("CarbonX Marketplace")
                    .issuerTitle("Authorized Signatory")
                    .issuerSignatureUrl("https://carbonx-storagee.s3.ap-southeast-2.amazonaws.com/ch%E1%BB%AF+k%C3%AD+CarbonX.jpg")
                    .leftLogoUrl("https://carbonx-storagee.s3.ap-southeast-2.amazonaws.com/carbonlogooo.jpg")
                    .rightLogoUrl("https://carbonx-storagee.s3.ap-southeast-2.amazonaws.com/carbonlogooo.jpg")
                    .verifiedBy(validatedBy)
                    .qrCodeUrl(cert.getQrCodeUrl())
                    .verifyUrl(cert.getVerifyUrl())
                    .perCreditTons(perCreditTons.intValue())
                    .build();

            // Sinh file PDF mới và upload lên dịch vụ lưu trữ
            StorageService.StoredObject stored = certificatePdfService.generateAndUploadPdf(data);
            String pdfUrl = stored.url();

            // Lưu URL PDF mới lên bản ghi chứng chỉ
            cert.setCertificateUrl(pdfUrl);
            certificateRepo.save(cert);

            // Tải file PDF để đính kèm vào email gửi khách hàng
            byte[] pdf = downloadPdf(pdfUrl);

            if (company != null && company.getUser() != null) {
                // Gửi email xác nhận retire kèm chứng chỉ PDF
                String subject = "Carbon Credit Retirement Confirmation";
                String projectName = projectTitle != null ? projectTitle : "your project";
                String retiredAmount = retiredQuantity.stripTrailingZeros().toPlainString();
                String htmlBody = """
                <div style='font-family:Arial,sans-serif;color:#333;'>
                  <h2 style='color:#16a34a;'>Credits Retired Successfully!</h2>
                  <p>Your company has retired <b>%s Carbon Credits</b> from project <b>%s</b>.</p>
                  <p>Certificate Code: <b>%s</b></p>
                  <p>You can <a href="%s" target="_blank">view/download the retirement certificate here</a>.</p>
                  <p>Best regards,<br><b>CarbonX Marketplace</b></p>
                </div>
                """.formatted(retiredAmount, projectName, cert.getCertificateCode(), pdfUrl);

                // Gửi email cùng file PDF đính kèm tới doanh nghiệp
                emailService.sendEmailWithAttachment(
                        company.getUser().getEmail(),
                        subject,
                        htmlBody,
                        pdf,
                        "CarbonX_Retirement_Certificate.pdf"
                );
            }
        } catch (Exception e) {
            log.error("[RETIRE] Failed to process retirement certificate/email for credit {}: {}",
                    credit.getId(), e.getMessage(), e);
        }
    }

    // Tìm chứng chỉ theo batch, nếu chưa có thì khởi tạo mặc định
    private CreditCertificate ensureCertificateForBatch(CreditBatch batch, Company company) {
        return certificateRepo.findByBatch_Id(batch.getId())
                .orElseGet(() -> {
                    String certificateCode = "CERT-" + batch.getBatchCode().replace("-", "") + "-" + System.currentTimeMillis();
                    String verifyUrl = "https://verify.carbonx.io/" + certificateCode;
                    String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=" +
                            URLEncoder.encode(verifyUrl, StandardCharsets.UTF_8);

                    CreditCertificate cert = CreditCertificate.builder()
                            .batch(batch)
                            .certificateCode(certificateCode)
                            .issuedTo(company != null ? company.getCompanyName() : null)
                            .issuedEmail(company != null && company.getUser() != null ? company.getUser().getEmail() : null)
                            .verifyUrl(verifyUrl)
                            .qrCodeUrl(qrCodeUrl)
                            .registry("CarbonX Internal Registry")
                            .standard("ISO 14064-2 aligned")
                            .methodology("EV Charging Emission Reduction Methodology v1.0")
                            .build();
                    return certificateRepo.save(cert);
                });
    }

    // Chuyển BigDecimal sang int cho template PDF (fallback nếu không phải số nguyên)
    private int safeIntValue(BigDecimal value) {
        if (value == null) {
            return 0;
        }
        try {
            return value.stripTrailingZeros().intValueExact();
        } catch (ArithmeticException ex) {
            log.warn("[RETIRE] Quantity {} is not an integer. Using floor value for certificate display.", value);
            return value.intValue();
        }
    }

    // Suy luận dải serial hiển thị trên chứng chỉ
    private String[] resolveSerialRange(CreditBatch batch, CarbonCredit credit) {
        String prefix = "";
        String from = "";
        String to = "";

        if (batch != null) {
            prefix = batch.getSerialPrefix() != null ? batch.getSerialPrefix() : "";
            from = batch.getSerialFrom() != null ? String.format("%06d", batch.getSerialFrom()) : "";
            to = batch.getSerialTo() != null ? String.format("%06d", batch.getSerialTo()) : "";
        } else if (credit.getCreditCode() != null) {
            String code = credit.getCreditCode();
            int lastDash = code.lastIndexOf('-');
            if (lastDash >= 0 && lastDash < code.length() - 1) {
                prefix = code.substring(0, lastDash + 1);
                String suffix = code.substring(lastDash + 1);
                from = suffix;
                to = suffix;
            } else {
                from = code;
                to = code;
            }
        }

        return new String[]{prefix, from, to};
    }

    // Tải nội dung PDF từ URL công khai để gắn vào email
    private byte[] downloadPdf(String pdfUrl) {
        try (InputStream in = new URL(pdfUrl).openStream()) {
            return in.readAllBytes();
        } catch (IOException ioEx) {
            throw new UncheckedIOException("Cannot download PDF from " + pdfUrl, ioEx);
        }
    }


}
