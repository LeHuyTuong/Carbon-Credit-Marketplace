package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.ProjectStatus;
import com.carbonx.marketcarbon.dto.response.ProjectResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.mapper.ProjectMapper;
import com.carbonx.marketcarbon.model.Admin;
import com.carbonx.marketcarbon.model.Project;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.AdminRepository;
import com.carbonx.marketcarbon.repository.ProjectRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    @Override
    public ProjectResponse finalApprove(Long projectId, ProjectStatus status) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User current = userRepository.findByEmail(email);
        if (current == null) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }

        Admin admin = adminRepository.findByUserId(current.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ADMIN_NOT_FOUND));

        Project p = projectRepository.findByIdWithCompany(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        if (p.getStatus() != ProjectStatus.CVA_APPROVED) {
            throw new AppException(ErrorCode.INVALID_STATE_TRANSITION);
        }
        if (status != ProjectStatus.ADMIN_APPROVED && status != ProjectStatus.REJECTED) {
            throw new AppException(ErrorCode.INVALID_FINAL_APPROVAL_STATUS);
        }

        p.setFinalReviewer(admin);
        p.setFinalReviewNote("Final decision by " + admin.getName());
        p.setStatus(status);

        Project saved = projectRepository.save(p);
        log.info(" Project {} final-reviewed by admin {}", saved.getId(), current.getEmail());

        return projectMapper.toResponse(saved);
    }

    public Page<ProjectResponse> adminListReviewedByCva(Long cvaId, Pageable pageable) {
        var statuses = List.of(ProjectStatus.CVA_APPROVED, ProjectStatus.REJECTED);
        return projectRepository.findReviewedByCva(cvaId, statuses, pageable)
                .map(projectMapper::toResponse);
    }

    public Page<ProjectResponse> adminInbox(Pageable pageable) {
        return projectRepository.findAllCvaApproved(pageable)
                .map(projectMapper::toResponse);
    }
}
