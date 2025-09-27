package com.carbonx.marketcarbon.response;

import com.carbonx.marketcarbon.domain.IDType;
import com.carbonx.marketcarbon.domain.KycStatus;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.sql.Date;
import java.time.LocalDate;

@Data
@Builder
@Getter
public class KycResponse {
    private Long id;
    private Long userId;
    private String email; // expose để xem
    private String phone;
    private String country;
    private String address;
    private KycStatus kycStatus;
    private IDType documentType;
    private String documentNumber;
    private LocalDate birthday;
}

