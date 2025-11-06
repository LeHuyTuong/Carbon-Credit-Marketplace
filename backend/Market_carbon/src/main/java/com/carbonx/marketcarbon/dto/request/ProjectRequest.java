package com.carbonx.marketcarbon.dto.request;

import com.carbonx.marketcarbon.common.ProjectStatus;
import com.carbonx.marketcarbon.common.validator.FileSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;


@Data
    public class ProjectRequest {

        @NotBlank(message = "Title must not be blank")
        @Size(max = 100, message = "Title must be at most 100 characters")
        private String title;

        @NotBlank(message = "Description must not be blank")
        @Size(max = 255, message = "Description must be at most 255 characters")
        private String description;


        @FileSize(max = 55242880, message = "Logo file must not exceed 50MB")
        private MultipartFile logo;

        @NotBlank(message = "Commitments must not be blank")
        @Size(max = 2000, message = "Commitments must be at most 2000 characters")
        private String commitments;

        @NotBlank(message = "Technical indicators must not be blank")
        @Size(max = 2000, message = "Technical indicators must be at most 2000 characters")
        private String technicalIndicators;

        @NotBlank(message = "Measurement method must not be blank")
        @Size(max = 2000, message = "Measurement method must be at most 2000 characters")
        private String measurementMethod;

        @FileSize(max = 55242880, message = "legalDocsFile file must not exceed 50MB")
        private MultipartFile  legalDocsFile;


        private BigDecimal emissionFactorKgPerKwh;

         private ProjectStatus status;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate startedDate;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate endDate;
    }