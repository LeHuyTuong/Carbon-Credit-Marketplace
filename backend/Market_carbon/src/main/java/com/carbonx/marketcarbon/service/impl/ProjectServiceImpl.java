package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.Status;
import com.carbonx.marketcarbon.dto.request.ProjectRequest;
import com.carbonx.marketcarbon.dto.response.ProjectDetailResponse;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.Project;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.ProjectRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {
    private final ProjectRepository projectRepository;

    private final UserRepository userRepository;

    @Override
    public void createProject(ProjectRequest req) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if(user == null){
            throw new ResourceNotFoundException("User not found");
        }
        Project project = Project.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .status(Status.PENDING)
                .logo(req.getLogo())
                .build();

        projectRepository.save(project);
        log.info("Project created");
    }

    @Override
    public void updateProject(Long id, ProjectRequest req) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if(user == null){
            throw new ResourceNotFoundException("User not found");
        }

        Project project = projectRepository.findById(id).
                orElseThrow(()  -> new ResourceNotFoundException("Project not found"));

        project.setTitle(req.getTitle());
        project.setDescription(req.getDescription());
        project.setStatus(Status.PENDING);
        project.setLogo(req.getLogo());
        projectRepository.save(project);
        log.info("Project updated");
    }

    @Override
    public void deleteProject(Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if(user == null){
            throw new ResourceNotFoundException("User not found");
        }

        Project project =  projectRepository.findById(id)
                .orElseThrow(()  -> new ResourceNotFoundException("Project not found"));
        projectRepository.delete(project);
    }

    @Override
    public List<ProjectDetailResponse> findAllProject() {
        return projectRepository.findAll().stream()
                .map(project ->  ProjectDetailResponse.builder()
                        .id(project.getId())
                        .title(project.getTitle())
                        .description(project.getDescription())
                        .status(project.getStatus())
                        .logo(project.getLogo())
                        .company(project.getCompany())
                        .updatedAt(project.getUpdatedAt())
                        .createAt(project.getCreateAt())
                        .build()
                )
                .toList();
    }
}
