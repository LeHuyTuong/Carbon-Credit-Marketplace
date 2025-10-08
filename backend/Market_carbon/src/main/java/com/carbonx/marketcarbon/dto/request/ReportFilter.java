package com.carbonx.marketcarbon.dto.request;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReportFilter {
    String period;
    Long sellerId;
    Long vehicleId;
    String status; // EmissionStatus
    Integer page;
    Integer size;
}