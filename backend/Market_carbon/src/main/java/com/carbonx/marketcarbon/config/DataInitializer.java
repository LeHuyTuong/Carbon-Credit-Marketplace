package com.carbonx.marketcarbon.config;

import com.carbonx.marketcarbon.common.PredefinedRole;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.Role;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.RoleRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
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

    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository, RoleRepository roleRepository) {
        log.info("Initializing application.....");

        return args -> {
            Optional<Role> userRole = roleRepository.findByName(PredefinedRole.USER_ROLE);
            if (userRole.isEmpty()) {
                roleRepository.save(Role.builder()
                        .name(PredefinedRole.USER_ROLE)
                        .description("EV_Owner role")
                        .build());

            }

            Role adminRole = roleRepository.findByName(PredefinedRole.ADMIN_ROLE)
                    .orElseGet(() -> roleRepository.save(
                            Role.builder()
                                    .name(PredefinedRole.ADMIN_ROLE)
                                    .description("role Admin")
                                    .build()
                    ));

            Optional<Role> CompanyRole = roleRepository.findByName(PredefinedRole.COMPANY_ROLE);
            if(CompanyRole.isEmpty()){
                roleRepository.save(Role.builder()
                        .name(PredefinedRole.COMPANY_ROLE)
                        .description("COMPANY role")
                        .build());
                log.info("Created role: {}", PredefinedRole.COMPANY_ROLE);
            }
            Optional<Role> cvaRole = roleRepository.findByName(PredefinedRole.CVA_ROLE);
            if(cvaRole.isEmpty()){
                roleRepository.save(Role.builder()
                        .name(PredefinedRole.CVA_ROLE)
                        .description("CVA role")
                        .build());
            }

            if (userRepository.findByEmail(ADMIN_USER_NAME) == null) {
                Role roleADM = roleRepository.findByName(PredefinedRole.ADMIN_ROLE)
                        .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));

                    User user = User.builder()
                            .email(ADMIN_USER_NAME)
                            .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD))
                            .roles(new HashSet<>(Set.of(adminRole)))
                            .otpCode(null)
                            .otpExpiryDate(LocalDateTime.now().plusMinutes(5)) // optional, có thể để null
                            .build();

                    userRepository.save(user);
                    log.warn("Admin user created with default password: 123456, please change it!");
                }

            log.info("Application initialization completed.");
        };
    }
    }
