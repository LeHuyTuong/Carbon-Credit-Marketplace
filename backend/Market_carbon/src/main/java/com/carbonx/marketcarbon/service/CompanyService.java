package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.request.importing.ImportReport;
import com.carbonx.marketcarbon.dto.response.ProjectResponse;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CompanyService {
    //TODO : join project
     void assignProject(Company company, Project project);
     void removeProject(Company company, Project project);
     List<Project> getProjects(Company company);
     ProjectResponse sendToReview(Long projectId);
     ImportReport importCsv(MultipartFile file);
     ProjectResponse applyToBaseProject(Long baseProjectId);              // Company chọn base project -> tạo hồ sơ nháp
     Page<ProjectResponse> listBaseProjectChoices(Pageable pageable);


 void createCompany(Company company);
     void updateCompany(Long id, Company company);
     void deleteCompany(Long id);
     void getCompanyById(Long id);
     List<Company> getAllCompanies();
}
