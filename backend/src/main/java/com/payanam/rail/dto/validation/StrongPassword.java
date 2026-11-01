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

/** Password policy: at least 8 characters with an upper-case letter, a lower-case letter, a digit and a special character. */
@Documented
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StrongPassword.Validator.class)
public @interface StrongPassword {
    String message() default "Password needs 8+ characters with upper and lower case letters, a digit and a special character";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** Checks the policy character by character (no regex, easy to explain). */
    class Validator implements ConstraintValidator<StrongPassword, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return isStrong(value);
        }

        public static boolean isStrong(String value) {
            if (value == null || value.length() < 8 || value.length() > 64) {
                return false;
            }
            boolean upper = false, lower = false, digit = false, special = false;
            for (char c : value.toCharArray()) {
                if (Character.isUpperCase(c)) {
                    upper = true;
                } else if (Character.isLowerCase(c)) {
                    lower = true;
                } else if (Character.isDigit(c)) {
                    digit = true;
                } else if (!Character.isWhitespace(c)) {
                    special = true;
                }
            }
            return upper && lower && digit && special;
        }
    }
}
