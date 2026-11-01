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

/** Class-level check that "confirm password" equals "password"; the error is reported on confirmPassword. */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordsMatch.Validator.class)
public @interface PasswordsMatch {
    String message() default "Passwords do not match";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** Compares the two values and attaches the message to the confirmPassword field. */
    class Validator implements ConstraintValidator<PasswordsMatch, PasswordPair> {
        @Override
        public boolean isValid(PasswordPair value, ConstraintValidatorContext context) {
            if (value == null || value.password() == null || value.password().equals(value.confirmPassword())) {
                return true;
            }
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("confirmPassword").addConstraintViolation();
            return false;
        }
    }
}
