package com.carbonx.marketcarbon.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ChargingDataService {
    void importCsvMonthly(MultipartFile file, Long companyId, String periodMonth) throws IOException;
    public void importCsvMonthlyWithMeta(MultipartFile file) throws Exception;
}
