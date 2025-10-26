package com.carbonx.marketcarbon.certificate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CertificateData {
    private int creditsCount;
    private double totalTco2e;
    private boolean retired;

    private String projectTitle;
    private String companyName;
    private String status;

    private int vintageYear;
    private String batchCode;
    private String serialPrefix;
    private String serialFrom;
    private String serialTo;
    private String certificateCode;

    private String standard;
    private String methodology;
    private String projectId;

    private String issuedAt;
    private String issuerName;
    private String issuerTitle;
    private String issuerSignatureUrl;
    private String leftLogoUrl;
    private String rightLogoUrl;

    private String verifiedBy;
    private String qrCodeUrl;
    private String verifyUrl;

    private String beneficiaryName;

    private int perCreditTons;
}
