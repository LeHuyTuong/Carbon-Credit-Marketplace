package com.carbonx.marketcarbon.service.impl;


import com.carbonx.marketcarbon.common.USER_STATUS;
import com.carbonx.marketcarbon.config.Translator;
import com.carbonx.marketcarbon.dto.request.*;
import com.carbonx.marketcarbon.dto.response.KycAdminResponse;
import com.carbonx.marketcarbon.dto.response.KycCompanyResponse;
import com.carbonx.marketcarbon.dto.response.KycCvaResponse;
import com.carbonx.marketcarbon.dto.response.KycResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.*;
import com.carbonx.marketcarbon.repository.*;
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
    private final CvaRepository cvaRepository;
    private final AdminRepository adminRepository;
    private final EVOwnerRepository evOwnerRepository;

    private User currentUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if(user == null){
            throw new ResourceNotFoundException("User not found with email: " + email);
        }
        return user;
    }

    @Override
    public Long createUser(@Validated(KycRequest.Create.class) KycRequest req) {
        // check email thông tin kyc đã tồn tại chưa
        User user = currentUser();
        String email = user.getEmail();

        if(EVOwnerRepository.existsByUserId(user.getId()))
            throw new ResourceNotFoundException("KYC exists with this profile : " + user.getEmail());

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

    @Override
    public Long updateUser( @Validated(KycRequest.Update.class) KycRequest req) {
        // check user id da co kyc chua
        User user = currentUser();

        EVOwner EVOwner = EVOwnerRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(Translator.toLocale("kyc.not.found")));

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
                .orElseThrow(() -> new ResourceNotFoundException(Translator.toLocale(Translator.toLocale("kyc.not.found"))));
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

    @Override
    public Long createCompany(KycCompanyRequest req) {
        //B1 check tk user
        User user = currentUser();
        String email = user.getEmail();
        //B2 check user dki company
        Company companyExist = companyRepository.findByUserEmail(email);

        if(companyExist != null){
            throw new ResourceNotFoundException(Translator.
                    toLocale(Translator.toLocale("company.exist")));
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
        String userEmail = user.getEmail();

        Company company = companyRepository.findByUserEmail(userEmail);
        if (company == null) {
            throw new ResourceNotFoundException(Translator.toLocale("company.not.found"));
        }

        return KycCompanyResponse.builder()
                .id(company.getId())
                .businessLicense(company.getBusinessLicense())
                .taxCode(company.getTaxCode())
                .companyName(company.getCompanyName())
                .address(company.getAddress())
                .createAt(company.getCreateAt())
                .updatedAt(company.getUpdatedAt())
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

    @Override
    public Long createCva(KycCvaRequest req) {
        User user = currentUser();
        if (cvaRepository.existsByUserId(user.getId())) {
            throw new ResourceNotFoundException("KYC for CVA already exists for user " + user.getEmail());
        }

        Cva cva = Cva.builder()
                .user(user)
                .name(req.getName())
                .email(user.getEmail())
                .organization(req.getOrganization())
                .positionTitle(req.getPositionTitle())
                .accreditationNo(req.getAccreditationNo())
                .capacityQuota(req.getCapacityQuota())
                .notes(req.getNotes())
                .status(USER_STATUS.PENDING)
                .build();

        cvaRepository.save(cva);
        log.info("Created KYC for CVA user {}", user.getEmail());
        return cva.getId();
    }

    @Override
    public Long updateCva(KycCvaRequest req) {
        User user = currentUser();
        Cva cva = cvaRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("CVA KYC not found"));

        cva.setName(req.getName());
        cva.setOrganization(req.getOrganization());
        cva.setPositionTitle(req.getPositionTitle());
        cva.setAccreditationNo(req.getAccreditationNo());
        cva.setCapacityQuota(req.getCapacityQuota());
        cva.setNotes(req.getNotes());
        cvaRepository.save(cva);

        log.info("Updated KYC for CVA user {}", user.getEmail());
        return cva.getId();
    }

    @Override
    public KycCvaResponse getCvaProfile() {
        User user = currentUser();
        Cva cva = cvaRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("CVA KYC not found"));

        return KycCvaResponse.builder()
                .id(cva.getId())
                .name(cva.getName())
                .email(cva.getEmail())
                .organization(cva.getOrganization())
                .positionTitle(cva.getPositionTitle())
                .accreditationNo(cva.getAccreditationNo())
                .capacityQuota(cva.getCapacityQuota())
                .status(cva.getStatus())
                .notes(cva.getNotes())
                .createdAt(cva.getCreatedAt())
                .updatedAt(cva.getUpdatedAt())
                .build();
    }

    @Override
    public List<KycCvaResponse> getAllCvaProfiles() {
        return cvaRepository.findAll().stream()
                .map(cva -> KycCvaResponse.builder()
                        .id(cva.getId())
                        .name(cva.getName())
                        .email(cva.getEmail())
                        .organization(cva.getOrganization())
                        .positionTitle(cva.getPositionTitle())
                        .accreditationNo(cva.getAccreditationNo())
                        .capacityQuota(cva.getCapacityQuota())
                        .status(cva.getStatus())
                        .notes(cva.getNotes())
                        .createdAt(cva.getCreatedAt())
                        .updatedAt(cva.getUpdatedAt())
                        .build())
                .toList();
    }

    // create
    @Override
    public Long createAdmin(KycAdminRequest req) {
        User user = currentUser();
        if (adminRepository.findByUserId(user.getId()).isPresent()) {
            throw new ResourceNotFoundException("Admin KYC already exists for user: " + user.getEmail());
        }

        Admin admin = Admin.builder()
                .user(user)
                .name(req.getName())
                .phone(req.getPhone())
                .build();

        adminRepository.save(admin);
        log.info(" Admin KYC created for user {}", user.getEmail());
        return admin.getId();
    }

    // update
    @Override
    public Long updateAdmin(KycAdminRequest req) {
        User user = currentUser();
        Admin admin = adminRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Admin profile not found"));

        admin.setName(req.getName());
        admin.setPhone(req.getPhone());

        adminRepository.save(admin);
        log.info("🛠️ Admin KYC updated for {}", user.getEmail());
        return admin.getId();
    }

    // get profile
    @Override
    public KycAdminResponse getAdminProfile() {
        User user = currentUser();
        Admin admin = adminRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Admin KYC not found"));

        return KycAdminResponse.builder()
                .id(admin.getId())
                .name(admin.getName())
                .email(admin.getUser().getEmail()) // 🔹 lấy từ User
                .phone(admin.getPhone())
                .updatedAt(admin.getUpdatedAt())
                .build();
    }

    @Override
    public Long createKycEVOwner(KycEvOwnerRequest req) {
        User user = currentUser();

        // Kiểm tra xem KYC đã tồn tại chưa
        if (evOwnerRepository.existsByUserId(user.getId())) {
            throw new AppException(ErrorCode.KYC_EXISTED);
        }

        // Tạo bản ghi mới
        EVOwner evOwner = EVOwner.builder()
                .user(user)
                .email(user.getEmail())
                .name(req.getName())
                .phone(req.getPhone())
                .country(req.getCountry())
                .address(req.getAddress())
                .birthDate(req.getBirthDate())
                .documentType(req.getDocumentType())
                .documentNumber(req.getDocumentNumber())
                .gender(req.getGender())
                .build();

        evOwnerRepository.save(evOwner);
        log.info(" KYC created for EV Owner: {}", user.getEmail());
        return evOwner.getId();
    }

}

