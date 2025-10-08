package com.carbonx.marketcarbon.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvidenceCheckRequest {
    Boolean checked;
    String cvaNote;
}