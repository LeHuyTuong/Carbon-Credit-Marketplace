package com.carbonx.marketcarbon.certificate;

import com.carbonx.marketcarbon.service.StorageService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.*;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificatePdfService {

    private final TemplateEngine templateEngine;
    private final ImageBase64Cache imageCache;
    private final StorageService storageService;

    public StorageService.StoredObject generateAndUploadPdf(CertificateData data) {
        try {

            Context context = new Context();
            Map<String, Object> vars = new HashMap<>();

            vars.put("creditsCount", data.getCreditsCount());
            vars.put("totalTco2e", data.getTotalTco2e());
            vars.put("projectTitle", data.getProjectTitle());
            vars.put("companyName", data.getCompanyName());
            vars.put("status", data.getStatus());
            vars.put("vintageYear", data.getVintageYear());
            vars.put("batchCode", data.getBatchCode());
            vars.put("serialPrefix", data.getSerialPrefix());
            vars.put("serialFrom", data.getSerialFrom());
            vars.put("serialTo", data.getSerialTo());
            vars.put("certificateCode", data.getCertificateCode());
            vars.put("standard", data.getStandard());
            vars.put("methodology", data.getMethodology());
            vars.put("projectId", data.getProjectId());
            vars.put("issuedAt", data.getIssuedAt());
            vars.put("issuerName", data.getIssuerName());
            vars.put("issuerTitle", data.getIssuerTitle());
            vars.put("verifiedBy", data.getVerifiedBy());
            vars.put("verifyUrl", data.getVerifyUrl());
            vars.put("qrCodeUrl", data.getQrCodeUrl());

            // Convert hình ảnh sang Base64 để nhúng vào PDF
            vars.put("issuerSignatureUrl", imageCache.get(data.getIssuerSignatureUrl()));
            vars.put("leftLogoUrl", imageCache.get(data.getLeftLogoUrl()));
            vars.put("rightLogoUrl", imageCache.get(data.getRightLogoUrl()));

            context.setVariables(vars);
            String htmlContent = templateEngine.process("certificate-retire-green.html", context);


            byte[] pdfBytes;
            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                builder.withHtmlContent(htmlContent, new File("src/main/resources/templates/").toURI().toString());
                builder.toStream(os);
                builder.run();
                pdfBytes = os.toByteArray();
            }


            String key = buildFileKey(data);
            try (InputStream in = new ByteArrayInputStream(pdfBytes)) {
                StorageService.StoredObject stored = storageService.upload(
                        key,
                        "application/pdf",
                        pdfBytes.length,
                        in
                );
                log.info(" Uploaded certificate PDF to S3:{}", stored.url());
                return stored;
            }

        } catch (Exception e) {
            log.error(" Error generating/uploading PDF certificate: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate and upload PDF certificate", e);
        }
    }

    private String buildFileKey(CertificateData data) {
        String safeCode = data.getCertificateCode() != null ? data.getCertificateCode() : "unknown";
        String timestamp = LocalDateTime.now().toString().replace(":", "-");
        return "certificates/" + safeCode + "-" + timestamp + ".pdf";
    }

    private String encodeImageToBase64(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return null;
        try (InputStream in = new URL(imageUrl).openStream()) {
            byte[] imageBytes = in.readAllBytes();
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            String type = imageUrl.toLowerCase().endsWith(".png") ? "png" : "jpeg";
            return "data:image/" + type + ";base64," + base64;
        } catch (Exception e) {
            log.warn(" Cannot load image from URL: {}", imageUrl);
            return null;
        }
    }
}
