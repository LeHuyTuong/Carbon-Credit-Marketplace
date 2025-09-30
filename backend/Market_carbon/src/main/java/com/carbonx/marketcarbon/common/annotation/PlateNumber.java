package com.carbonx.marketcarbon.common.annotation;

import com.carbonx.marketcarbon.common.validator.PlateNumberValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = {PlateNumberValidator.class})
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PlateNumber {
    String message() default "Invalid Plate Number";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
