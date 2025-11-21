package com.carbonx.marketcarbon.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record CreditInventorySummaryResponse(
        long buyed,        //  tổng tín chỉ đã mua
        long issued,       // tín chỉ do mình phát hành
        long available,    // đang còn trong kho
        long reserved,     // đang niêm yết
        long sold,         // đã bán
        long retired,      // đã sử dụng
        List<StatusCount> byStatus,
        List<ProjectCount> byProject,
        List<VintageCount> byVintage
) {
    @Builder
    public record StatusCount(String status, long count) {}
    @Builder
    public record ProjectCount(Long projectId, String projectTitle, long count) {}
    @Builder
    public record VintageCount(Integer vintageYear, long count) {}
}
