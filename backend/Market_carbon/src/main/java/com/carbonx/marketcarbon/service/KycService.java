package com.carbonx.marketcarbon.service;


import com.carbonx.marketcarbon.dto.request.KycAdminRequest;
import com.carbonx.marketcarbon.dto.request.KycCompanyRequest;
import com.carbonx.marketcarbon.dto.request.KycCvaRequest;
import com.carbonx.marketcarbon.dto.response.KycAdminResponse;
import com.carbonx.marketcarbon.dto.response.KycCompanyResponse;
import com.carbonx.marketcarbon.dto.response.KycCvaResponse;
import com.carbonx.marketcarbon.dto.response.KycResponse;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.EVOwner;
import com.carbonx.marketcarbon.dto.request.KycRequest;

import java.util.List;

public interface KycService {

    Long createUser(KycRequest req);

    Long updateUser(KycRequest req);

    EVOwner getByUserId();

    List<KycResponse> getAllKYCUser();

    Long createCompany(KycCompanyRequest req);

    Long updateCompany(KycCompanyRequest req);

    Company getByCompanyId();

    List<KycCompanyResponse> getAllKYCCompany();

    Long createCva(KycCvaRequest req);

    Long updateCva(KycCvaRequest req);

    KycCvaResponse getCvaProfile();

    List<KycCvaResponse> getAllCvaProfiles();


    Long createAdmin(KycAdminRequest req);
    Long updateAdmin(KycAdminRequest req);
    KycAdminResponse getAdminProfile();
}