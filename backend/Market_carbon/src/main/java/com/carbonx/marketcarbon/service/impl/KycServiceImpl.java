package com.carbonx.marketcarbon.service.impl;


import com.carbonx.marketcarbon.config.Translator;
import com.carbonx.marketcarbon.common.KycStatus;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.KycProfile;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.KycRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.dto.request.KycRequest;
import com.carbonx.marketcarbon.service.KycService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
public class KycServiceImpl implements KycService {

    private  KycRepository kycRepository;
    private  UserRepository userRepository;

    @Override
    public Long create(@Validated(KycRequest.Create.class) KycRequest req) {
        // check email thông tin kyc đã tồn tại chưa

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if(user == null){
            throw new ResourceNotFoundException("User not found");
        }

        if(kycRepository.existsByUserId(user.getId()))
            throw new ResourceNotFoundException("KYC exists");

        // B1 Lấy data từ request vào object
        KycProfile kycProfile = KycProfile.builder()
                .userId(user.getId())
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
        //B3 ghi log
        log.info("KYC Created : {}" , user.getFullName());

        //B4 trả về kết quả
        return user.getId();
    }

    @Override
    public Long update(Long id, @Validated(KycRequest.Update.class) KycRequest req) {
        // check user id da co kyc chua
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if(user == null){
            throw new ResourceNotFoundException("User not found");
        }

        KycProfile kycProfile = kycRepository.findById((id))
                .orElseThrow(() -> new ResourceNotFoundException(Translator.toLocale("kyc.not.found")));

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
        return user.getId();
    }

    @Override
    public KycProfile getByUserId(Long userId) {
        // B1 xem thử có data không
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if(user == null){
            throw new ResourceNotFoundException("User not found");
        }

        KycProfile kycProfile = kycRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(Translator.toLocale(Translator.toLocale("kyc.not.found"))));

        //B2 Trả Về response
        return kycProfile;
    }

}
