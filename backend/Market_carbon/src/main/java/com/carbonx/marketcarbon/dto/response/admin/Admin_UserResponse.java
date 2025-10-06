package com.carbonx.marketcarbon.dto.response.admin;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Admin_UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String status;
    private String companyName;
    private List<String> roles;
}