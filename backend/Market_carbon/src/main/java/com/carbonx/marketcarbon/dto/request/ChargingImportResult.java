package com.carbonx.marketcarbon.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChargingImportResult {
    Long batchId;
    int totalRows;
    int inserted;
    int duplicated;
    int invalidSchema;
}