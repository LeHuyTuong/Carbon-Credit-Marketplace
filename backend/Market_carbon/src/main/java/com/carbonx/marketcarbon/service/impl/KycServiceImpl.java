package com.carbonx.marketcarbon.service.impl;


import com.carbonx.marketcarbon.config.Translator;
import com.carbonx.marketcarbon.dto.request.KycCompanyRequest;
import com.carbonx.marketcarbon.dto.response.KycCompanyResponse;
import com.carbonx.marketcarbon.dto.response.KycResponse;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.EVOwner;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.CompanyRepository;
import com.carbonx.marketcarbon.repository.EVOwnerRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.dto.request.KycRequest;
import com.carbonx.marketcarbon.service.KycService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KycServiceImpl implements KycService {

    private final EVOwnerRepository EVOwnerRepository;
    private final  UserRepository userRepository;
    private final CompanyRepository companyRepository;

    @Override
    public Long createUser(@Validated(KycRequest.Create.class) KycRequest req) {
        // check email thông tin kyc đã tồn tại chưa

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if(user == null){
            throw new ResourceNotFoundException("User not found");
        }

        if(EVOwnerRepository.existsByUserId(user.getId()))
            throw new ResourceNotFoundException("KYC exists" + user.getId());

        // B1 Lấy data từ request vào object
        EVOwner evOwner = EVOwner.builder()
                .user(user)
                .email(email)
                .name(req.getName())
                .gender(req.getGender())
                .phone(req.getPhone())
                .country(req.getCountry())
                .address(req.getAddress())
                .documentType(req.getDocumentType())
                .documentNumber(req.getDocumentNumber())
                .birthDate(req.getBirthday())
                .build();

        // B2 lưu data vào repo
        EVOwnerRepository.save(evOwner);
        //B3 ghi log
        log.info("KYC Created : {}" , user.getId());

        //B4 trả về kết quả
        return user.getId();
    }

    @Override
    public Long updateUser(Long id, @Validated(KycRequest.Update.class) KycRequest req) {
        // check user id da co kyc chua
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if(user == null){
            throw new ResourceNotFoundException("User not found");
        }

        EVOwner EVOwner = EVOwnerRepository.findById((id))
                .orElseThrow(() -> new ResourceNotFoundException(Translator.toLocale("kyc.not.found")));

        // B1 Set data vào kyc profile
        EVOwner.setName(req.getName());
        EVOwner.setPhone(req.getPhone());
        EVOwner.setCountry(req.getCountry());
        EVOwner.setAddress(req.getAddress());
        EVOwner.setDocumentType(req.getDocumentType());
        EVOwner.setDocumentNumber(req.getDocumentNumber());
        EVOwner.setBirthDate(req.getBirthday());

        //B2 save lại
        EVOwnerRepository.save(EVOwner);
        log.info("KYC Updated : {}" , EVOwner);
        return user.getId();
    }

    @Override
    public EVOwner getByUserId() {
        // B1 xem thử có data không
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if(user == null){
            throw new ResourceNotFoundException("User not found");
        }


        //B2 Trả Về response
        return  EVOwnerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(Translator.toLocale(Translator.toLocale("kyc.not.found"))));
    }

    @Override
    public List<KycResponse> getAllKYCUser() {
        return EVOwnerRepository.findAll().stream()
                .map(EVOwner -> new KycResponse(
                        EVOwner.getId(),
                        EVOwner.getUser().getId(),
                        EVOwner.getName(),
                        EVOwner.getGender(),
                        EVOwner.getEmail(),
                        EVOwner.getPhone(),
                        EVOwner.getCountry(),
                        EVOwner.getAddress(),
                        EVOwner.getDocumentType(),
                        EVOwner.getDocumentNumber(),
                        EVOwner.getBirthDate()
                )).toList();
    }

    @Override
    public Long createCompany(KycCompanyRequest req) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return 0L;
    }

    @Override
    public Long updateCompany(Long id, KycCompanyRequest req) {
        return 0L;
    }

    @Override
    public Company getByCompanyId(Long companyId) {
        return null;
    }

    @Override
    public List<KycCompanyResponse> getAllKYCCompany() {
        return List.of();
    }


}
