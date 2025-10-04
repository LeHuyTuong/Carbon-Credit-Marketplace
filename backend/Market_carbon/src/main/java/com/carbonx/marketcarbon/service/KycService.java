package com.carbonx.marketcarbon.service;


import com.carbonx.marketcarbon.dto.response.KycResponse;
import com.carbonx.marketcarbon.model.KycProfile;
import com.carbonx.marketcarbon.dto.request.KycRequest;

import java.util.List;

public interface KycService {
    public Long create(KycRequest req);
    public Long update(Long id, KycRequest req);
    public KycProfile getByUserId();
    public List<KycResponse> getAllKYC();
}
