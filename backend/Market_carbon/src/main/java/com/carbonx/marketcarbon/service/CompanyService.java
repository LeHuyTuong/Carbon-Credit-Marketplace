package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.Project;

import java.util.List;

public interface CompanyService {
    //TODO : join project
    public void assignProject(Company company, Project project);
    public void removeProject(Company company, Project project);
    public List<Project> getProjects(Company company);


    public void createCompany(Company company);
    public void updateCompany(Long id, Company company);
    public void deleteCompany(Long id);
    public void getCompanyById(Long id);
    public List<Company> getAllCompanies();
}
