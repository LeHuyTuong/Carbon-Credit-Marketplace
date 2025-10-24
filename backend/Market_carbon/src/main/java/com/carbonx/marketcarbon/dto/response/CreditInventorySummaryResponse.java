package com.carbonx.marketcarbon.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record CreditInventorySummaryResponse(
        long total,
        long available,
        long reserved,
        long sold,
        long retired,
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
