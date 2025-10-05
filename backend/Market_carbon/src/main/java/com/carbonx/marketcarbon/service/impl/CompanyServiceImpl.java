package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.Project;
import com.carbonx.marketcarbon.repository.CompanyRepository;
import com.carbonx.marketcarbon.repository.VehicleRepository;
import com.carbonx.marketcarbon.service.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyServiceImpl implements CompanyService {
    private final CompanyRepository companyRepository;
    private final VehicleRepository vehicleRepository;


    @Override
    public void assignProject(Company company, Project project) {

    }

    @Override
    public void removeProject(Company company, Project project) {

    }

    @Override
    public List<Project> getProjects(Company company) {
        return List.of();
    }

    @Override
    public void createCompany(Company company) {

    }

    @Override
    public void updateCompany(Long id, Company company) {

    }

    @Override
    public void deleteCompany(Long id) {

    }

    @Override
    public void getCompanyById(Long id) {

    }

    @Override
    public List<Company> getAllCompanies() {
        return List.of();
    }
}
