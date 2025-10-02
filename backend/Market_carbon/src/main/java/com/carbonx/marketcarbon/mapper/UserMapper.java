package com.carbonx.marketcarbon.mapper;


import com.carbonx.marketcarbon.dto.request.UserCreationRequest;
import com.carbonx.marketcarbon.dto.response.UserResponse;
import com.carbonx.marketcarbon.model.User;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(UserCreationRequest request);
    UserResponse toUserResponse(User user);
}
