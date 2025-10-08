package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.service.ReportService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;

@Service
public class ReportServiceImpl implements ReportService {
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
}
