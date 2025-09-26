package com.valantic.sti.image.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class UUIDValidatorTest {

    private UUIDValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new UUIDValidator();
        context = mock(ConstraintValidatorContext.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "550e8400-e29b-41d4-a716-446655440000",
        "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
        "123e4567-e89b-12d3-a456-426614174000"
    })
    void isValid_ShouldReturnTrue_WhenValidUUIDProvided(String validUUID) {
        boolean result = validator.isValid(validUUID, context);
        
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "invalid-uuid",
        "550e8400-e29b-41d4-a716",
        "550e8400-e29b-41d4-a716-446655440000-extra",
        "550e8400xe29bx41d4xa716x446655440000",
        "",
        "550e8400-e29b-41d4-a716-44665544000g"
    })
    void isValid_ShouldReturnFalse_WhenInvalidUUIDProvided(String invalidUUID) {
        boolean result = validator.isValid(invalidUUID, context);
        
        assertThat(result).isFalse();
    }

    @Test
    void isValid_ShouldReturnFalse_WhenNullProvided() {
        boolean result = validator.isValid(null, context);
        
        assertThat(result).isFalse();
    }
}