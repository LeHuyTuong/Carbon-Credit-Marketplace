package com.carbonx.marketcarbon.service;


import com.carbonx.marketcarbon.model.KycProfile;
import com.carbonx.marketcarbon.request.KycRequest;
import com.carbonx.marketcarbon.response.KycResponse;

public interface KycService {
    public Long create(KycRequest req);
    public Long update(Long id, KycRequest req);
    public KycProfile getByUserId(Long userId);
}
