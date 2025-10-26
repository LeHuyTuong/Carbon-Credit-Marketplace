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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
    static final String ADMIN_PASSWORD = "Password@1";

    // --- New Company Constants ---
    @NonFinal
    static final String COMPANY_USER_EMAIL = "company@example.com";
    @NonFinal
    static final String COMPANY_PASSWORD = "Password@1";
    // --- End New Company Constants ---

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
            Role evOwnerRole = roleRepository.findByName(PredefinedRole.USER_ROLE)
                    .orElseGet(() -> roleRepository.save(Role.builder().name(PredefinedRole.USER_ROLE).description("EV_Owner role").build()));

            Role adminRole = roleRepository.findByName(PredefinedRole.ADMIN_ROLE)
                    .orElseGet(() -> roleRepository.save(Role.builder().name(PredefinedRole.ADMIN_ROLE).description("role Admin").build()));

            Role companyRole = roleRepository.findByName(PredefinedRole.COMPANY_ROLE)
                    .orElseGet(() -> roleRepository.save(Role.builder().name(PredefinedRole.COMPANY_ROLE).description("COMPANY role").build()));

            Role cvaRole = roleRepository.findByName(PredefinedRole.CVA_ROLE)
                    .orElseGet(() -> roleRepository.save(Role.builder().name(PredefinedRole.CVA_ROLE).description("CVA role").build()));

            // Initialize Admin User
            User adminUser = userRepository.findByEmail(ADMIN_USER_NAME);
            if (adminUser == null) {
                adminUser = User.builder()
                        .email(ADMIN_USER_NAME)
                        .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD))
                        .roles(new HashSet<>(Set.of(adminRole)))
                        .status(USER_STATUS.ACTIVE)
                        .build();
                userRepository.save(adminUser);
                log.warn("Admin user created with default password.");
            }

            final User finalAdminUser = adminUser;
            Company adminCompany = companyRepository.findByUserId(finalAdminUser.getId())
                    .orElseGet(() -> {
                        log.info("Creating a test company for admin user...");
                        Company newCompany = Company.builder()
                                .user(finalAdminUser)
                                .companyName("Admin Test Corp")
                                .businessLicense("ADMIN_LIC")
                                .taxCode("ADMIN_TAX")
                                .address("123 Admin Street")
                                .build();
                        return companyRepository.save(newCompany);
                    });

            // Initialize a Wallet for the Admin user
            if (walletRepository.findByUserId(adminUser.getId()) == null) {
                log.info("Creating a wallet for admin user...");
                Wallet newWallet = Wallet.builder()
                        .user(adminUser)
                        .balance(BigDecimal.valueOf(1_000_000))
                        .carbonCreditBalance(BigDecimal.ZERO)
                        .company(adminCompany)
                        .build();
                walletRepository.save(newWallet);
            }

            // Initialize a test Project
            Project testProject = projectRepository.findByTitle("Test Project for Credits")
                    .orElseGet(() -> {
                        log.info("Creating a test project...");
                        Project newProject = Project.builder()
                                .title("Test Project for Credits")
                                .description("A sample project for generating carbon credits.")
                                .logo("https://example.com/default_logo.png")
                                .status(ProjectStatus.OPEN)
                                .build();
                        return projectRepository.save(newProject);
                    });

            // --- Initialize New Company User ---
            User companyUser = userRepository.findByEmail(COMPANY_USER_EMAIL);
            if (companyUser == null) {
                companyUser = User.builder()
                        .email(COMPANY_USER_EMAIL)
                        .passwordHash(passwordEncoder.encode(COMPANY_PASSWORD))
                        .roles(new HashSet<>(Set.of(companyRole)))
                        .status(USER_STATUS.ACTIVE)
                        .build();
                userRepository.save(companyUser);
                log.info("Created new Company user: {}", COMPANY_USER_EMAIL);
            }

            // --- Initialize Company Entity and Wallet for the new user ---
            final User finalCompanyUser = companyUser;
            Company newRegisteredCompany = companyRepository.findByUserId(finalCompanyUser.getId())
                    .orElseGet(() -> {
                        log.info("Creating Company entity for user: {}", finalCompanyUser.getEmail());
                        Company newCompany = Company.builder()
                                .user(finalCompanyUser)
                                .companyName("Example Corp")
                                .businessLicense("COMP_LIC_111")
                                .taxCode("COMP_TAX_222")
                                .address("456 Company Ave")
                                .build();
                        return companyRepository.saveAndFlush(newCompany);
                    });

            // --- Create Wallet Separately ---
            if (walletRepository.findByUserId(finalCompanyUser.getId()) == null) {
                log.info("Creating Wallet for company user: {}", finalCompanyUser.getEmail());
                Wallet companyWallet = Wallet.builder()
                        .user(finalCompanyUser)
                        .balance(new BigDecimal("500000.00"))
                        .carbonCreditBalance(BigDecimal.ZERO)
                        .company(newRegisteredCompany)
                        .build();
                walletRepository.save(companyWallet);
            }

            // --- Initialize an ISSUED Carbon Credit block for the new Company ---
            String companyCreditCode = "COMP-ISSUED-001";
            boolean creditExistsForCompany =
                    carbonCreditRepository.findByCreditCode(companyCreditCode)
                            .stream()
                            .anyMatch(cc -> cc.getCompany() != null
                                    && cc.getCompany().getId() != null
                                    && cc.getCompany().getId().equals(newRegisteredCompany.getId()));

            if (!creditExistsForCompany) {
                log.info("Creating an ISSUED carbon credit block for company: {}", newRegisteredCompany.getCompanyName());
                CarbonCredit companyCredit = CarbonCredit.builder()
                        .creditCode(companyCreditCode)
                        .carbonCredit(new BigDecimal("2500.00"))
                        .company(newRegisteredCompany)
                        .project(testProject)
                        .status(CreditStatus.ISSUE)
                        .listedAmount(BigDecimal.ZERO)
                        .issuedAt(OffsetDateTime.now(ZoneOffset.ofHours(7)))
                        .name("Sample Issued Credits - " + newRegisteredCompany.getCompanyName())
                        .build();
                carbonCreditRepository.save(companyCredit);
                log.info("Created ISSUED CarbonCredit with code {} for company {}", companyCreditCode, newRegisteredCompany.getCompanyName());
            }

            // --- Initialize Credits and Listings for Admin Company (Existing Logic) ---
            String adminIssuedCreditCode1 = "ADMIN-ISSUED-001";
            CarbonCredit adminListableCredit1 = carbonCreditRepository.findByCreditCode(adminIssuedCreditCode1)
                    .stream().findFirst()
                    .orElseGet(() -> {
                        log.info("Creating ADMIN sample ISSUED carbon credit 1 for listing...");
                        return carbonCreditRepository.save(CarbonCredit.builder()
                                .creditCode(adminIssuedCreditCode1)
                                .carbonCredit(new BigDecimal("5000.00"))
                                .company(adminCompany)
                                .project(testProject)
                                .status(CreditStatus.ISSUE)
                                .listedAmount(BigDecimal.ZERO)
                                .issuedAt(OffsetDateTime.now(ZoneOffset.ofHours(7)))
                                .name("Admin Sample Listable Credits 1 - 2025")
                                .build());
                    });

            String adminIssuedCreditCode2 = "ADMIN-ISSUED-002";
            CarbonCredit adminListableCredit2 = carbonCreditRepository.findByCreditCode(adminIssuedCreditCode2)
                    .stream().findFirst()
                    .orElseGet(() -> {
                        log.info("Creating ADMIN sample ISSUED carbon credit 2 for listing...");
                        return carbonCreditRepository.save(CarbonCredit.builder()
                                .creditCode(adminIssuedCreditCode2)
                                .carbonCredit(new BigDecimal("3000.00"))
                                .company(adminCompany)
                                .project(testProject)
                                .status(CreditStatus.ISSUE)
                                .listedAmount(BigDecimal.ZERO)
                                .issuedAt(OffsetDateTime.now(ZoneOffset.ofHours(7)))
                                .name("Admin Sample Listable Credits 2 - 2025")
                                .build());
                    });

            // Initialize Marketplace Listings for Admin's Company (if none exist)
            if (marketplaceListingRepository.count() == 0) {
                log.info("Creating sample marketplace listings for Admin's company...");

                MarketPlaceListing listing1 = MarketPlaceListing.builder()
                        .company(adminCompany)
                        .carbonCredit(adminListableCredit1)
                        .quantity(new BigDecimal("1500.00"))
                        .pricePerCredit(new BigDecimal("25.50"))
                        .status(ListingStatus.AVAILABLE)
                        .expiresAt(LocalDateTime.now().plusDays(30))
                        .build();
                marketplaceListingRepository.save(listing1);
                adminListableCredit1.setCarbonCredit(adminListableCredit1.getCarbonCredit().subtract(listing1.getQuantity()));
                adminListableCredit1.setListedAmount(adminListableCredit1.getListedAmount().add(listing1.getQuantity()));
                carbonCreditRepository.save(adminListableCredit1);

                MarketPlaceListing listing2 = MarketPlaceListing.builder()
                        .company(adminCompany)
                        .carbonCredit(adminListableCredit1)
                        .quantity(new BigDecimal("700.00"))
                        .pricePerCredit(new BigDecimal("23.50"))
                        .status(ListingStatus.AVAILABLE)
                        .expiresAt(LocalDateTime.now().plusDays(60))
                        .build();
                marketplaceListingRepository.save(listing2);
                adminListableCredit1.setCarbonCredit(adminListableCredit1.getCarbonCredit().subtract(listing2.getQuantity()));
                adminListableCredit1.setListedAmount(adminListableCredit1.getListedAmount().add(listing2.getQuantity()));
                carbonCreditRepository.save(adminListableCredit1);

                MarketPlaceListing listing3 = MarketPlaceListing.builder()
                        .company(adminCompany)
                        .carbonCredit(adminListableCredit2)
                        .quantity(new BigDecimal("1000.00"))
                        .pricePerCredit(new BigDecimal("21.50"))
                        .status(ListingStatus.AVAILABLE)
                        .expiresAt(LocalDateTime.now().plusDays(25))
                        .build();
                marketplaceListingRepository.save(listing3);
                adminListableCredit2.setCarbonCredit(adminListableCredit2.getCarbonCredit().subtract(listing3.getQuantity()));
                adminListableCredit2.setListedAmount(adminListableCredit2.getListedAmount().add(listing3.getQuantity()));
                carbonCreditRepository.save(adminListableCredit2);

                log.info("Sample MarketPlaceListings created.");
            }

            String adminPendingCreditCode = "ADMIN-PENDING-001";
            if (carbonCreditRepository.findByCreditCode(adminPendingCreditCode).isEmpty()) {
                log.info("Creating a sample PENDING carbon credit for the admin's company...");
                CarbonCredit pendingCredit = CarbonCredit.builder()
                        .creditCode(adminPendingCreditCode)
                        .carbonCredit(new BigDecimal("1000.00"))
                        .company(adminCompany)
                        .project(testProject)
                        .status(CreditStatus.AVAILABLE)
                        .name("Admin Sample Pending Credits - 2025")
                        .build();
                carbonCreditRepository.save(pendingCredit);
                log.info("Sample PENDING CarbonCredit created with ID: {}. Admin can approve it.",
                        pendingCredit.getId());
            }

            log.info("Application initialization completed.");
        };
    }
}
