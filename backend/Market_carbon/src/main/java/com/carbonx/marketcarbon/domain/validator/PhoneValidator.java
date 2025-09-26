package com.carbonx.marketcarbon.domain.validator;

import com.carbonx.marketcarbon.domain.annotation.PhoneNumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneValidator implements ConstraintValidator<PhoneNumber, String> {

    @Override
    public void initialize(PhoneNumber constraintAnnotation) {
    }

    @Override
    public boolean isValid(String phoneNo, ConstraintValidatorContext constraintValidatorContext) {
        if(phoneNo==null || phoneNo.isEmpty())
        {
            return false;
        }
        //validate sdt với format "0902345345"
        if (phoneNo.matches("\\d{10}")) return true;
            //validating sdt có kí tự lạ  -, . hoặc spaces: 090-234-4567
        else if(phoneNo.matches("\\d{3}[-\\.\\s]\\d{3}[-\\.\\s]\\d{4}")) return true;
            //validating sđt with có x phía sau 090-234-4567 x123
        else return false;
    }
}
