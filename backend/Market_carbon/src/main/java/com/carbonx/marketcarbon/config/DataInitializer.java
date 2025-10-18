package com.carbonx.marketcarbon.config;

import com.carbonx.marketcarbon.common.CreditStatus;
import com.carbonx.marketcarbon.common.ListingStatus;
import com.carbonx.marketcarbon.common.PredefinedRole;
import com.carbonx.marketcarbon.common.ProjectStatus;
import com.carbonx.marketcarbon.common.USER_STATUS;
import com.carbonx.marketcarbon.model.*;
import com.carbonx.marketcarbon.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DataInitializer {

    PasswordEncoder passwordEncoder;

    @NonFinal
    static final String ADMIN_USER_NAME = "admin@gmail.com";
    @NonFinal
    static final String ADMIN_PASSWORD = "123456";

    @Bean("databaseInitializer")
    @Transactional
    ApplicationRunner applicationRunner(UserRepository userRepository,
                                        RoleRepository roleRepository,
                                        CompanyRepository companyRepository,
                                        ProjectRepository projectRepository,
                                        CarbonCreditRepository carbonCreditRepository,
                                        WalletRepository walletRepository,
                                        MarketplaceListingRepository marketplaceListingRepository) {
        log.info("Initializing application.....");

        return args -> {
            // Initialize Roles
            if (roleRepository.findByName(PredefinedRole.USER_ROLE).isEmpty()) {
                roleRepository.save(Role.builder().name(PredefinedRole.USER_ROLE).description("EV_Owner role").build());
            }

            Optional<Role> adminRoleOpt = roleRepository.findByName(PredefinedRole.ADMIN_ROLE);
            Role adminRole = adminRoleOpt.orElseGet(() ->
                    roleRepository.save(Role.builder().name(PredefinedRole.ADMIN_ROLE).description("role Admin").build()));


            if (roleRepository.findByName(PredefinedRole.COMPANY_ROLE).isEmpty()) {
                roleRepository.save(Role.builder().name(PredefinedRole.COMPANY_ROLE).description("COMPANY role").build());
            }

            if (roleRepository.findByName(PredefinedRole.CVA_ROLE).isEmpty()) {
                roleRepository.save(Role.builder().name(PredefinedRole.CVA_ROLE).description("CVA role").build());
            }

            // Initialize Admin User
            User adminUser = userRepository.findByEmail(ADMIN_USER_NAME);
            if (adminUser == null) {
                adminUser = User.builder()
                        .email(ADMIN_USER_NAME)
                        .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD))
                        .roles(new HashSet<>(Set.of(adminRole)))
                        .status(USER_STATUS.ACTIVE)
                        .otpCode(null)
                        .otpExpiryDate(LocalDateTime.now().plusMinutes(5))
                        .build();
                userRepository.save(adminUser);
                log.warn("Admin user created with default password: " + ADMIN_PASSWORD);
            }

            // Initialize a Company for the Admin user
            Optional<Company> companyOpt = companyRepository.findByUserId(adminUser.getId());
            Company adminCompany;
            if (companyOpt.isPresent()) {
                adminCompany = companyOpt.get();
            } else {
                log.info("Creating a test company for admin user...");
                Company newCompany = Company.builder()
                        .user(adminUser)
                        .companyName("Admin Test Corp")
                        .businessLicense("123456789")
                        .taxCode("987654321")
                        .address("123 Admin Street")
                        .build();
                adminCompany = companyRepository.save(newCompany);
            }


            // Initialize a Wallet for the Admin user
            Wallet adminWallet = walletRepository.findByUserId(adminUser.getId());
            if (adminWallet == null) {
                log.info("Creating a wallet for admin user...");
                Wallet newWallet = Wallet.builder()
                        .user(adminUser)
                        .balance(BigDecimal.valueOf(1000000))
                        .carbonCreditBalance(new BigDecimal(50000))
                        .company(adminCompany)
                        .build();
                walletRepository.save(newWallet);
            }

            // Initialize a test Project
            Optional<Project> projectOpt = projectRepository.findByTitle("Test Project for Credits");
            Project testProject;
            if(projectOpt.isPresent()){
                testProject = projectOpt.get();
            } else {
                log.info("Creating a test project...");
                Project newProject = Project.builder()
                        .title("Test Project for Credits")
                        .description("A sample project for generating carbon credits.")
                        .logo("https://example.com/default_logo.png")
                        .status(ProjectStatus.OPEN)
                        .build();
                testProject = projectRepository.save(newProject);
            }


            // Initialize an ISSUED Carbon Credit to be listed
            String issuedCreditCode = "ISSUED-CREDIT-001";
            List<CarbonCredit> listableCredits = carbonCreditRepository.findByCreditCode(issuedCreditCode);
            CarbonCredit listableCredit;
            if (!listableCredits.isEmpty()) {
                listableCredit = listableCredits.get(0);
            } else {
                log.info("Creating a sample ISSUED carbon credit for listing...");
                CarbonCredit credit = CarbonCredit.builder()
                        .creditCode(issuedCreditCode)
                        .carbonCredit(new BigDecimal("5000.00"))
                        .company(adminCompany)
                        .project(testProject)
                        .status(CreditStatus.ISSUE)
                        .issueAt(LocalDateTime.now())
                        .name("Sample Listable Credits - 2025")
                        .build();
                listableCredit = carbonCreditRepository.save(credit);
            }


            // Initialize a Marketplace Listing
            if (marketplaceListingRepository.count() == 0) {
                log.info("Creating a sample marketplace listing...");
                MarketPlaceListing listing = MarketPlaceListing.builder()
                        .company(adminCompany)
                        .carbonCredit(listableCredit)
                        .quantity(new BigDecimal("1500.00"))
                        .pricePerCredit(new BigDecimal("25.50"))
                        .status(ListingStatus.AVAILABLE)
                        .expiresAt(LocalDateTime.now().plusDays(30))
                        .build();
                marketplaceListingRepository.save(listing);
                log.info("Sample MarketPlaceListing created with ID: {}. You can use this ID to create an order.", listing.getId());
            }


            // Initialize a PENDING Carbon Credit for the admin's company
            String testCreditCode = "TEST-CREDIT-001";
            if (carbonCreditRepository.findByCreditCode(testCreditCode).isEmpty()) {
                log.info("Creating a sample PENDING carbon credit for the admin's company...");
                CarbonCredit pendingCredit = CarbonCredit.builder()
                        .creditCode(testCreditCode)
                        .carbonCredit(new BigDecimal("1000.00"))
                        .company(adminCompany)
                        .project(testProject)
                        .status(CreditStatus.PENDING)
                        .name("Sample Credits from Test Project - 2025")
                        .build();
                carbonCreditRepository.save(pendingCredit);
                log.info("Sample PENDING CarbonCredit created with ID: {}. Admin can approve it via POST /api/v1/carbonCredit/{}/approve to add credits to the wallet.",
                        pendingCredit.getId(), pendingCredit.getId());
            }

            log.info("Application initialization completed.");
        };
    }
}

