package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.dto.response.CompanyPayoutSummaryItemResponse;
import com.carbonx.marketcarbon.dto.response.CompanyPayoutSummaryResponse;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.service.CompanyPayoutQueryService;
import com.carbonx.marketcarbon.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final String[] HEADER = new String[] {
            "Owner Name", "Email", "#Vehicles", "Energy (kWh)", "Credits", "Amount (VND)", "Status"
    };

    private final CompanyPayoutQueryService companyPayoutQueryService;

    @Override
    public byte[] generatePdf(User user) {
        try (PDDocument doc = new PDDocument()) {
            //B1 tao 1 page
            PDPage page = new PDPage(PDRectangle.A4); // chuẩn format A4
            //B2 them vao doc
            doc.addPage(page);
            try (PDPageContentStream cont = new PDPageContentStream(doc, page)) {
                float margin = 25f;
                float StartY = page.getMediaBox().getHeight() - 50f;
                //B1 bat dau
                cont.beginText();
                cont.setLeading(14f);                     // <-- line spacing cho newLine()

                //B3.1 Set title
                cont.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cont.newLineAtOffset(margin, StartY);
                String title = "CARBON CREDIT CERTIFICATE";
                cont.showText(title);
                cont.newLine();

                //3.2 Set body
                cont.setFont(PDType1Font.HELVETICA, 12);
                String confirm = "This certificate confirms that";
                cont.showText(confirm);
                cont.newLine();
                String fullName = "hehe";

                // TODO: thay <CREDITS> bằng dữ liệu thực
                cont.showText(fullName + " has purchased <CREDITS> CARBON CREDITS");
                cont.newLine();

                String issueCertificate = "HAS BEEN PURCHASED AND IS BEING RETIRED TO SUPPORT";
                cont.showText(issueCertificate);
                cont.newLine();

                String projectName = "Ghi ten project ma aggre dki voi admin";
                cont.showText(projectName);
                cont.newLine();

                String text = "ON BEHALF OF";
                cont.showText(text);
                cont.newLine();

                String aggreratorName = "FLop qua thi ghi ten anh vao , SON TUNG MTP";
                cont.showText(aggreratorName);
                cont.newLine();

                String serial_iso = "SERIAL NUMBER PURCHASED:" + System.currentTimeMillis();
                cont.showText(serial_iso);
                cont.newLine();

                cont.showText("Date: " + LocalDate.now());
                // B4: có mở file thì có đóng file
                cont.endText();
            }
            // Trả về byte[] thay vì ghi file cứng, controller hỗ trợ download về
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate PDF", e);
        }
    }

    @Override
    public byte[] exportCompanyPayoutXlsx( Long distributionId) {
        CompanyPayoutSummaryResponse summary = companyPayoutQueryService.getDistributionSummary(distributionId);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Payout Summary");
            int rowIdx = 0;

            Row headerRow = sheet.createRow(rowIdx++);
            for (int i = 0; i < HEADER.length; i++) {
                headerRow.createCell(i).setCellValue(HEADER[i]);
            }

            List<CompanyPayoutSummaryItemResponse> items = summary.getItems();
            for (CompanyPayoutSummaryItemResponse item : items) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(safe(item.getOwnerName()));
                row.createCell(1).setCellValue(safe(item.getEmail()));
                row.createCell(2).setCellValue(item.getVehiclesCount());
                setNumericCell(row, 3, item.getEnergyKwh());
                setNumericCell(row, 4, item.getCredits());
                setNumericCell(row, 5, item.getAmountVnd());
                row.createCell(6).setCellValue(safe(item.getStatus()));
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
            throw new IllegalStateException("Failed to export payout summary", e);
        }
    }
    private void setNumericCell(Row row, int columnIndex, BigDecimal value) {
        Cell cell = row.createCell(columnIndex);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        } else {
            cell.setCellValue(0d);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
