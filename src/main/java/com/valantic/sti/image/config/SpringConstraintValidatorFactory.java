package com.valantic.sti.image.config;

import com.valantic.sti.image.ImageProperties;
import com.valantic.sti.image.validation.ImageFileValidator;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;

public class SpringConstraintValidatorFactory implements ConstraintValidatorFactory {
    
    private final ImageProperties imageProperties;

    public SpringConstraintValidatorFactory(ImageProperties imageProperties) {
        this.imageProperties = imageProperties;
    }

    @Override
    public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
        if (key == ImageFileValidator.class) {
            return key.cast(new ImageFileValidator(imageProperties));
        }
        try {
            return key.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create validator instance", e);
        }
    }

    @Override
    public void releaseInstance(ConstraintValidator<?, ?> instance) {
        // No cleanup needed
    }
}