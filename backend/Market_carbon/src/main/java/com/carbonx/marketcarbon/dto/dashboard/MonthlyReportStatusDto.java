package com.carbonx.marketcarbon.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyReportStatusDto {
    private String month;     // "Jan", "Feb", ...
    private long approved;    // nhóm các trạng thái approved
    private long pending;     // nhóm các trạng thái pending
    private long rejected;    // nhóm các trạng thái rejected
}