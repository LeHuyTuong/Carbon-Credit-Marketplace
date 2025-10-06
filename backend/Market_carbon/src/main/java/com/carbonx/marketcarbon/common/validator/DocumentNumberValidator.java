package com.carbonx.marketcarbon.common.validator;

import com.carbonx.marketcarbon.common.annotation.DocumentNumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class DocumentNumberValidator implements ConstraintValidator<DocumentNumber, String> {
    // CMND 9 số (không cho phép 9 số 0)
    private static final Pattern CMND = Pattern.compile("^(?!0{9})\\d{9}$");

    // CCCD 12 số, bán-chặt:
    // - 3 số đầu != 000
    // - số thứ 4 thuộc [0-3]
    // - 2 số tiếp theo là YY
    // - 6 số cuối không phải 000000
    private static final Pattern CCCD = Pattern.compile("^(?!000)\\d{3}[0-3]\\d{2}(?!000000)\\d{6}$");


    @Override
    public void initialize(DocumentNumber constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        // Để @NotBlank/@NotNull xử lý bắt buộc nhập; validator này chỉ check định dạng
        if (value == null || value.isBlank()) return true;

        // Chuẩn hoá: bỏ mọi ký tự không phải số (vd: "0790 12 123456")
        String digits = value.replaceAll("\\D", "");

        if (digits.length() == 12) {
            return CCCD.matcher(digits).matches();
        }
        if ( digits.length() == 9) {
            return CMND.matcher(digits).matches();
        }
        return false;
    }
}
