package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.request.ProjectRegisterRequest;
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
     List<Project> getProjects(Company company);
      ProjectResponse registerAndSubmit(ProjectRegisterRequest req);
     Page<ProjectResponse> listBaseProjectChoices(Pageable pageable);


}
