package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.common.Gender;
import com.carbonx.marketcarbon.common.IDType;
import com.carbonx.marketcarbon.common.KycStatus;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
public class KycResponse {
    private Long id;
    private Long userId;
    private String name;
    private Gender gender;
    private String email; // expose để xem
    private String phone;
    private String country;
    private String address;
    private KycStatus kycStatus;
    private IDType documentType;
    private String documentNumber;
    private LocalDate birthday;
}

