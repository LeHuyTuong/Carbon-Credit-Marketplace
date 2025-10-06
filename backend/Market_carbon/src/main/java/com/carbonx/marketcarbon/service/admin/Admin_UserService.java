//package com.carbonx.marketcarbon.service.admin;
//
//import com.carbonx.marketcarbon.dto.response.admin.Admin_UserResponse;
//import com.carbonx.marketcarbon.mapper.Admin_UserMapper;
//
//import com.carbonx.marketcarbon.repository.RoleRepository;
//import com.carbonx.marketcarbon.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class Admin_UserService {
//
//    private final UserRepository userRepository;
//    private final RoleRepository roleRepository;
//    private final Admin_UserMapper userMapper;
//
//
//    public Page<Admin_UserResponse> getAllUsers(Pageable pageable) {
//        return userRepository.findAll(pageable).map(userMapper::toUserResponse);
//    }
//
//}
