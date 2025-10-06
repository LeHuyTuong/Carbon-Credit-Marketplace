//package com.carbonx.marketcarbon.mapper;
//
//import com.carbonx.marketcarbon.dto.response.admin.Admin_UserResponse;
//import com.carbonx.marketcarbon.model.User;
//import org.mapstruct.Mapper;
//import org.mapstruct.Mapping;
//
//@Mapper(componentModel = "spring")
//public interface Admin_UserMapper {
//
//    // ánh xạ từ entity User sang Admin_UserResponse
//    @Mapping(target = "roles", expression = "java(user.getRoles().stream().map(role -> role.getName()).toList())")
//    @Mapping(target = "companyName", expression = "java(user.getCompany() != null ? user.getCompany().getName() : null)")
//    @Mapping(target = "status", expression = "java(user.getStatus().name())")
//    Admin_UserResponse toUserResponse(User user);
//}