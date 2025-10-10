package com.carbonx.marketcarbon.dto.request.importing;

import lombok.Builder;
import lombok.Data;

import java.util.List;

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
        private String error;        // thông báo lỗi gọn (nếu thất bại)
        private String titleEcho;    // echo lại title để tiện đối chiếu
    }
}