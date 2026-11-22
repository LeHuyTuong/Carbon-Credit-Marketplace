package com.carbonx.marketcarbon.common.security;

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
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Điểm truy cập duy nhất để lấy thông tin người dùng hiện tại từ SecurityContext.
 * Email trong JWT được dùng làm {@code authentication.getName()}.
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private static final String ANONYMOUS_USER = "anonymousUser";

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final EVOwnerRepository evOwnerRepository;
    private final CvaRepository cvaRepository;
    private final AdminRepository adminRepository;

    /** @throws AppException UNAUTHENTICATED khi không có authentication hoặc là anonymous. */
    public String getCurrentEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()
                || ANONYMOUS_USER.equalsIgnoreCase(authentication.getName())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return authentication.getName();
    }

    public User getCurrentUser() {
        User user = userRepository.findByEmail(getCurrentEmail());
        if (user == null) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }
        return user;
    }

    public Company getCurrentCompany() {
        Company company = companyRepository.findByUserEmail(getCurrentEmail());
        if (company == null) {
            throw new AppException(ErrorCode.COMPANY_NOT_FOUND);
        }
        return company;
    }

    public Long getCurrentCompanyId() {
        return getCurrentCompany().getId();
    }

    public EVOwner getCurrentEvOwner() {
        return evOwnerRepository.findByEmail(getCurrentEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    public Cva getCurrentCva() {
        return cvaRepository.findByEmail(getCurrentEmail())
                .orElseThrow(() -> new AppException(ErrorCode.CVA_NOT_FOUND));
    }

    public Admin getCurrentAdmin() {
        return adminRepository.findByUserEmail(getCurrentEmail())
                .orElseThrow(() -> new AppException(ErrorCode.ADMIN_NOT_FOUND));
    }
}
