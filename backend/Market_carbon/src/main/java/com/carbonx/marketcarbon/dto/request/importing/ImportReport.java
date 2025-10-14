package com.carbonx.marketcarbon.dto.request.importing;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ImportReport {
    private int total;
    private int success;
    private int failed;
    private List<RowResult> results;

    @Data
    @Builder
    public static class RowResult {
        private int lineNumber;      // số dòng thực tế trong file (tính cả header)
        private boolean success;
        private Long projectId;      // id dự án tạo được (nếu thành công)
        private Map<String, String> columns;

        private String errorCode;
        private String errorDetails;
    }
}