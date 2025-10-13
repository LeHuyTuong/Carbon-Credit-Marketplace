package com.carbonx.marketcarbon.dto.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KycCvaRequest {
    @NotBlank String name;
    @Email String email;
    String organization;
    String positionTitle;
    String accreditationNo;
    Integer capacityQuota;
    String notes;
}
