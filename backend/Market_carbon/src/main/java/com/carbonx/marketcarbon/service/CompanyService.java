package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.Project;

import java.util.List;

public interface CompanyService {
    //TODO : join project
     void assignProject(Company company, Project project);
     void removeProject(Company company, Project project);
     List<Project> getProjects(Company company);


     void createCompany(Company company);
     void updateCompany(Long id, Company company);
     void deleteCompany(Long id);
     void getCompanyById(Long id);
     List<Company> getAllCompanies();
}
