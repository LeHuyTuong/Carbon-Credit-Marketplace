package com.carbonx.marketcarbon.dto.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KycCvaRequest {
    @NotBlank String name;
    @Email String email;
    String organization;
    String positionTitle;
    MultipartFile avatar;
}
