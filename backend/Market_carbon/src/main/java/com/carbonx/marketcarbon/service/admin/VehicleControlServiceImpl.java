package com.carbonx.marketcarbon.service.admin;

import com.carbonx.marketcarbon.dto.response.PageResponse;
import com.carbonx.marketcarbon.dto.response.VehicleDetailResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.model.Vehicle;
import com.carbonx.marketcarbon.repository.CompanyRepository;
import com.carbonx.marketcarbon.repository.SearchRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.repository.VehicleRepository;
import com.carbonx.marketcarbon.service.VehicleControlService;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class VehicleControlServiceImpl implements VehicleControlService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final SearchRepository searchRepository;
    private final CompanyRepository companyRepository;

    @Override
    public PageResponse<?> getAllVehiclesWithSortBy(int pageNo, int pageSize, String sorts) {
        //B1 tao pagealbe
        if(pageNo > 0){
            pageNo = pageNo - 1;
        }

        List<Sort.Order> orders = new ArrayList<>(); // Sort.Order đại diện cho một cột + hướng sắp xếp (ASC hoặc DESC)

        //B2 Nếu có giá trị thì sẽ sortBy xử lý
        if(StringUtils.hasLength(sorts)){
            //PlateNumber asc/desc
            Pattern pattern = Pattern.compile("(\\w+?)(:)(.*)"); // có 3 cái sort , platenumber , brand , model
            Matcher matcher = pattern.matcher(sorts);

            if(matcher.find()){
                // xử lý sort
                if(matcher.group(3).equalsIgnoreCase("asc")){ // truyen vao group 3 la asc hoac desc
                    orders.add(new Sort.Order(Sort.Direction.ASC, matcher.group(1)));
                }else{
                    orders.add(new Sort.Order(Sort.Direction.DESC, matcher.group(1)));
                }
            }
        }
        // B3 Trả về page
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(orders)); // truyền field, platenumber , brand , modeltrong Entity
        Page<Vehicle> vehiclePage = vehicleRepository.findAll(pageable); // find theo pageable

        //B4 bóc tách vehicle để truyền về list

        List<VehicleDetailResponse> responses = vehiclePage.stream()
                .map( vehicle -> VehicleDetailResponse.builder()
                        .updatedAt(vehicle.getUpdatedAt())
                        .createdAt(vehicle.getCreateAt())
                        .plateNumber(vehicle.getPlateNumber())
                        .id(vehicle.getId())
                        .brand(vehicle.getBrand())
                        .model(vehicle.getModel())
                        .companyId(vehicle.getCompany().getId())
                        .build())
                .toList(); // toList return về 1 list của VehicleDetailResponse

        return PageResponse.builder()
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalPages(vehiclePage.getTotalPages())
                .items(responses)
                .build();

    }

    @Override
    public PageResponse<?> getAllVehiclesWithSortByMultipleColumns(int pageNo, int pageSize, String... sorts) {
        //B1 Check pageNo
        if(pageNo > 0){
            pageNo = pageNo - 1;
        }

        if(sorts == null){
            sorts = new String[0]; // để for-each & Sort.by(...) không bị NPE
        }
        //B2 process logic sort
        List<Sort.Order> orders = new ArrayList<>();
        for(String sortBy : sorts){
            Pattern pattern = Pattern.compile("(\\w+?)(:)(.*)"); // có 3 groups
            Matcher matcher = pattern.matcher(sortBy);
            if(matcher.find()){
                if(matcher.group(3).equalsIgnoreCase("asc")){ // truyen vao group 3 la asc hoac desc
                    orders.add(new Sort.Order(Sort.Direction.ASC, matcher.group(1)));
                }else{
                    orders.add(new Sort.Order(Sort.Direction.DESC, matcher.group(1)));
                }
            }
        }

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(orders));
        //B3 Tra ve Page
        Page<Vehicle> vehiclePage = vehicleRepository.findAll(pageable);

        List<VehicleDetailResponse> responses = vehiclePage.stream().
                map(vehicle -> VehicleDetailResponse.builder()
                        .createdAt(vehicle.getCreateAt())
                        .updatedAt(vehicle.getUpdatedAt())
                        .plateNumber(vehicle.getPlateNumber())
                        .id(vehicle.getId())
                        .brand(vehicle.getBrand())
                        .model(vehicle.getModel())
                        .companyId(vehicle.getCompany().getId())
                        .build())
                .toList();

        return PageResponse.builder()
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalPages(vehiclePage.getTotalPages())
                .items(responses)
                .build();
    }

    @Override
    public PageResponse<?> getAllVehiclesWithSortByMultipleColumnsAndSearch(int pageNo, int pageSize, String search, String sortsBy) {
        return searchRepository.getAllVehiclesWithSortByMultipleColumnsAndSearch(pageNo, pageSize, search, sortsBy);
    }

    @Override
    public PageResponse<?> getAllVehiclesOfCompanyWithSortBy(int pageNo, int pageSize, String sorts) {
        // B1 tạo pageNo
        if(pageNo > 0){
            pageNo = pageNo - 1;
        }

        //B2 process logic sort
        List<Sort.Order> orders = new ArrayList<>();
        if(StringUtils.hasLength(sorts)){
            Pattern pattern = Pattern.compile("(\\w+?)(:)(.*)");
            Matcher matcher = pattern.matcher(sorts);
            if(matcher.find()){
                if(matcher.group(3).equalsIgnoreCase("asc")){ // truyen vao group 3 la asc hoac desc
                    orders.add(new Sort.Order(Sort.Direction.ASC, matcher.group(1)));
                }else{
                    orders.add(new Sort.Order(Sort.Direction.DESC, matcher.group(1)));
                }
            }
        }

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(orders));

        // B3 get CompanyId from user login
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User)authentication.getPrincipal();
        Long companyId = user.getId();
        Page<Vehicle> vehiclePage = vehicleRepository.findByCompany_Id(companyId, pageable);

        List<VehicleDetailResponse> responses = vehiclePage.stream()
                .map(vehicle -> new VehicleDetailResponse(
                        vehicle.getId(),
                        vehicle.getPlateNumber(),
                        vehicle.getBrand(),
                        vehicle.getModel(),
                        vehicle.getCreateAt(),
                        vehicle.getUpdatedAt(),
                        vehicle.getCompany().getId()
                )).toList();

        return PageResponse.builder()
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalPages(vehiclePage.getTotalPages())
                .items(responses)
                .build();
    }


    // Cho aggrerator trả về thời gian tạo và thời gian update
    @Override
    public List<VehicleDetailResponse> getAll() {
        return vehicleRepository.findAll().stream()
                .map(vehicle -> new VehicleDetailResponse(
                        vehicle.getId(),
                        vehicle.getPlateNumber(),
                        vehicle.getBrand(),
                        vehicle.getModel(),
                        vehicle.getCreateAt(),
                        vehicle.getUpdatedAt(),
                        vehicle.getCompany().getId()
                )).toList(); // return về 1 list của Vehicle
    }
}
