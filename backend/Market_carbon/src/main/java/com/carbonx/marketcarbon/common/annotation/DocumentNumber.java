package com.carbonx.marketcarbon.common.annotation;


import com.carbonx.marketcarbon.common.validator.DocumentNumberValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = DocumentNumberValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME) // chạy khi run time
public @interface DocumentNumber {
    String message() default "Document Number CCCD need 12 number and CMND need 9 number ";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
