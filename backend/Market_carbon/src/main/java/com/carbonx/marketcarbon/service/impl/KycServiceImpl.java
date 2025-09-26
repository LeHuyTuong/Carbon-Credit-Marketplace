package com.carbonx.marketcarbon.service.impl;


import com.carbonx.marketcarbon.domain.KycStatus;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.KycProfile;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.KycRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.request.KycRequest;
import com.carbonx.marketcarbon.response.KycResponse;
import com.carbonx.marketcarbon.service.KycService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
public class KycServiceImpl implements KycService {

    @Autowired
    private KycRepository kycRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public KycResponse create(@Validated(KycRequest.Create.class) KycRequest req) {
        // check email thông tin kyc đã tồn tại chưa
        if(kycRepository.existsById(req.getUserId()))
            throw new ResourceNotFoundException("User with id " + req.getUserId() + " not found");

        String email = req.getEmail();
        // B1 Lấy data từ request vào object
        KycProfile kycProfile = KycProfile.builder()
                .userId(req.getUserId())
                .email(email)
                .name(req.getName())
                .phone(req.getPhone())
                .country(req.getCountry())
                .address(req.getAddress())
                .documentType(req.getDocumentType())
                .documentNumber(req.getDocumentNumber())
                .birthDate(req.getBirthday())
                .kycStatus(KycStatus.NEW)
                .build();

        // B2 lưu data vào repo
        kycRepository.save(kycProfile);

        //B3 lưu data vào response để trả về
        KycResponse kycResponse = KycResponse.builder()
                .userId(req.getUserId())
                .kycStatus(KycStatus.NEW)
                .phone(req.getPhone())
                .country(req.getCountry())
                .address(req.getAddress())
                .documentType(req.getDocumentType())
                .documentNumber(req.getDocumentNumber())
                .birthday(req.getBirthday())
                .build();
        log.info("KYC Created : {}" , kycResponse);

        //B4 trả về kết quả
        return kycResponse;
    }

    @Override
    public KycResponse update(Long id, @Validated(KycRequest.Update.class) KycRequest req) {
        // check user id da co kyc chua
        KycProfile kycProfile = kycRepository.findByUserId((id))
                .orElseThrow(() -> new ResourceNotFoundException("Id of KYC not found"));

        // B1 Set data vào kyc profile
        kycProfile.setName(req.getName());
        kycProfile.setPhone(req.getPhone());
        kycProfile.setCountry(req.getCountry());
        kycProfile.setAddress(req.getAddress());
        kycProfile.setDocumentType(req.getDocumentType());
        kycProfile.setDocumentNumber(req.getDocumentNumber());
        kycProfile.setBirthDate(req.getBirthday());

        //B2 save lại
        kycRepository.save(kycProfile);
        log.info("KYC Updated : {}" , kycProfile);
        //B3 Trả về response
        return KycResponse.builder()
                .userId(req.getUserId())
                .birthday(req.getBirthday())
                .phone(req.getPhone())
                .country(req.getCountry())
                .address(req.getAddress())
                .documentType(req.getDocumentType())
                .documentNumber(req.getDocumentNumber())
                .birthday(req.getBirthday())
                .build();
    }

    @Override
    public KycResponse getByUser(Long userId) {
        // B1 xem thử có data không
        KycProfile kycProfile = kycRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Id of KYC not found"));

        //B2 Trả Về response
        return KycResponse.builder()
                .userId(kycProfile.getUserId())
                .birthday(kycProfile.getBirthDate())
                .phone(kycProfile.getPhone())
                .country(kycProfile.getCountry())
                .address(kycProfile.getAddress())
                .documentType(kycProfile.getDocumentType())
                .documentNumber(kycProfile.getDocumentNumber())
                .kycStatus(kycProfile.getKycStatus())
                .build();
    }

}
