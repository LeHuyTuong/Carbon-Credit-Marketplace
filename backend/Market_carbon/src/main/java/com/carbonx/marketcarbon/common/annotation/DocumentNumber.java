package com.carbonx.marketcarbon.common.annotation;


import com.carbonx.marketcarbon.common.validator.PhoneValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PhoneValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME) // chạy khi run time
public @interface DocumentNumber {
    String message() default "Invalid Document Number";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
