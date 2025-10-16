package com.carbonx.marketcarbon.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectRegisterRequest {

    @NotNull
    private Long baseProjectId;

    @Size(max = 255)
    private String companyName;

    @Size(max = 64)
    private String businessLicense;

    @Size(max = 32)
    private String taxCode;

    @Size(max = 512)
    private String address;

    @Size(max = 1024)
    private String document; // URL hoặc mô tả tài liệu kèm theo
}
