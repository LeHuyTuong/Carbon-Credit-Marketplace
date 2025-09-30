package com.carbonx.marketcarbon.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Admin_UserResponse {
    private Long id;
    private String name;
    private String email;
    private String gender;
    private boolean enabled; // Thêm thuộc tính này
    private String role; // Kiểu String
    private LocalDateTime createAt; // Đảm bảo kiểu dữ liệu là LocalDateTime
}

