package com.carbonx.marketcarbon.certificate;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/certificates")
public class CertificateController {

    private final CertificatePdfService pdfService;

    // POST JSON -> trả PDF
    @PostMapping(value = "/pdf", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> generatePdf(@RequestBody CertificateData req) {
        byte[] pdf = pdfService.generatePdf(req);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline().filename("carbon-certificate.pdf").build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

}
