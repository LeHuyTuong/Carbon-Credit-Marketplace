package com.carbonx.marketcarbon.common.validator;

import lombok.extern.slf4j.Slf4j;
import com.carbonx.marketcarbon.common.annotation.PlateNumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PlateNumberValidator implements ConstraintValidator<PlateNumber, String> {

    private static final String PLATE_REGEX =  "^(" +
            "\\d{2}[A-Z]-\\d{3}\\.\\d{2}" +       // e.g., 51A-123.45
            "|" +
            "\\d{2}[A-Z]-\\d{4,5}" +             // e.g., 30A-1234 or 50H-22288 (IMPROVED)
            "|" +
            "\\d{2}[A-Z]{2}-\\d{3}\\.\\d{2}" +    // e.g., 51LD-123.45
            "|" +
            "\\d{2}-[A-Z]\\d-\\d{5}" +           // e.g., 29-S1-12345
            "|" +
            "\\d{2}-[A-Z]{2}-\\d{5}" +           // e.g., 29-AA-12345
            ")$";

    @Override
    public void initialize(PlateNumber constraintAnnotation) {}

    @Override
    public boolean isValid(String plateNumber, ConstraintValidatorContext context) {
        try {
            if (plateNumber == null || plateNumber.isEmpty()) {
                return false;
            }
            if(plateNumber.length() == 9) return true;
            else if(plateNumber.matches(PLATE_REGEX)) return true;
            else return false;
        }catch (Exception e) {
            log.warn("Plate number validation failed for input '{}'", plateNumber, e);
            return false;
        }
    }
}
