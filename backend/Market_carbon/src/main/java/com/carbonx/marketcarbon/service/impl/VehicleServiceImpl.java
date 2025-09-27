package com.carbonx.marketcarbon.service.impl;



import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Vehicle;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.VehicleRepository;
import com.carbonx.marketcarbon.request.VehicleCreateRequest;
import com.carbonx.marketcarbon.response.VehicleResponse;
import com.carbonx.marketcarbon.service.VehicleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    @Override
    public VehicleResponse create(VehicleCreateRequest req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName(); // JWT phải có email/username trong claim "sub"
        User owner = userRepository.findByEmail(email);
        if (owner == null) {
            throw new ResourceNotFoundException("Owner not found with email: " + email);
        }

        // B1 add data form request
        Vehicle vehicle = Vehicle.builder()
                .plateNumber(req.getPlateNumber())
                .brand(req.getBrand())
                .model(req.getModel())
                .yearOfManufacture(req.getYearOfManufacture())
                .owner(owner)
                .build();
        // B2 save data on repo
        vehicleRepository.save(vehicle);
        //B3 add data to response
        VehicleResponse vehicleResponse = VehicleResponse
                .builder()
                .id(vehicle.getId())
                .ownerId(vehicle.getOwner().getId())
                .plateNumber(vehicle.getPlateNumber())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .yearOfManufacture(vehicle.getYearOfManufacture())
                .build();

        log.info("Vehicle created: {}", vehicleResponse);
        return vehicleResponse;
    }

    @Override
    public List<VehicleResponse> getAll() {

        return vehicleRepository.findAll().stream()
                .map(vehicle -> VehicleResponse.builder()
                        .id(vehicle.getId())
                        .ownerId(vehicle.getOwner().getId())
                        .brand(vehicle.getBrand())
                        .plateNumber(vehicle.getPlateNumber())
                        .model(vehicle.getModel())
                        .yearOfManufacture(vehicle.getYearOfManufacture())
                        .build())
                .toList(); // return về 1 list của VehicleReponse
    }

    @Override
    public VehicleResponse getById(Long id) {
        //B1 : xác định xe bằng ID , nếu ko thì trả về exception
        Vehicle vehicle =  vehicleRepository.findById(id)
                .orElseThrow( () -> new ResourceNotFoundException("Vehicle not found") );
        //B2: Trả về response
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .ownerId(vehicle.getOwner().getId())
                .plateNumber(vehicle.getPlateNumber())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .yearOfManufacture(vehicle.getYearOfManufacture())
                .build();
    }

    @Override
    public VehicleResponse update(Long id, VehicleCreateRequest req) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow( () -> new ResourceNotFoundException("Vehicle not found") );
        vehicle.setPlateNumber(req.getPlateNumber());
        vehicle.setBrand(req.getBrand());
        vehicle.setModel(req.getModel());

        vehicleRepository.save(vehicle);
        log.info("Vehicle updated successfully");
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .ownerId(vehicle.getOwner().getId())
                .plateNumber(vehicle.getPlateNumber())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .yearOfManufacture(vehicle.getYearOfManufacture())
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
