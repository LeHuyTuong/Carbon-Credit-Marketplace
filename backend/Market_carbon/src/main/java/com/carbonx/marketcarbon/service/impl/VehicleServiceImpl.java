package com.carbonx.marketcarbon.service.impl;



import com.carbonx.marketcarbon.dto.response.VehicleResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.EVOwner;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Vehicle;
import com.carbonx.marketcarbon.repository.CompanyRepository;
import com.carbonx.marketcarbon.repository.EVOwnerRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.VehicleRepository;
import com.carbonx.marketcarbon.dto.request.VehicleCreateRequest;
import com.carbonx.marketcarbon.dto.request.VehicleUpdateRequest;
import com.carbonx.marketcarbon.service.VehicleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final EVOwnerRepository evOwnerRepository;

    @Override
    public VehicleResponse create(VehicleCreateRequest req) {
        // Check owner
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        EVOwner evOwner = evOwnerRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        if(evOwner == null){
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }
        if(vehicleRepository.existsByPlateNumber(req.getPlateNumber())){
            throw new AppException(ErrorCode.VEHICLE_PLATE_EXISTS);
        }
        Company company = companyRepository.findById(req.getCompanyId()).orElseThrow(
                () -> new ResourceNotFoundException("Company not found: id = " + req.getCompanyId()));

        // B1 add data form request
        Vehicle vehicle = Vehicle.builder()
                .plateNumber(req.getPlateNumber())
                .brand(req.getBrand())
                .model(req.getModel())
                .evOwner(evOwner)
                .company(company)
                .build();
        // B2 save data on repo
        vehicleRepository.save(vehicle);

        log.info("Vehicle created: {}", vehicle.getBrand());
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .plateNumber(req.getPlateNumber())
                .brand(req.getBrand())
                .model(req.getModel())
                .companyId(company.getId())
                .build();
    }

    // cho EV Owner . ko trả về thời gian tạo và thời gian update
    public List<VehicleResponse> getOwnerVehicles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User owner =  userRepository.findByEmail(email);
        if(owner == null){
            throw new ResourceNotFoundException("User not found with email: " + email);
        }
        return vehicleRepository.findByEvOwner_Id(owner.getId())
                .stream()
                .map(VehicleResponse::from)
                .toList();
    }

    @Override
    public VehicleResponse update(Long id, VehicleUpdateRequest req) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        EVOwner evOwner = evOwnerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("EV Owner not found with email: " + email));
        if(evOwner == null){
            throw new ResourceNotFoundException("User not found with email: " + email);
        }

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow( () -> new ResourceNotFoundException("Vehicle not found") );

        if(vehicleRepository.existsByPlateNumber(req.getPlateNumber())){
            throw new AppException(ErrorCode.VEHICLE_PLATE_EXISTS);
        }

        Company company = companyRepository.findById(req.getCompanyId()).orElseThrow(
                () -> new ResourceNotFoundException("Company not found: id = " + req.getCompanyId()));

        vehicle.setPlateNumber(req.getPlateNumber());
        vehicle.setBrand(req.getBrand());
        vehicle.setModel(req.getModel());
        vehicle.setEvOwner(evOwner);
        vehicle.setCompany(company);

        vehicleRepository.save(vehicle);
        log.info("Vehicle updated successfully");
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .plateNumber(req.getPlateNumber())
                .brand(req.getBrand())
                .model(req.getModel())
                .companyId(company.getId())
                .build();
    }

    @Override
    public void delete(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow( () -> new ResourceNotFoundException("Vehicle not found") );
        vehicleRepository.delete(vehicle);
        log.info("Vehicle deleted successfully");
    }

}
