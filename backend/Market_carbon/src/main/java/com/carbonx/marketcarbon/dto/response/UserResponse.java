package com.carbonx.marketcarbon.dto.response;

import com.carbonx.marketcarbon.model.Role;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {

    String email;
    String firstName;
    String lastName;
    LocalDate dob;
    Boolean noPassword;
    Set<Role> roles = new HashSet<>();

}
