package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.ChargingData;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.Vehicle;
import com.carbonx.marketcarbon.repository.ChargingDataRepository;
import com.carbonx.marketcarbon.repository.CompanyRepository;
import com.carbonx.marketcarbon.repository.VehicleRepository;
import com.carbonx.marketcarbon.repository.EmissionReportRepository;
import com.carbonx.marketcarbon.service.ChargingDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChargingDataServiceImpl implements ChargingDataService {

    private final ChargingDataRepository chargingRepo;
    private final VehicleRepository vehicleRepo;
    private final CompanyRepository companyRepo;
    private final EmissionReportRepository reportRepo;

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final List<DateTimeFormatter> TS_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    );

    @Override
    public void importCsvMonthly(MultipartFile file, Long companyId, String periodMonth) throws IOException {
        log.info("Start import CSV for company={}, period={}", companyId, periodMonth);

        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));

        YearMonth ym = parseYearMonth(periodMonth);
        LocalDateTime from = ym.atDay(1).atStartOfDay();
        LocalDateTime to = ym.plusMonths(1).atDay(1).atStartOfDay();

        var format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build();

        int total = 0;
        List<ChargingData> batch = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = new CSVParser(reader, format)) {

            for (CSVRecord record : parser) {
                try {
                    LocalDateTime ts = parseTs(req(record, "timestamp"));
                    if (ts.isBefore(from) || !ts.isBefore(to)) {
                        log.warn("Timestamp {} not in valid period {}", ts, periodMonth);
                        continue;
                    }

                    BigDecimal kWh = new BigDecimal(normalize(req(record, "charging_energy")));
                    Long vehicleId = Long.parseLong(req(record, "vehicle_id"));

                    Vehicle vehicle = vehicleRepo.findById(vehicleId)
                            .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

                    batch.add(ChargingData.builder()
                            .vehicle(vehicle)
                            .timestamp(ts)
                            .chargingEnergy(kWh)
                            .build());

                    if (batch.size() >= 1000) {
                        chargingRepo.saveAll(batch);
                        total += batch.size();
                        batch.clear();
                    }

                } catch (NumberFormatException e) {
                    log.error("Invalid numeric format at line {}: {}", record.getRecordNumber(), e.getMessage());
                    throw new AppException(ErrorCode.CSV_INVALID_NUMBER_FORMAT);
                } catch (AppException e) {
                    throw e;
                } catch (Exception e) {
                    log.error("Unexpected CSV parsing error at line {}: {}", record.getRecordNumber(), e.getMessage());
                    throw new AppException(ErrorCode.CSV_UNEXPECTED_ERROR);
                }
            }

            if (!batch.isEmpty()) {
                chargingRepo.saveAll(batch);
                total += batch.size();
            }

            log.info("Imported {} charging records for company={} period={}", total, companyId, periodMonth);

        } catch (IOException e) {
            log.error("Failed to read CSV file: {}", e.getMessage());
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        log.info("CSV import completed successfully for company={}, period={}", companyId, periodMonth);
    }

    @Override
    public void importCsvMonthlyWithMeta(MultipartFile file) throws Exception {
        log.info("Start import CSV (with meta inside file)");

        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());

            int total = 0;
            List<ChargingData> batch = new ArrayList<>();

            for (CSVRecord record : parser) {
                try {
                    String period = req(record, "period");
                    Long companyId = Long.parseLong(req(record, "company_id"));
                    String timestamp = req(record, "timestamp");
                    Long vehicleId = Long.parseLong(req(record, "vehicle_id"));
                    BigDecimal energy = new BigDecimal(normalize(req(record, "charging_energy")));

                    ChargingData data = processRecord(companyId, period, timestamp, vehicleId, energy);
                    batch.add(data);

                    if (batch.size() >= 1000) {
                        chargingRepo.saveAll(batch);
                        total += batch.size();
                        batch.clear();
                    }

                } catch (AppException e) {
                    throw e;
                } catch (Exception e) {
                    log.error("Error parsing record line {}: {}", record.getRecordNumber(), e.getMessage());
                    throw new AppException(ErrorCode.CSV_UNEXPECTED_ERROR);
                }
            }

            if (!batch.isEmpty()) {
                chargingRepo.saveAll(batch);
                total += batch.size();
            }

            log.info("Imported {} rows successfully (meta mode)", total);

        } catch (IOException e) {
            log.error("Failed to process CSV: {}", e.getMessage());
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private ChargingData processRecord(Long companyId, String period, String timestamp, Long vehicleId, BigDecimal energy) {
        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));

        YearMonth ym = parseYearMonth(period);
        LocalDateTime from = ym.atDay(1).atStartOfDay();
        LocalDateTime to = ym.plusMonths(1).atDay(1).atStartOfDay();

        LocalDateTime ts = parseTs(timestamp);
        if (ts.isBefore(from) || !ts.isBefore(to)) {
            log.warn("Skipping timestamp {} outside of period {} for company {}", ts, period, company.getCompanyName());
            throw new AppException(ErrorCode.CSV_INVALID_FILE_FORMAT);
        }

        Vehicle vehicle = vehicleRepo.findById(vehicleId)
                .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        return ChargingData.builder()
                .vehicle(vehicle)
                .timestamp(ts)
                .chargingEnergy(energy)
                .build();
    }

    private YearMonth parseYearMonth(String s) {
        try {
            return YearMonth.parse(s, YM);
        } catch (Exception e) {
            log.error("Invalid period format: {}", s);
            throw new AppException(ErrorCode.CSV_INVALID_FILE_FORMAT);
        }
    }

    private LocalDateTime parseTs(String raw) {
        for (var f : TS_FORMATS) {
            try {
                return LocalDateTime.parse(raw, f);
            } catch (Exception ignored) {
            }
        }
        log.error("Invalid timestamp: {}", raw);
        throw new AppException(ErrorCode.CSV_INVALID_NUMBER_FORMAT);
    }

    private String req(CSVRecord r, String h) {
        if (!r.isMapped(h) || r.get(h) == null || r.get(h).isBlank()) {
            log.error("Missing CSV column: {}", h);
            throw new AppException(ErrorCode.CSV_MISSING_FIELD);
        }
        return r.get(h).trim();
    }

    private String normalize(String s) {
        return s.replace(" ", "").replace(",", ".");
    }
}
