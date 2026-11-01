package com.payanam.rail.dto.validation;

/** A form with a password and its confirmation (checked by {@link PasswordsMatch}). */
public interface PasswordPair {
    String password();

    String confirmPassword();
}
