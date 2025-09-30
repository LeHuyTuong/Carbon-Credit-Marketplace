package com.carbonx.marketcarbon.service.impl;



import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Vehicle;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.VehicleRepository;
import com.carbonx.marketcarbon.dto.request.VehicleCreateRequest;
import com.carbonx.marketcarbon.dto.request.VehicleUpdateRequest;
import com.carbonx.marketcarbon.service.VehicleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    @Override
    public Long create(VehicleCreateRequest req) {
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
                .yearOfManufacture(req.getYearOfManufacture())
                .user(owner)
                .build();
        // B2 save data on repo
        vehicleRepository.save(vehicle);

        log.info("Vehicle created: {}", vehicle.getBrand());
        return vehicle.getId();
    }

    // cho EV Owner . ko trả về thời gian tạo và thời gian update
    public List<Vehicle> getOwnerVehicles() {
        return vehicleRepository.findAll(). // return về 1 list của Vehicle
        stream()
                .map(vehicle -> Vehicle.builder()
                        .id(vehicle.getId())
                        .user(vehicle.getUser())
                        .brand(vehicle.getBrand())
                        .plateNumber(vehicle.getPlateNumber())
                        .model(vehicle.getModel())
                        .yearOfManufacture(vehicle.getYearOfManufacture())
                        .build())
                .toList();
    }

    @Override
    public Long update(Long id, VehicleUpdateRequest req) {
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
        return vehicle.getId();
    }

    @Override
    public void delete(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow( () -> new ResourceNotFoundException("Vehicle not found") );
        vehicleRepository.delete(vehicle);
        log.info("Vehicle deleted successfully");
    }

}
