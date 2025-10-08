package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.model.User;

public interface ReportService {
     byte[] generatePdf(User user) ;
}
