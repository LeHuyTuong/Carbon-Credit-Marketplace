package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.common.Gender;
import com.carbonx.marketcarbon.common.IDType;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.EVOwner;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Vehicle;
import com.carbonx.marketcarbon.repository.CompanyRepository;
import com.carbonx.marketcarbon.repository.EVOwnerRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class VehicleTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EVOwnerRepository evOwnerRepository;

    private User user;
    private Company company;
    private EVOwner evOwner;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("test@test.com");
        user.setPasswordHash("password");
        user = userRepository.saveAndFlush(user);

        company = new Company();
        company.setCompanyName("Test Company");
        company.setBusinessLicense("12345");
        company.setUser(user);
        company = companyRepository.saveAndFlush(company);

        evOwner = new EVOwner();
        evOwner.setUser(user);
        evOwner.setAddress("Test Address");
        evOwner.setBirthDate(LocalDate.now());
        evOwner.setCountry("Test Country");
        evOwner.setDocumentType(IDType.CCCD);
        evOwner.setEmail("test@test.com");
        evOwner.setGender(Gender.MALE);
        evOwner.setName("Test Name");
        evOwner.setPhone("1234567890");
        // evOwner.setCompany(company);
        evOwner = evOwnerRepository.saveAndFlush(evOwner);
    }

    @Test
    public void createVehicleTest(){
        Vehicle vehicle = new Vehicle();
        vehicle.setPlateNumber("123");
        vehicle.setBrand("Vinfast");
        vehicle.setModel("VF3");
        // vehicle.setCompany(company);
        vehicle.setEvOwner(evOwner);

        vehicle = vehicleRepository.save(vehicle);
        Long vehicleId = vehicle.getId();
    }

/*
    @Test
    public void testReadVehicle() {
        Vehicle vehicle = new Vehicle();
        vehicle.setPlateNumber("vietnam-70001");
        vehicle.setBrand("Toyota");
        vehicle.setModel("Corolla");
        vehicle.setCompany(company);
        vehicle.setEvOwner(evOwner);

        Vehicle saved = vehicleRepository.save(vehicle);
        Vehicle found = vehicleRepository.findById(saved.getId()).orElse(null);
        assertNotNull(found);
        assertEquals("Toyota", found.getBrand());
    }

    @Test
    public void testGetAllVehicles() {
        Vehicle vehicle1 = new Vehicle();
        vehicle1.setPlateNumber("vietnam-80001");
        vehicle1.setBrand("Hyundai");
        vehicle1.setModel("Accent");
        vehicle1.setCompany(company);
        vehicle1.setEvOwner(evOwner);

        Vehicle vehicle2 = new Vehicle();
        vehicle2.setPlateNumber("vietnam-80002");
        vehicle2.setBrand("Kia");
        vehicle2.setModel("Morning");
        vehicle2.setCompany(company);
        vehicle2.setEvOwner(evOwner);

        vehicleRepository.save(vehicle1);
        vehicleRepository.save(vehicle2);

        List<Vehicle> vehicles = vehicleRepository.findAll();
        assertTrue(vehicles.size() >= 2);
        for (Vehicle v : vehicles) {
            System.out.println("Vehicle ID: " + v.getId()
                + ", Owner ID: " + v.getEvOwner().getId()
                + ", Plate Number: " + v.getPlateNumber()
                + ", Brand: " + v.getBrand()
                + ", Model: " + v.getModel());
        }
    }


    @Test
    public void testUpdateVehicle() {
        Vehicle vehicle = new Vehicle();
        vehicle.setPlateNumber("vietnam-70002");
        vehicle.setBrand("Honda");
        vehicle.setModel("Civic");
        vehicle.setCompany(company);
        vehicle.setEvOwner(evOwner);

        Vehicle saved = vehicleRepository.save(vehicle);
        saved.setBrand("Mazda");
        Vehicle updated = vehicleRepository.save(saved);
        assertEquals("Mazda", updated.getBrand());
    }

    @Test
    public void testDeleteVehicle() {
        Vehicle vehicle = new Vehicle();
        vehicle.setPlateNumber("vietnam-70003");
        vehicle.setBrand("Ford");
        vehicle.setModel("Focus");
        vehicle.setCompany(company);
        vehicle.setEvOwner(evOwner);

        Vehicle saved = vehicleRepository.save(vehicle);
        Long id = saved.getId();
        vehicleRepository.deleteById(id);
        assertFalse(vehicleRepository.findById(id).isPresent());
    }
*/
}