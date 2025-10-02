package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.model.Vehicle;
import com.carbonx.marketcarbon.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

//@SpringBootTest // test toàn bộ api
@DataJpaTest // test riêng lẻ thằng có CRUD
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)

public class VehicleTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Test
    public void createVehicleTest(){
        // B1 Create
        Vehicle vehicle = new Vehicle();
        vehicle.setId(122L);
        vehicle.setPlateNumber("123");
        vehicle.setBrand("Vinfast");
        vehicle.setModel("VF3");
        vehicle.setPlateNumber("vietnam-70000");
        vehicle.setYearOfManufacture(2025);

        vehicle = vehicleRepository.save(vehicle);
        Long vehicleId = vehicle.getId();
    }


    @Test
    public void testReadVehicle() {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(123L);
        vehicle.setPlateNumber("vietnam-70001");
        vehicle.setBrand("Toyota");
        vehicle.setModel("Corolla");
        vehicle.setYearOfManufacture(2020);

        Vehicle saved = vehicleRepository.save(vehicle);
        Vehicle found = vehicleRepository.findById(saved.getId()).orElse(null);
        assertNotNull(found);
        assertEquals("Toyota", found.getBrand());
    }

    @Test
    public void testGetAllVehicles() {
        Vehicle vehicle1 = new Vehicle();
        vehicle1.setId(201L);
        vehicle1.setPlateNumber("vietnam-80001");
        vehicle1.setBrand("Hyundai");
        vehicle1.setModel("Accent");
        vehicle1.setYearOfManufacture(2021);

        Vehicle vehicle2 = new Vehicle();
        vehicle2.setId(202L);
        vehicle2.setPlateNumber("vietnam-80002");
        vehicle2.setBrand("Kia");
        vehicle2.setModel("Morning");
        vehicle2.setYearOfManufacture(2022);

        vehicleRepository.save(vehicle1);
        vehicleRepository.save(vehicle2);

        List<Vehicle> vehicles = vehicleRepository.findAll();
        assertTrue(vehicles.size() >= 2);
        for (Vehicle v : vehicles) {
            System.out.println("Vehicle ID: " + v.getId()
                + ", Owner ID: " + v.getId()
                + ", Plate Number: " + v.getPlateNumber()
                + ", Brand: " + v.getBrand()
                + ", Model: " + v.getModel()
                + ", Year: " + v.getYearOfManufacture());
        }
    }


    @Test
    public void testUpdateVehicle() {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(124L);
        vehicle.setPlateNumber("vietnam-70002");
        vehicle.setBrand("Honda");
        vehicle.setModel("Civic");
        vehicle.setYearOfManufacture(2019);

        Vehicle saved = vehicleRepository.save(vehicle);
        saved.setBrand("Mazda");
        Vehicle updated = vehicleRepository.save(saved);
        assertEquals("Mazda", updated.getBrand());
    }

    @Test
    public void testDeleteVehicle() {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(125L);
        vehicle.setPlateNumber("vietnam-70003");
        vehicle.setBrand("Ford");
        vehicle.setModel("Focus");
        vehicle.setYearOfManufacture(2018);

        Vehicle saved = vehicleRepository.save(vehicle);
        Long id = saved.getId();
        vehicleRepository.deleteById(id);
        assertFalse(vehicleRepository.findById(id).isPresent());
    }

}
