//package com.carbonx.marketcarbon.service.impl;
//
//import com.carbonx.marketcarbon.common.CreditStatus;
//import com.carbonx.marketcarbon.common.ProjectStatus;
//import com.carbonx.marketcarbon.dto.request.CreditIssuanceRequest;
//import com.carbonx.marketcarbon.dto.response.ProjectResponse;
//import com.carbonx.marketcarbon.exception.AppException;
//import com.carbonx.marketcarbon.exception.ErrorCode;
//import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
//import com.carbonx.marketcarbon.exception.WalletException;
//import com.carbonx.marketcarbon.model.*;
//import com.carbonx.marketcarbon.repository.*;
//import com.carbonx.marketcarbon.service.CarbonCreditService;
//import com.carbonx.marketcarbon.service.WalletService;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.Pageable;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class CarbonCreditServiceImpl implements CarbonCreditService {
//
//    private final CarbonCreditRepository carbonCreditRepository;
//    private final ChargingDataRepository chargingDataRepository;
//    private final CompanyRepository companyRepository;
//    private final ProjectRepository projectRepository;
//    private final AdminRepository adminRepository;
//    private final WalletRepository walletRepository;
//    private final WalletService walletService;
//    private final UserRepository userRepository;
//    private final CvaRepository  cvaRepository;
//
//    private User currentUser(){
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String email = authentication.getName();
//        User user = userRepository.findByEmail(email);
//        if(user == null){
//            throw new ResourceNotFoundException("User not found with email: " + email);
//        }
//        return user;
//    }
//
//    @Override
//    public BigDecimal calculateCarbonCredit(BigDecimal chargingEnergy) {
//        // 1 kWh sạc xe điện giảm 0.7 kg CO2 so với xe xăng.
//        // 1 tấn CO2 = 1 tín chỉ carbon
//        BigDecimal emissionReductionKg = chargingEnergy.multiply(new BigDecimal("0.7"));
//        return emissionReductionKg.divide(new BigDecimal("1000"), RoundingMode.HALF_UP);
//    }
//
//    @Transactional
//    @Override
//    public CarbonCredit issueCredits(CreditIssuanceRequest request) {
//        //B1 Lấy Dữ Liệu Sạc
//        List<ChargingData> chargingData = chargingDataRepository.findAllById(request.getChargingDataIds());
//
//        //B2 Kiểm Tra Dữ Liệu
//        if(chargingData.isEmpty()){
//            throw new IllegalArgumentException("No valid charging data found for the provided IDs.");
//        }
//
//        //B3 Xác Định Công Ty và Dự Án
//        // tất cả dữ liệu sạc trong một lần yêu cầu đều thuộc về cùng một công ty. lấy companyId từ bản ghi đầu tiên.
//        Long companyId = chargingData.get(0).getCompanyId();
//        //get(0) lấy ra phần tử ở vị trí đầu tiên trong list chargingDataList.
//
//        //Dựa vào companyId và projectId từ request, tìm thông tin đầy đủ của công ty và dự án liên quan.
//        Company company = companyRepository.findById(companyId)
//                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + companyId));
//
//        Project project = projectRepository.findById(request.getProjectId())
//                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + request.getProjectId()));
//
//        //B4 Tổng Hợp Năng Lượng và Tính Toán Tín Chỉ
//        // tính tổng toàn bộ năng lượng đã sạc (chargingEnergy) từ tất cả các bản ghi ChargingData đã lấy được.
//        BigDecimal totalEnergy = chargingData.stream()
//                .map(ChargingData::getChargingEnergy)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        //chuyển đổi tổng năng lượng (kWh) thành tổng số tín chỉ carbon
//        BigDecimal totalCredits = calculateCarbonCredit(totalEnergy);
//
//        //B5 Tạo Lô Tín Chỉ Carbon Mới
//        CarbonCredit newCredit = CarbonCredit.builder()
//                .carbonCredit(totalCredits) // carbonCredit(totalCredits): Số lượng tín chỉ được tạo ra.
//                .company(company) // Gán tín chỉ này cho công ty sở hữu.
//                .project(project) // Nguồn gốc dự án của tín chỉ.
//                .status(CreditStatus.PENDING)// tình trạng ban đầu là PENDING (Chờ duyệt). Admin sẽ cần phê duyệt để chuyển sang ISSUED (Đã cấp).
//                .amount(totalCredits.intValue())//  số tín chỉ là kiểu số nguyên không tách rời
//                .name("Credits from " + project.getTitle() + " - " + LocalDate.now().getYear()) // xác định năm phát sinh của tín chỉ
//                //tạo mã định danh duy nhất cho lô tín chỉ này, bao gồm ID dự án, ID công ty và thời gian tạo để đảm bảo không trùng lặp.
//                .creditCode("CC-"+ project.getId() +"-" + company.getId() + "-"+ System.currentTimeMillis())
//                .build();
//        log.info("Creating a new CarbonCredit batch with code {} for company {}", newCredit.getCreditCode(), company.getCompanyName());
//
//        //B6 Lưu vào Cơ sở dữ liệu
//        return carbonCreditRepository.save(newCredit);
//    }
//
//    @Override
//    public ProjectResponse finalApprove(Long projectId, ProjectStatus status) {
//        //B1 xac dinh user
//        User user = currentUser();
//
//        // B2 tim admin
//        Admin admin = adminRepository.findByUserId(user.getId())
//                .orElseThrow(() -> new AppException(ErrorCode.ADMIN_NOT_FOUND));
//
//        //B3 tim project theo company
//        Project p = projectRepository.findByIdWithCompany(projectId)
//                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));
//
//        //Neu project chua duoc CVA Approved thi khong cho
//        if(p.getStatus() != ProjectStatus.CVA_APPROVED){
//            throw new AppException(ErrorCode.CVA_NOT_APPROVED);
//        }
//
//        //Neu project chua duoc admin approved hoac project bi tu choi thi in ra loi
//        if(status != ProjectStatus.ADMIN_APPROVED && status != ProjectStatus.REJECTED){
//            throw new AppException(ErrorCode.INVALID_FINAL_APPROVAL_STATUS);
//        }
//
//        // B4 Set admin lan cuoi duyet , doi trang thai
//        p.setFinalReviewer(admin);
//        p.setReviewNote("Final decision by" + admin.getName());
//        p.setStatus(status);
//
//        // B5 save thong tin
//        Project saveProject =  projectRepository.save(p);
//        log.info("Project {} final-reviewd by admin {}", saveProject.getId(), admin.getName());
//
//        //B6 tra ve response cho client
//        return ProjectResponse.builder()
//                .id(saveProject.getId())
//                .title(saveProject.getTitle())
//                .description(saveProject.getDescription())
//                .status(saveProject.getStatus())
//                .createdAt(saveProject.getCreateAt())
//                .companyName(saveProject.getCompany() != null ? saveProject.getCompany().getCompanyName() : null)
//                .reviewer(saveProject.getReviewer())
//                .build();
//    }
//
//    @Override
//    public Page<ProjectResponse> adminListReviewedByCva(Long cvaId, Pageable pageable) {
//        // B1 tim user
//        User user = currentUser();
//        // B2 list ra cac du an duoc CVA approve , hoac reject
//        List<ProjectStatus> statuses = List.of(ProjectStatus.CVA_APPROVED, ProjectStatus.REJECTED);
//
//        //B3 tim cva
//        Cva cvaReviewer = cvaRepository.findByUser_Id(user.getId()).
//                orElseThrow(() -> new AppException(ErrorCode.CVA_NOT_FOUND));
//
//        //B4 tim list ra cac project duoc cva duyet tren client admin
//        Page<Project> projectPage = projectRepository.findByReviewerAndStatusIn(cvaReviewer.getName(), statuses, pageable);
//
//        // B5 tra ve response co chua cac thong tin project duoc tim thay
//        List<ProjectResponse> responses = projectPage.getContent().stream()
//                .map(project -> ProjectResponse.builder()
//                        .id(project.getId())
//                        .title(project.getTitle())
//                        .description(project.getDescription())
//                        .status(project.getStatus())
//                        .createdAt(project.getCreateAt())
//                        .companyName(project.getCompany() != null ? project.getCompany().getCompanyName() : null)
//                        .reviewer(project.getReviewer())
//                        .build())
//                .toList();
//
//        return new PageImpl<>(responses, pageable, projectPage.getTotalElements());
//    }
//
//    @Override
//    public Page<ProjectResponse> adminInbox(Pageable pageable) {
//        //
//        Page<Project> projectPage = projectRepository.findAllCvaApproved(pageable);
//
//        List<ProjectResponse> responses = projectPage.getContent().stream()
//                .map(project -> ProjectResponse.builder()
//                        .id(project.getId())
//                        .title(project.getTitle())
//                        .description(project.getDescription())
//                        .status(project.getStatus())
//                        .createdAt(project.getCreateAt())
//                        .companyName(project.getCompany() != null ? project.getCompany().getCompanyName() : null)
//                        .reviewer(project.getReviewer())
//                        .build())
//                .toList();
//
//        return new PageImpl<>(responses, pageable, projectPage.getTotalElements());
//    }
//
//    @Override
//    public CarbonCredit approveCarbonCredit(Long carbonCreditId) throws WalletException {
//        // B1 tìm carbon theo id carbon credit
//        CarbonCredit credit = carbonCreditRepository.findById(carbonCreditId)
//                .orElseThrow(() -> new ResourceNotFoundException("CarbonCredit not found with id: " + carbonCreditId));
//
//        // B2 neu status cua credit khong pending thi khong duyet
//        if(credit.getStatus() != CreditStatus.PENDING){
//            throw new AppException(ErrorCode.CARBON_CREDIT_NOT_PENDING);
//        }
//
//        // B3 set approve status
//        credit.setStatus(CreditStatus.APPROVED);
//        credit.setIssueAt(LocalDateTime.now());
//
//        CarbonCredit savedCarbonCredit = carbonCreditRepository.save(credit);
//
//        //B4 gui credit vao vi company
//        Company company = savedCarbonCredit.getCompany();
//        Wallet wallet = walletRepository.findByUserId(company.getUser().getId());
//
//        // 4.1 neu company chua co wallet thi phai tao
//        if(wallet == null){
//            wallet = walletService.getUserWallet();
//            company.setWallet(wallet);
//        }
//
//        // 4.2 neu wallet ko co tin chi carbon thi phai set bang 0
//        if(wallet.getCarbonCreditBalance() == null){
//            wallet.setCarbonCreditBalance(BigDecimal.ZERO);
//        }
//
//        //B5 luu lai balance vao trong wallet
//
//        wallet.setCarbonCreditBalance(wallet.getCarbonCreditBalance().add(savedCarbonCredit.getCarbonCredit()));
//        walletRepository.save(wallet);
//
//        log.info("Admin approved CarbonCredit id {}. Transferred {} credits to wallet of company {}",
//                savedCarbonCredit.getId(), savedCarbonCredit.getCarbonCredit(), company.getCompanyName());
//
//        return savedCarbonCredit;
//    }
//
//}
