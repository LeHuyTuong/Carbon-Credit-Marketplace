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
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChargingDataServiceImpl implements ChargingDataService {

    private final ChargingDataRepository chargingRepo;
    private final VehicleRepository vehicleRepo;
    private final CompanyRepository companyRepo;
    private final EmissionReportRepository reportRepo;

    // --- Định dạng năm-tháng ---
    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy-MM");

    // --- Các định dạng timestamp được hỗ trợ ---
    private static final List<DateTimeFormatter> TS_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
            DateTimeFormatter.ofPattern("M/d/yyyy H:mm"),
            DateTimeFormatter.ofPattern("M/d/yyyy HH:mm")
    );

    // --- Cache lookup để tránh query DB trùng ---
    private final Map<String, Long> companyNameCache = new HashMap<>();
    private final Map<String, Long> vehicleCache = new HashMap<>();

    @Override
    public void importCsvMonthly(MultipartFile file, Long companyId, String periodMonth) throws IOException {

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
                    // Đọc các cột có thể có
                    String period = req(record, "period");
                    String companyIdStr = opt(record, "company_id");
                    String companyName = opt(record, "company_name");
                    String timestamp = req(record, "timestamp");
                    String vehicleIdStr = opt(record, "vehicle_id");
                    String plateNumber = opt(record, "plate_number");
                    BigDecimal energy = new BigDecimal(normalize(req(record, "charging_energy")));

                    // Xác định company
                    Company company = resolveCompany(companyIdStr, companyName);

                    // Xác định vehicle
                    Vehicle vehicle = resolveVehicle(company, vehicleIdStr, plateNumber);

                    // Kiểm tra period và timestamp
                    YearMonth ym = parseYearMonth(period);
                    LocalDateTime from = ym.atDay(1).atStartOfDay();
                    LocalDateTime to = ym.plusMonths(1).atDay(1).atStartOfDay();
                    LocalDateTime ts = parseTs(timestamp);

                    if (ts.isBefore(from) || !ts.isBefore(to)) {
                        log.warn("Skipping timestamp {} outside of period {} for {}", ts, period, company.getCompanyName());
                        continue;
                    }

                    // Tạo record
                    ChargingData data = ChargingData.builder()
                            .vehicle(vehicle)
                            .timestamp(ts)
                            .chargingEnergy(energy)
                            .build();
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

            log.info(" Imported {} rows successfully (meta mode)", total);

        } catch (IOException e) {
            log.error("Failed to process CSV: {}", e.getMessage());
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        } finally {
            companyNameCache.clear();
            vehicleCache.clear();
        }
    }

    private Company resolveCompany(String companyIdStr, String companyName) {
        if (companyIdStr != null && !companyIdStr.isBlank()) {
            Long id = Long.parseLong(companyIdStr);
            return companyRepo.findById(id)
                    .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
        }

        if (companyName == null || companyName.isBlank()) {
            throw new AppException(ErrorCode.CSV_MISSING_FIELD);
        }

        Long cachedId = companyNameCache.get(companyName.toLowerCase());
        if (cachedId != null) {
            return companyRepo.findById(cachedId)
                    .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
        }

        Company company = companyRepo.findByCompanyNameIgnoreCase(companyName)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));

        companyNameCache.put(companyName.toLowerCase(), company.getId());
        return company;
    }

    private Vehicle resolveVehicle(Company company, String vehicleIdStr, String plateNumber) {
        if (vehicleIdStr != null && !vehicleIdStr.isBlank()) {
            Long id = Long.parseLong(vehicleIdStr);
            return vehicleRepo.findById(id)
                    .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));
        }

        if (plateNumber == null || plateNumber.isBlank()) {
            throw new AppException(ErrorCode.CSV_MISSING_FIELD);
        }

        String key = company.getId() + "|" + plateNumber.toUpperCase();
        Long cachedId = vehicleCache.get(key);
        if (cachedId != null) {
            return vehicleRepo.findById(cachedId)
                    .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));
        }

        Vehicle v = vehicleRepo.findByCompanyIdAndPlateNumberIgnoreCase(company.getId(), plateNumber)
                .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        vehicleCache.put(key, v.getId());
        return v;
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
            } catch (Exception ignored) { }
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

    private String opt(CSVRecord r, String h) {
        return (r.isMapped(h) && r.get(h) != null && !r.get(h).isBlank()) ? r.get(h).trim() : null;
    }

    private String normalize(String s) {
        return s.replace(" ", "").replace(",", ".");
    }
}
