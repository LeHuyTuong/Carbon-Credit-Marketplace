package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.dto.request.VehicleCreateRequest;
import com.carbonx.marketcarbon.dto.request.VehicleUpdateRequest;
import com.carbonx.marketcarbon.dto.response.CompanyVehicleSummaryResponse;
import com.carbonx.marketcarbon.dto.response.VehicleResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.EVOwner;
import com.carbonx.marketcarbon.model.Vehicle;
import com.carbonx.marketcarbon.repository.CompanyRepository;
import com.carbonx.marketcarbon.repository.EVOwnerRepository;
import com.carbonx.marketcarbon.repository.VehicleRepository;
import com.carbonx.marketcarbon.service.S3Service;
import com.carbonx.marketcarbon.service.VehicleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final CompanyRepository companyRepository;
    private final EVOwnerRepository evOwnerRepository;
    private final S3Service s3Service;

    @Override
    public VehicleResponse create(VehicleCreateRequest req) {
        //  Lấy EV Owner hiện tại
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        EVOwner evOwner = evOwnerRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        //  Kiểm tra trùng biển số
        if (vehicleRepository.existsByPlateNumber(req.getPlateNumber())) {
            throw new AppException(ErrorCode.VEHICLE_PLATE_EXISTS);
        }

        //  Kiểm tra company hợp lệ
        Company company = companyRepository.findById(req.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: id = " + req.getCompanyId()));

        //  Upload giấy tờ xe / ảnh xe lên S3
        String documentUrl = null;
        if (req.getDocumentFile() != null && !req.getDocumentFile().isEmpty()) {
            documentUrl = s3Service.uploadFile(req.getDocumentFile());
        }

        //  Tạo vehicle mới
        Vehicle vehicle = Vehicle.builder()
                .plateNumber(req.getPlateNumber())
                .brand(req.getBrand())
                .model(req.getModel())
                .evOwner(evOwner)
                .company(company)
                .documentUrl(documentUrl)
                .build();

        vehicleRepository.save(vehicle);
        log.info(" Vehicle created with S3 document: {}", documentUrl);

        return VehicleResponse.builder()
                .id(vehicle.getId())
                .plateNumber(vehicle.getPlateNumber())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .companyId(company.getId())
                .documentUrl(documentUrl)
                .build();
    }

    //  Lấy danh sách xe của EV Owner hiện tại
    @Override
    public List<VehicleResponse> getOwnerVehicles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        EVOwner evOwner = evOwnerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("EV Owner not found with email: " + email));

        return vehicleRepository.findByEvOwner_Id(evOwner.getId())
                .stream()
                .map(VehicleResponse::from)
                .toList();
    }

    @Override
    public long countMyVehicles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        EVOwner evOwner = evOwnerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("EV Owner not found with email: " + email));

        return vehicleRepository.countByEvOwner_Id(evOwner.getId());
    }

    //  Update thông tin xe
    @Override
    public VehicleResponse update(Long id, VehicleUpdateRequest req) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        EVOwner evOwner = evOwnerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("EV Owner not found with email: " + email));

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        Company company = companyRepository.findById(req.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: id = " + req.getCompanyId()));

        vehicle.setPlateNumber(req.getPlateNumber());
        vehicle.setBrand(req.getBrand());
        vehicle.setModel(req.getModel());
        vehicle.setEvOwner(evOwner);
        vehicle.setCompany(company);

        if (req.getDocumentFile() != null && !req.getDocumentFile().isEmpty()) {
            vehicle.setDocumentUrl(s3Service.uploadFile(req.getDocumentFile()));
        }

        vehicleRepository.save(vehicle);
        log.info(" Vehicle updated successfully");

        return VehicleResponse.builder()
                .id(vehicle.getId())
                .plateNumber(vehicle.getPlateNumber())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .companyId(company.getId())
                .documentUrl(vehicle.getDocumentUrl())
                .build();
    }

    @Override
    public void delete(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        vehicleRepository.delete(vehicle);
        log.info(" Vehicle deleted successfully");
    }

    @Override
    public List<CompanyVehicleSummaryResponse> getCompanyVehicleSummary() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        //  Gọi đúng method trong repository
        Company company = companyRepository.findByUserEmail(email);
        if (company == null) {
            throw new ResourceNotFoundException("Company not found with email: " + email);
        }

        List<Vehicle> vehicles = vehicleRepository.findByCompany_Id(company.getId());

        return vehicles.stream()
                .collect(Collectors.groupingBy(Vehicle::getEvOwner))
                .entrySet()
                .stream()
                .map(entry -> {
                    EVOwner owner = entry.getKey();
                    List<Vehicle> ownerVehicles = entry.getValue();

                    List<CompanyVehicleSummaryResponse.VehicleItem> vehicleItems = ownerVehicles.stream()
                            .map(v -> CompanyVehicleSummaryResponse.VehicleItem.builder()
                                    .id(v.getId())
                                    .plateNumber(v.getPlateNumber())
                                    .brand(v.getBrand())
                                    .model(v.getModel())
                                    .build())
                            .toList();

                    //  Sửa lại getFullName() → getName()
                    return CompanyVehicleSummaryResponse.builder()
                            .ownerId(owner.getId())
                            .fullName(owner.getName())
                            .email(owner.getEmail())
                            .vehicleCount(vehicleItems.size())
                            .vehicles(vehicleItems)
                            .build();
                })
                .toList();
    }
}
