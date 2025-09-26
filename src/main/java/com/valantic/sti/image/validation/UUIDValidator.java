package com.valantic.sti.image.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class UUIDValidator implements ConstraintValidator<ValidUUID, String> {

    private static final Pattern UUID_PATTERN = Pattern.compile("^[a-fA-F0-9-]{36}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && UUID_PATTERN.matcher(value).matches();
    }
}