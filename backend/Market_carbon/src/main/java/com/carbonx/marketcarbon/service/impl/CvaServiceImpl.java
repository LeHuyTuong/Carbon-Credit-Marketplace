package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.ProjectStatus;
import com.carbonx.marketcarbon.dto.request.ProjectReviewRequest;
import com.carbonx.marketcarbon.dto.response.ProjectResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.mapper.ProjectMapper;
import com.carbonx.marketcarbon.model.Cva;
import com.carbonx.marketcarbon.model.Project;
import com.carbonx.marketcarbon.repository.CvaRepository;
import com.carbonx.marketcarbon.repository.ProjectRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.service.CvaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CvaServiceImpl implements CvaService {

    private final ProjectRepository projectRepository;
    private final CvaRepository cvaRepository;
    private final ProjectMapper projectMapper;
    private final UserRepository userRepository;

    @Override
    public ProjectResponse review(ProjectReviewRequest request) {
        Project p = projectRepository.findByIdWithCompany(request.getProjectId())
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        // Chỉ được thẩm định khi đang chờ duyệt
        if (p.getStatus() != ProjectStatus.PENDING_REVIEW && p.getStatus() != ProjectStatus.UNDER_REVIEW) {
            throw new AppException(ErrorCode.INVALID_STATE_TRANSITION);
        }

        var auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Cva reviewer = cvaRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.CVA_NOT_FOUND));

        p.setReviewer(reviewer);
        p.setReviewNote(request.getReviewNote());

        if (request.getDecision() == ProjectStatus.CVA_APPROVED) {
            p.setStatus(ProjectStatus.CVA_APPROVED);
        } else {
            p.setStatus(ProjectStatus.REJECTED);
        }

        Project saved = projectRepository.save(p);
        log.info(" Project {} reviewed by CVA {}", p.getId(), reviewer.getName());
        return projectMapper.toResponse(saved);
    }

    public Page<ProjectResponse> cvaInbox(boolean assignedOnly, Pageable pageable) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var user = userRepository.findByEmail(auth.getName());
        if (user == null) throw new AppException(ErrorCode.USER_NOT_EXISTED);

        var cva = cvaRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.CVA_NOT_FOUND));

        var statuses = List.of(ProjectStatus.PENDING_REVIEW, ProjectStatus.UNDER_REVIEW);
        Page<Project> page = assignedOnly
                ? projectRepository.findInboxAssigned(cva.getId(), statuses, pageable)
                : projectRepository.findInboxUnassigned(statuses, pageable);

        return page.map(projectMapper::toResponse);
    }
}
