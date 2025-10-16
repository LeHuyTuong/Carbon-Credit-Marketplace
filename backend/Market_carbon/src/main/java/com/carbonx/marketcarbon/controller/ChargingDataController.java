package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.service.ChargingDataService;
import com.carbonx.marketcarbon.utils.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/charging-data")
@RequiredArgsConstructor
@Tag(name = "Charging Data", description = "Upload monthly EV charging data (CSV) by EV_OWNER or ENTERPRISE")
public class ChargingDataController {

    private final ChargingDataService service;

    @Operation(summary = "Upload EV charging data CSV (with period and company_id inside file)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Upload successful",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid CSV format or missing columns", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "User not authorized", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Server error while processing file", content = @Content(mediaType = "application/json"))
    })
    @PreAuthorize("hasAnyRole('EV_OWNER','ENTERPRISE')")
    @PostMapping(
            value = "/upload-csv-charging",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CommonResponse<String>> uploadCsvWithPeriodAndCompany(
            @RequestPart("file") MultipartFile file
    ) throws Exception {

        service.importCsvMonthlyWithMeta(file); //  Hàm mới xử lý file có period + company_id

        CommonResponse<String> response = CommonResponse.<String>builder()
                .requestTrace("TRACE-" + System.currentTimeMillis())
                .responseDateTime(OffsetDateTime.now())
                .responseStatus(CommonResponse.ResponseStatus.builder()
                        .responseCode("200")
                        .responseMessage("Charging data uploaded successfully (auto-read period & company_id from CSV)")
                        .build())
                .responseData("CSV file processed successfully")
                .build();

        return ResponseEntity.ok(response);
    }
}
