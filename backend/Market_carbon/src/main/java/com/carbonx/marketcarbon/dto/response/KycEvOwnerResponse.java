package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.common.Gender;
import com.carbonx.marketcarbon.common.IDType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycEvOwnerResponse {
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String country;
    private String address;
    private LocalDate birthDate;
    private IDType documentType;
    private String documentNumber;
    private Gender gender;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

