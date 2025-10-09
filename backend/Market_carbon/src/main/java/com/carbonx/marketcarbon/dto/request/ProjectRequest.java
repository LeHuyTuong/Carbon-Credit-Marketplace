package com.carbonx.marketcarbon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;


    @Data
    public class ProjectRequest {

        @NotBlank(message = "Title must not be blank")
        @Size(max = 100, message = "Title must be at most 100 characters")
        private String title;

        @NotBlank(message = "Description must not be blank")
        @Size(max = 255, message = "Description must be at most 255 characters")
        private String description;

        @NotBlank(message = "Logo URL must not be blank")
        @URL(message = "Logo must be a valid URL")
        @Size(max = 255, message = "Logo URL must be at most 255 characters")
        private String logo;

        @Size(max = 2000, message = "Commitments must be at most 2000 characters")
        private String commitments;

        @Size(max = 2000, message = "Technical indicators must be at most 2000 characters")
        private String technicalIndicators;

        @Size(max = 2000, message = "Measurement method must be at most 2000 characters")
        private String measurementMethod;

        @URL(message = "LegalDocsUrl must be a valid URL")
        @Size(max = 255, message = "LegalDocsUrl must be at most 255 characters")
        private String legalDocsUrl;
    }