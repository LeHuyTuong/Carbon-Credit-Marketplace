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
    String email;
    String name;
    String phone;
    String firstName;
    String lastName;
    String country;
    String city;
    String birthday;
    String avatarUrl;
    OffsetDateTime updatedAt;
}