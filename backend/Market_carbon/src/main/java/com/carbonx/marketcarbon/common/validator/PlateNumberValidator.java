package com.carbonx.marketcarbon.common.validator;

import com.carbonx.marketcarbon.common.annotation.PlateNumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:validation.yml")
public class PlateNumberValidator implements ConstraintValidator<PlateNumber, String> {

    @Value("${plate.regex")
    private String plateRegex;

    @Override
    public void initialize(PlateNumber constraintAnnotation) {}

    @Override
    public boolean isValid(String plateNumber, ConstraintValidatorContext context) {
        try {
            if (plateNumber == null || plateNumber.isEmpty()) {
                return false;
            }
            if(plateNumber.length() == 9) return true;
            else if(plateNumber.matches(plateRegex)) return true;
            else return false;
        }catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
