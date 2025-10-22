package com.carbonx.marketcarbon.dto.request;

import com.carbonx.marketcarbon.common.validator.FileSize;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ProjectApplicationRequest {

    @NotNull
    private Long projectId;

    @FileSize(max = 10485760, message = "Legal document must not exceed 10MB")
    private MultipartFile applicationDocs;
}
