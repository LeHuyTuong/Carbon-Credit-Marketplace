package com.carbonx.marketcarbon.service.payout.export;

import com.carbonx.marketcarbon.dto.response.CompanyPayoutSummaryItemResponse;
import com.carbonx.marketcarbon.dto.response.CompanyPayoutSummaryResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Single responsibility: dựng file .xlsx từ một {@link CompanyPayoutSummaryResponse}.
 * Không truy vấn DB, không kiểm tra quyền — những việc đó thuộc service.
 */
@Component
public class PayoutExcelExporter {

    public static final String SHEET_NAME = "Payout Summary";

    private static final String[] HEADER = new String[] {
            "Owner Name", "Email", "#Vehicles", "Energy (kWh)", "Credits", "Amount (USD)", "Status"
    };

    public byte[] export(CompanyPayoutSummaryResponse summary) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            int rowIdx = 0;

            Row headerRow = sheet.createRow(rowIdx++);
            for (int i = 0; i < HEADER.length; i++) {
                headerRow.createCell(i).setCellValue(HEADER[i]);
            }

            List<CompanyPayoutSummaryItemResponse> items = summary.getItems();
            if (items != null) {
                for (CompanyPayoutSummaryItemResponse item : items) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(safe(item.getOwnerName()));
                    row.createCell(1).setCellValue(safe(item.getEmail()));
                    row.createCell(2).setCellValue(item.getVehiclesCount());
                    setNumericCell(row, 3, item.getEnergyKwh());
                    setNumericCell(row, 4, item.getCredits());
                    setNumericCell(row, 5, item.getAmountUsd());
                    row.createCell(6).setCellValue(safe(item.getStatus()));
                }
            }

            Row totalRow = sheet.createRow(rowIdx);
            totalRow.createCell(0).setCellValue("Totals");
            totalRow.createCell(1).setCellValue("");
            totalRow.createCell(2).setCellValue(summary.getOwnersCount());
            setNumericCell(totalRow, 3, summary.getTotalEnergyKwh());
            setNumericCell(totalRow, 4, summary.getTotalCredits());
            setNumericCell(totalRow, 5, summary.getGrandTotalPayout());
            totalRow.createCell(6).setCellValue("");

            for (int i = 0; i < HEADER.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to export payout summary", e);
        }
    }

    private void setNumericCell(Row row, int columnIndex, BigDecimal value) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value == null ? 0d : value.doubleValue());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
