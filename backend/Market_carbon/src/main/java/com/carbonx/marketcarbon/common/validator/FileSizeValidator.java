package com.carbonx.marketcarbon.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

public class FileSizeValidator implements ConstraintValidator<FileSize, MultipartFile> {

    private long maxSize;
    private boolean required;

    @Override
    public void initialize(FileSize constraintAnnotation) {
        this.maxSize = constraintAnnotation.max();
        this.required = constraintAnnotation.required();
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {

        // OPTIONAL FILE (required = false)
        if (!required) {
            // Không gửi file → hợp lệ
            if (file == null || file.isEmpty()) {
                return true;
            }
            // Có file → kiểm tra size
            return file.getSize() <= maxSize;
        }

        // REQUIRED FILE (required = true)
        if (file == null || file.isEmpty()) {
            return false; // file bắt buộc
        }

        // Kiểm tra size khi required=true và có file
        return file.getSize() <= maxSize;
    }
}
