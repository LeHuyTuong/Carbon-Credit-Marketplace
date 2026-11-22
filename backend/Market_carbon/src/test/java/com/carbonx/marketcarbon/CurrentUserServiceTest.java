package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.common.security.CurrentUserService;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.Admin;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.Cva;
import com.carbonx.marketcarbon.model.EVOwner;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.AdminRepository;
import com.carbonx.marketcarbon.repository.CompanyRepository;
import com.carbonx.marketcarbon.repository.CvaRepository;
import com.carbonx.marketcarbon.repository.EVOwnerRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    private static final String EMAIL = "owner@carbonx.test";

    @Mock private UserRepository userRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private EVOwnerRepository evOwnerRepository;
    @Mock private CvaRepository cvaRepository;
    @Mock private AdminRepository adminRepository;

    @InjectMocks private CurrentUserService currentUserService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        email, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @Test
    void getCurrentEmailReturnsJwtPrincipalName() {
        authenticate(EMAIL);

        assertThat(currentUserService.getCurrentEmail()).isEqualTo(EMAIL);
    }

    @Test
    void getCurrentEmailRejectsMissingAndAnonymousAuthentication() {
        assertThatThrownBy(currentUserService::getCurrentEmail)
                .isInstanceOfSatisfying(AppException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));

        authenticate("anonymousUser");
        assertThatThrownBy(currentUserService::getCurrentEmail)
                .isInstanceOfSatisfying(AppException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));
    }

    @Test
    void getCurrentUserReturnsAuthenticatedUser() {
        authenticate(EMAIL);
        User user = User.builder().id(7L).email(EMAIL).build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(user);

        assertThat(currentUserService.getCurrentUser()).isSameAs(user);
    }

    @Test
    void getCurrentUserThrowsWhenUserIsMissing() {
        authenticate(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(null);

        assertThatThrownBy(currentUserService::getCurrentUser)
                .isInstanceOfSatisfying(AppException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_EXISTED));
    }

    @Test
    void getCurrentCompanyAndIdReturnCompanyOfAuthenticatedUser() {
        authenticate(EMAIL);
        Company company = Company.builder().id(42L).build();
        when(companyRepository.findByUserEmail(EMAIL)).thenReturn(company);

        assertThat(currentUserService.getCurrentCompany()).isSameAs(company);
        assertThat(currentUserService.getCurrentCompanyId()).isEqualTo(42L);
    }

    @Test
    void getCurrentCompanyThrowsWhenCompanyIsMissing() {
        authenticate(EMAIL);
        when(companyRepository.findByUserEmail(EMAIL)).thenReturn(null);

        assertThatThrownBy(currentUserService::getCurrentCompany)
                .isInstanceOfSatisfying(AppException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.COMPANY_NOT_FOUND));
    }

    @Test
    void getCurrentEvOwnerReturnsOwnerProfile() {
        authenticate(EMAIL);
        EVOwner owner = EVOwner.builder().id(7L).build();
        when(evOwnerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));

        assertThat(currentUserService.getCurrentEvOwner()).isSameAs(owner);
    }

    @Test
    void getCurrentEvOwnerThrowsWhenOwnerIsMissing() {
        authenticate(EMAIL);
        when(evOwnerRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(currentUserService::getCurrentEvOwner)
                .isInstanceOfSatisfying(AppException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_EXISTED));
    }

    @Test
    void getCurrentCvaReturnsCvaProfile() {
        authenticate(EMAIL);
        Cva cva = Cva.builder().id(11L).email(EMAIL).build();
        when(cvaRepository.findByEmail(EMAIL)).thenReturn(Optional.of(cva));

        assertThat(currentUserService.getCurrentCva()).isSameAs(cva);
    }

    @Test
    void getCurrentCvaThrowsWhenCvaIsMissing() {
        authenticate(EMAIL);
        when(cvaRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(currentUserService::getCurrentCva)
                .isInstanceOfSatisfying(AppException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CVA_NOT_FOUND));
    }

    @Test
    void getCurrentAdminReturnsAdminProfile() {
        authenticate(EMAIL);
        Admin admin = Admin.builder().id(3L).build();
        when(adminRepository.findByUserEmail(EMAIL)).thenReturn(Optional.of(admin));

        assertThat(currentUserService.getCurrentAdmin()).isSameAs(admin);
    }

    @Test
    void getCurrentAdminThrowsWhenAdminIsMissing() {
        authenticate(EMAIL);
        when(adminRepository.findByUserEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(currentUserService::getCurrentAdmin)
                .isInstanceOfSatisfying(AppException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ADMIN_NOT_FOUND));
    }
}
