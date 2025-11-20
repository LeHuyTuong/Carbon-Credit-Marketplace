package com.carbonx.marketcarbon.dto.request;

import com.carbonx.marketcarbon.common.ProjectStatus;
import com.carbonx.marketcarbon.common.validator.FileSize;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request dành riêng cho UPDATE Project
 * - KHÔNG bắt buộc bất kỳ field nào
 * - File logo & legalDocsFile là OPTIONAL
 * - Chỉ update field nào FE gửi lên
 */
@Data
public class ProjectUpdateRequest {

    private String title;

    private String description;

    private String commitments;

    private String technicalIndicators;

    private String measurementMethod;

    @FileSize(max = 55242880, required = false, message = "Logo must not exceed 50MB")
    private MultipartFile logo;   // optional

    @FileSize(max = 55242880, required = false, message = "Legal documents must not exceed 50MB")
    private MultipartFile legalDocsFile;  // optional

    private BigDecimal emissionFactorKgPerKwh;

    private ProjectStatus status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startedDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}
