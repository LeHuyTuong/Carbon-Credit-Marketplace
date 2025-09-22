package com.example.Market_carbon.service;

import com.example.Market_carbon.request.KycRequest;
import com.example.Market_carbon.response.KycResponse;

public interface KycService {
    KycResponse create(KycRequest req);
    KycResponse update(Long id, KycRequest req);
    KycResponse getByUser(Long userId);
}
