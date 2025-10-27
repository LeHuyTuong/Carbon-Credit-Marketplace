package com.carbonx.marketcarbon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiEvaluationResponse {
    private Double aiPreScore;
    private String aiVersion;
    private String aiPreNotes;
}