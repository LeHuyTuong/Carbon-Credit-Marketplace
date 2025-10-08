package com.carbonx.marketcarbon.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDecisionRequest {
    boolean approve;     // true = ADMIN_APPROVED
    String comment;      // lý do khi reject
    Long creditId;       // nếu approve có thể gắn credit luôn
}
