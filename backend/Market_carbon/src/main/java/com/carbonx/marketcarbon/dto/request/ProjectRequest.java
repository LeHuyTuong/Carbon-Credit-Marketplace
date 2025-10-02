package com.carbonx.marketcarbon.dto.request;

import jakarta.persistence.Column;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class ProjectRequest {
    @Column(unique = true, nullable = false,  length = 20)
    private String title;

    @Column(nullable = false,  length = 255)
    private String description;

    @URL
    @Column(nullable = false,  length = 255)
    private String logo;
}
