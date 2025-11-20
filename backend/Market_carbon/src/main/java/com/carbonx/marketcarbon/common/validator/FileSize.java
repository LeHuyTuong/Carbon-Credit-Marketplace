package com.carbonx.marketcarbon.common.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = FileSizeValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FileSize {
    String message() default "File size exceeds the maximum limit";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    boolean required() default true;

    long max() default 5242880;
}