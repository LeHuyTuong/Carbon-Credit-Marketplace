package com.carbonx.marketcarbon.service.impl;



import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Vehicle;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.VehicleRepository;
import com.carbonx.marketcarbon.request.VehicleCreateRequest;
import com.carbonx.marketcarbon.request.VehicleUpdateRequest;
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

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    @Override
    public VehicleResponse create(VehicleCreateRequest req) {
        // Check owwner id is exist
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User owner = userRepository.findByEmail(email);
        if(owner == null){
            throw new ResourceNotFoundException("User not found with email: " + email);
        }
        // B1 add data form request
        Vehicle vehicle = Vehicle.builder()
                .plateNumber(req.getPlateNumber())
                .brand(req.getBrand())
                .model(req.getModel())
                .yearOfManufacture(req.getYear())
                .owner(owner)
                .build();
        // B2 save data on repo
        vehicleRepository.save(vehicle);
        //B3 add data to response
        VehicleResponse vehicleResponse = VehicleResponse
                .builder()
                .id(vehicle.getId())
                .ownerId(owner.getId())
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
    public VehicleResponse getByPlateNumber(String plateNumber) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User owner = userRepository.findByEmail(email);
        if(owner == null){
            throw new ResourceNotFoundException("User not found with email: " + email);
        }

        //B1 : xác định xe bằng ID , nếu ko thì trả về exception
        Vehicle vehicle = (Vehicle) vehicleRepository.findByPlateNumber(plateNumber)
                .orElseThrow( () -> new ResourceNotFoundException("Vehicle not found") );
        //B2: Trả về response
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .ownerId(owner.getId())
                .plateNumber(vehicle.getPlateNumber())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .yearOfManufacture(vehicle.getYearOfManufacture())
                .build();
    }

    @Override
    public VehicleResponse update(Long id, VehicleUpdateRequest req) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User owner = userRepository.findByEmail(email);
        if(owner == null){
            throw new ResourceNotFoundException("User not found with email: " + email);
        }

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow( () -> new ResourceNotFoundException("Vehicle not found") );
        vehicle.setYearOfManufacture(req.getYear());
        vehicle.setPlateNumber(req.getPlateNumber());
        vehicle.setBrand(req.getBrand());
        vehicle.setModel(req.getModel());

        vehicleRepository.save(vehicle);
        log.info("Vehicle updated successfully");
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .ownerId(owner.getId())
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
