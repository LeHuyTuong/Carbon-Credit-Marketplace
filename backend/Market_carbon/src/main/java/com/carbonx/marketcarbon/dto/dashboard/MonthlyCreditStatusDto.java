package com.carbonx.marketcarbon.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyCreditStatusDto {
    private String month;  // ví dụ "Jan", "Feb", ...
    private long listed;
    private long sold;
    private long traded;
    private long retired;
    private long pending;
    private long active;   // tổng hợp: ISSUE + ISSUED
    private long revoke;   // EXPIRED
}
