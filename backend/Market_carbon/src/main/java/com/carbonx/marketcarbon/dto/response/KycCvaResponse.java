package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.common.USER_STATUS;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KycCvaResponse {
    Long id;
    String name;
    String email;
    String organization;
    String positionTitle;
    USER_STATUS status;
    String avatarUrl;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
