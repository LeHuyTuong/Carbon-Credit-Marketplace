package com.carbonx.marketcarbon.dto.request;


import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KycAdminRequest {
    String code;                // admin unique code
    String name;
    String email;
    String phone;
    Boolean isSuperAdmin;
    String positionTitle;
}
