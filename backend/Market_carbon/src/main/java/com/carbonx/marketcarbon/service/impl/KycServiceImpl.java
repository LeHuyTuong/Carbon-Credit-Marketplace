package com.carbonx.marketcarbon.service.impl;


import com.carbonx.marketcarbon.dto.request.KycCompanyRequest;
import com.carbonx.marketcarbon.dto.response.KycCompanyResponse;
import com.carbonx.marketcarbon.dto.response.KycResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KycServiceImpl implements KycService {

    private final EVOwnerRepository EVOwnerRepository;
    private final  UserRepository userRepository;
    private final CompanyRepository companyRepository;

    private User currentUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if(user == null){
            throw new ResourceNotFoundException("User not found with email: " + email);
        }
        return user;
    }

    @Transactional
    @Override
    public Long createUser(@Validated(KycRequest.Create.class) KycRequest req) {
        // check email thông tin kyc đã tồn tại chưa
        User user = currentUser();
        String email = user.getEmail();

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
                .birthDate(req.getBirthDate())
                .build();

        // B2 lưu data vào repo
        EVOwnerRepository.save(evOwner);
        //B3 ghi log
        log.info("KYC Created : {}" , user.getId());

        //B4 trả về kết quả
        return user.getId();
    }

    @Transactional
    @Override
    public Long updateUser( @Validated(KycRequest.Update.class) KycRequest req) {
        // check user id da co kyc chua
        User user = currentUser();

        EVOwner EVOwner = EVOwnerRepository.findById(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // B1 Set data vào kyc profile
        EVOwner.setName(req.getName());
        EVOwner.setPhone(req.getPhone());
        EVOwner.setCountry(req.getCountry());
        EVOwner.setAddress(req.getAddress());
        EVOwner.setDocumentType(req.getDocumentType());
        EVOwner.setDocumentNumber(req.getDocumentNumber());
        EVOwner.setBirthDate(req.getBirthDate());

        //B2 save lại
        EVOwnerRepository.save(EVOwner);
        log.info("KYC Updated : {}" , EVOwner);
        return user.getId();
    }

    @Override
    public EVOwner getByUserId() {
        // B1 xem thử có data không
        User user = currentUser();

        //B2 Trả Về response
        return  EVOwnerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    @Override
    public List<KycResponse> getAllKYCUser() {
        return EVOwnerRepository.findAll().stream()
                .map(EVOwner -> new KycResponse(
                        EVOwner.getUser().getId(),
                        EVOwner.getName(),
                        EVOwner.getGender(),
                        EVOwner.getEmail(),
                        EVOwner.getPhone(),
                        EVOwner.getCountry(),
                        EVOwner.getAddress(),
                        EVOwner.getDocumentType(),
                        EVOwner.getDocumentNumber(),
                        EVOwner.getBirthDate(),
                        EVOwner.getCreateAt(),
                        EVOwner.getUpdatedAt()
                )).toList();
    }

    @Transactional
    @Override
    public Long createCompany(KycCompanyRequest req) {
        //B1 check tk user
        User user = currentUser();
        String email = user.getEmail();
        //B2 check user dki company
        Company companyExist = companyRepository.findByUserEmail(email);

        if(companyExist != null){
            throw new AppException(ErrorCode.COMPANY_IS_EXIST);
        }
        // khi tạo mới Company cần ko cần EV owner hay vehicle
        Company company = Company.builder()
                .user(user)
                .businessLicense(req.getBusinessLicense())
                .taxCode(req.getTaxCode())
                .companyName(req.getCompanyName())
                .address(req.getAddress())
                .build();

        companyRepository.save(company);

        return company.getId();
    }

    @Transactional
    @Override
    public Long updateCompany(KycCompanyRequest req) {
        User user = currentUser();

        Company company = companyRepository.findByUserEmail(user.getEmail());

        // B1 Set data vào kyc profile
        company.setId(company.getId());
        company.setUser(user);
        company.setBusinessLicense(req.getBusinessLicense());
        company.setTaxCode(req.getTaxCode());
        company.setCompanyName(req.getCompanyName());
        company.setAddress(req.getAddress());
        //B2 save lại
        companyRepository.save(company);
        log.info("KYC Company Updated : {}" , company);
        return user.getId();
    }

    @Override
    public KycCompanyResponse getByCompanyId() {
        User user = currentUser();
        String userEmail =  user.getEmail();
        Company companyExist = companyRepository.findByUserEmail(userEmail);
        if(companyExist == null)
            throw new ResourceNotFoundException("Company not found");
        return  KycCompanyResponse.builder()
                 .id(companyExist.getId())
                 .companyName(companyExist.getCompanyName())
                 .taxCode(companyExist.getTaxCode())
                 .businessLicense(companyExist.getBusinessLicense())
                 .address(companyExist.getAddress())
                 .createAt(companyExist.getCreateAt())
                 .updatedAt(companyExist.getUpdatedAt())
                 .build();
    }

    @Override
    public List<KycCompanyResponse> getAllKYCCompany() {

//        // tìm list EvOwner thuộc Company
//        List<EVOwner> EvOwner = EVOwnerRepository.findByCompanyId(companyExist.getId());
        return companyRepository.findAll().stream()
                .map(company -> new KycCompanyResponse(
                        company.getId(),
                        company.getBusinessLicense(),
                        company.getTaxCode(),
                        company.getCompanyName(),
                        company.getAddress(),
                        company.getCreateAt(),
                        company.getUpdatedAt()
                ) ).toList();
    }


}
