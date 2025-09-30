package com.carbonx.marketcarbon.mapper;


import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.request.UserCreationRequest;
import com.carbonx.marketcarbon.response.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(UserCreationRequest request);
    UserResponse toUserResponse(User user);

}
