package com.carbonx.marketcarbon.service;


import com.carbonx.marketcarbon.dto.request.KycCompanyRequest;
import com.carbonx.marketcarbon.dto.response.KycCompanyResponse;
import com.carbonx.marketcarbon.dto.response.KycResponse;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.EVOwner;
import com.carbonx.marketcarbon.dto.request.KycRequest;

import java.util.List;

public interface KycService {
    Long createUser(KycRequest req);

    Long updateUser(Long id, KycRequest req);

    EVOwner getByUserId();

    List<KycResponse> getAllKYCUser();

    Long createCompany(KycCompanyRequest req);

    Long updateCompany(Long id, KycCompanyRequest req);

    Company getByCompanyId(Long  companyId);

    List<KycCompanyResponse> getAllKYCCompany();

}
