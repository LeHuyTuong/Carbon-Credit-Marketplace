package com.example.Market_carbon.service.impl;

import com.example.Market_carbon.request.KycRequest;
import com.example.Market_carbon.response.KycResponse;
import com.example.Market_carbon.service.KycService;
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
