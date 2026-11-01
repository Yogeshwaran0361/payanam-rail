package com.payanam.rail.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.time.Period;

/** The date of birth must make the person at least 18 years old today (and not be in the future). */
@Documented
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = Adult.Validator.class)
public @interface Adult {
    String message() default "You must be at least 18 years old to register";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** Period.between gives whole years, so a birthday tomorrow still counts as 17. */
    class Validator implements ConstraintValidator<Adult, LocalDate> {
        @Override
        public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
            if (value == null) {
                return true;                 // @NotNull reports a missing value
            }
            LocalDate today = LocalDate.now();
            return !value.isAfter(today) && Period.between(value, today).getYears() >= 18
                    && Period.between(value, today).getYears() <= 120;
        }
    }
}
