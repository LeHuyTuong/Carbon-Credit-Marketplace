package com.carbonx.marketcarbon.service.impl;


import com.carbonx.marketcarbon.request.KycRequest;
import com.carbonx.marketcarbon.response.KycResponse;
import com.carbonx.marketcarbon.service.KycService;
import org.springframework.stereotype.Service;

@Service
public class KycServiceImpl implements KycService {
    @Override
    public KycResponse create(KycRequest req) {
        return null;
    }

    @Override
    public KycResponse update(Long id, KycRequest req) {
        return null;
    }

    @Override
    public KycResponse getByUser(Long userId) {
        return null;
    }
}
