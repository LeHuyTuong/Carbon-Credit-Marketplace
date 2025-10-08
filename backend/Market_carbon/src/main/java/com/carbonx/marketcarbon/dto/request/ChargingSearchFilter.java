package com.carbonx.marketcarbon.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChargingSearchFilter {
    Long projectId;
    Long vehicleId;
    String period;               // ví dụ "2025-09" (sẽ quy về first/last day)
    OffsetDateTime from;
    OffsetDateTime to;
    String status;               // PENDING/VALID/INVALID
    Integer page;
    Integer size;
}
