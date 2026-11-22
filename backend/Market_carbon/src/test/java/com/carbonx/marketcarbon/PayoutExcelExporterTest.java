package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.dto.response.CompanyPayoutSummaryItemResponse;
import com.carbonx.marketcarbon.dto.response.CompanyPayoutSummaryResponse;
import com.carbonx.marketcarbon.service.payout.export.PayoutExcelExporter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PayoutExcelExporterTest {

    private final PayoutExcelExporter exporter = new PayoutExcelExporter();

    @Test
    void createsValidXlsxWithOneSheetNamedPayoutSummary() throws Exception {
        byte[] bytes = exporter.export(summaryWithTwoOwners());

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
            assertThat(workbook.getSheetName(0)).isEqualTo("Payout Summary");
            assertThat(workbook.getSheetAt(0).getSheetName()).isEqualTo(PayoutExcelExporter.SHEET_NAME);
        }
    }

    @Test
    void writesHeaderDataRowsAndTotalsRow() throws Exception {
        byte[] bytes = exporter.export(summaryWithTwoOwners());

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            // 1 header + 2 owner rows + 1 totals row
            assertThat(sheet.getLastRowNum()).isEqualTo(3);
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(4);

            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Owner Name");
            assertThat(header.getCell(6).getStringCellValue()).isEqualTo("Status");

            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Owner A");
            assertThat(sheet.getRow(1).getCell(5).getNumericCellValue()).isEqualTo(10.5d);
            assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).isEqualTo("Owner B");

            Row totals = sheet.getRow(3);
            assertThat(totals.getCell(0).getStringCellValue()).isEqualTo("Totals");
            assertThat(totals.getCell(2).getNumericCellValue()).isEqualTo(2d);
            assertThat(totals.getCell(5).getNumericCellValue()).isEqualTo(25d);
        }
    }

    @Test
    void emptyPayoutStillHasHeaderAndTotalsRows() throws Exception {
        CompanyPayoutSummaryResponse empty = CompanyPayoutSummaryResponse.builder()
                .items(List.of())
                .ownersCount(0L)
                .totalEnergyKwh(BigDecimal.ZERO)
                .totalCredits(BigDecimal.ZERO)
                .grandTotalPayout(BigDecimal.ZERO)
                .build();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(exporter.export(empty)))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(2);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Owner Name");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Totals");
        }
    }

    private CompanyPayoutSummaryResponse summaryWithTwoOwners() {
        CompanyPayoutSummaryItemResponse first = CompanyPayoutSummaryItemResponse.builder()
                .ownerId(1L).ownerName("Owner A").email("a@example.com").vehiclesCount(1L)
                .energyKwh(new BigDecimal("100.000000")).credits(new BigDecimal("0.100000"))
                .amountUsd(new BigDecimal("10.50")).status("SUCCESS").build();
        CompanyPayoutSummaryItemResponse second = CompanyPayoutSummaryItemResponse.builder()
                .ownerId(2L).ownerName("Owner B").email("b@example.com").vehiclesCount(1L)
                .energyKwh(new BigDecimal("200.000000")).credits(new BigDecimal("0.200000"))
                .amountUsd(new BigDecimal("14.50")).status("SUCCESS").build();

        return CompanyPayoutSummaryResponse.builder()
                .items(List.of(first, second))
                .ownersCount(2L)
                .totalEnergyKwh(new BigDecimal("300.000000"))
                .totalCredits(new BigDecimal("0.300000"))
                .grandTotalPayout(new BigDecimal("25.00"))
                .build();
    }
}
