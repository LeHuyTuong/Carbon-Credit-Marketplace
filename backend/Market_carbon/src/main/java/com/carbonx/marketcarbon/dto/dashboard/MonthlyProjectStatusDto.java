package com.carbonx.marketcarbon.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MonthlyProjectStatusDto {
    private String month;
    private Long submitted;
    private Long approved;
    private Long rejected;
}
