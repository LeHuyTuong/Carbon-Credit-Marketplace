package com.carbonx.marketcarbon.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class ProjectSubmitRequest {
    @NotNull
    private Long baseProjectId; // Dự án mà công ty tham gia

    @NotBlank
    private String companyCommitment; // Cam kết cụ thể của doanh nghiệp

    @NotBlank
    private String technicalIndicators; // Chỉ số kỹ thuật thực tế của doanh nghiệp

    @NotBlank
    private String measurementMethod; // Phương pháp đo lường phát thải hoặc giảm phát thải
}