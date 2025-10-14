package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.common.USER_STATUS;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KycAdminResponse {
    Long id;
    String code;
    String name;
    String email;
    String phone;
    Boolean isSuperAdmin;
    String positionTitle;
    USER_STATUS status;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}