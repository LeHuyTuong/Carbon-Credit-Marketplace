package com.carbonx.marketcarbon.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CvaReviewRequest {
    boolean approve;     // true = CVA_APPROVED, false = REJECTED
    String comment;      // lý do khi reject hoặc ghi chú khi approve
}
