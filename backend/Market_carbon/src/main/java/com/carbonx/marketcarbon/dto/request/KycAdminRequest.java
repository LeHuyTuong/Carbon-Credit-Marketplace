package com.carbonx.marketcarbon.dto.request;


import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KycAdminRequest {
    String name;
    String phone;
    String firstName;
    String lastName;
    String country;
    String city;
    String birthday;
    MultipartFile avatar;
}
