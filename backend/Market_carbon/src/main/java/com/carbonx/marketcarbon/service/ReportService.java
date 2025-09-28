package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.model.User;

public interface ReportService {
    public byte[] generatePdf(User user) ;
}
