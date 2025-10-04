package com.carbonx.marketcarbon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class ProjectRequest {

    @NotBlank(message ="Title must not be blank")
    @Size(max = 100, message = "Title max 100 chars")
    private String title;

    @NotBlank(message = "Description must not be blank")
    @Size(max = 1000, message = "Description max 1000 chars")
    private String description;

    @NotBlank(message = "Logo URL must not be blank")
    @URL(message = "Logo must be a valid URL")
    @Size(max = 255, message = "Logo URL max 255 chars")
    private String logo;
}
