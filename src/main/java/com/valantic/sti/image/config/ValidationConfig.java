package com.valantic.sti.image.config;

import com.valantic.sti.image.ImageProperties;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValidationConfig {
    
    @Bean
    public Validator validator(ImageProperties imageProperties) {
        return Validation.byDefaultProvider()
            .configure()
            .constraintValidatorFactory(new SpringConstraintValidatorFactory(imageProperties))
            .buildValidatorFactory()
            .getValidator();
    }
}