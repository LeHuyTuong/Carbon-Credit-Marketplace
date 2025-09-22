package com.carbonx.marketcarbon.service;


import com.carbonx.marketcarbon.request.KycRequest;
import com.carbonx.marketcarbon.response.KycResponse;

public interface KycService {
    KycResponse create(KycRequest req);
    KycResponse update(Long id, KycRequest req);
    KycResponse getByUser(Long userId);
}
