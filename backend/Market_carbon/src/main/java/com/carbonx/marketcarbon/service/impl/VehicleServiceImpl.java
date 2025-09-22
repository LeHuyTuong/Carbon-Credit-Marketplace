package com.carbonx.marketcarbon.service.impl;



import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.Vehicle;
import com.carbonx.marketcarbon.repository.VehicleRepository;
import com.carbonx.marketcarbon.request.VehicleCreateRequest;
import com.carbonx.marketcarbon.response.VehicleResponse;
import com.carbonx.marketcarbon.service.VehicleService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
public class VehicleServiceImpl implements VehicleService {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Override
    public VehicleResponse create(VehicleCreateRequest req) {
        // B1 add data form request
        Vehicle vehicle = Vehicle.builder()
                .plateNumber(req.getPlateNumber())
                .brand(req.getBrand())
                .model(req.getModel())
                .build();
        // B2 save data on repo
        vehicleRepository.save(vehicle);
        //B3 add data to response
        VehicleResponse vehicleResponse = VehicleResponse
                .builder()
                .id(vehicle.getId())
                .ownerId(vehicle.getOwnerId())
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
                        .ownerId(vehicle.getOwnerId())
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
                .ownerId(vehicle.getOwnerId())
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
                .ownerId(vehicle.getOwnerId())
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
