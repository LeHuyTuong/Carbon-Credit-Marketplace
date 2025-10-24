package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.dto.request.VehicleCreateRequest;
import com.carbonx.marketcarbon.dto.request.VehicleUpdateRequest;
import com.carbonx.marketcarbon.dto.response.VehicleResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.EVOwner;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Vehicle;
import com.carbonx.marketcarbon.repository.CompanyRepository;
import com.carbonx.marketcarbon.repository.EVOwnerRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.VehicleRepository;
import com.carbonx.marketcarbon.service.impl.VehicleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleTest {

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private EVOwnerRepository evOwnerRepository;

    @InjectMocks
    private VehicleServiceImpl vehicleService;

    private Authentication authentication;
    private SecurityContext securityContext;

    @BeforeEach
    void setupSecurity() {
        authentication = mock(Authentication.class);
        securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn("owner@example.com");
    }

    @Test
    void create_ShouldPersistVehicle_WhenValidAndUniquePlate() {
        // arrange
        EVOwner owner = new EVOwner();
        owner.setId(1L);
        Company company = new Company();
        company.setId(10L);

        VehicleCreateRequest req = new VehicleCreateRequest();
        req.setPlateNumber("ABC-123");
        req.setBrand("Vinfast");
        req.setModel("VF3");
        req.setCompanyId(10L);

        when(evOwnerRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(vehicleRepository.existsByPlateNumber("ABC-123")).thenReturn(false);
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> {
            Vehicle v = invocation.getArgument(0);
            v.setId(100L);
            return v;
        });

        // act
        VehicleResponse res = vehicleService.create(req);

        // assert
        assertThat(res.getId()).isEqualTo(100L);
        assertThat(res.getPlateNumber()).isEqualTo("ABC-123");
        assertThat(res.getCompanyId()).isEqualTo(10L);
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    void create_ShouldThrow_WhenPlateExists() {
        EVOwner owner = new EVOwner();
        when(evOwnerRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(vehicleRepository.existsByPlateNumber("DUP-1")).thenReturn(true);

        VehicleCreateRequest req = new VehicleCreateRequest();
        req.setPlateNumber("DUP-1");
        req.setCompanyId(1L);

        assertThatThrownBy(() -> vehicleService.create(req))
                .isInstanceOf(AppException.class);
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void getOwnerVehicles_ShouldMapEntitiesToResponses() {
        User user = new User();
        user.setId(99L);
        when(userRepository.findByEmail("owner@example.com")).thenReturn(user);

        Vehicle v1 = new Vehicle(); v1.setId(1L); v1.setPlateNumber("P1");
        Vehicle v2 = new Vehicle(); v2.setId(2L); v2.setPlateNumber("P2");
        when(vehicleRepository.findByEvOwner_Id(99L)).thenReturn(List.of(v1, v2));

        List<VehicleResponse> list = vehicleService.getOwnerVehicles();
        assertThat(list).hasSize(2);
        assertThat(list.get(0).getId()).isEqualTo(1L);
        assertThat(list.get(1).getPlateNumber()).isEqualTo("P2");
    }

    @Test
    void update_ShouldApplyChanges_WhenValid() {
        EVOwner owner = new EVOwner(); owner.setId(7L);
        when(evOwnerRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));

        Company company = new Company(); company.setId(55L);
        when(companyRepository.findById(55L)).thenReturn(Optional.of(company));

        Vehicle existing = new Vehicle(); existing.setId(100L); existing.setPlateNumber("OLD");
        when(vehicleRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(vehicleRepository.existsByPlateNumber("NEW"))
                .thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(i -> i.getArgument(0));

        VehicleUpdateRequest req = new VehicleUpdateRequest();
        req.setPlateNumber("NEW");
        req.setBrand("B");
        req.setModel("M");
        req.setCompanyId(55L);

        VehicleResponse res = vehicleService.update(100L, req);

        assertThat(res.getPlateNumber()).isEqualTo("NEW");
        assertThat(res.getCompanyId()).isEqualTo(55L);
        verify(vehicleRepository).save(existing);
    }

    @Test
    void delete_ShouldRemove_WhenExists() {
        Vehicle existing = new Vehicle(); existing.setId(9L);
        when(vehicleRepository.findById(9L)).thenReturn(Optional.of(existing));

        vehicleService.delete(9L);

        verify(vehicleRepository).delete(existing);
    }

    @Test
    void getOwnerVehicles_ShouldThrow_WhenUserMissing() {
        when(userRepository.findByEmail("owner@example.com")).thenReturn(null);
        assertThatThrownBy(() -> vehicleService.getOwnerVehicles())
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
