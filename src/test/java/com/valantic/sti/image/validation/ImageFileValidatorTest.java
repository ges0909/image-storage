package com.valantic.sti.image.validation;

import com.valantic.sti.image.ImageProperties;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageFileValidatorTest {

    @Mock
    private ConstraintValidatorContext context;
    
    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    private ImageFileValidator validator;
    private ImageProperties imageProperties;

    @BeforeEach
    void setUp() {
        imageProperties = new ImageProperties(
            "bucket", "thumbnails", "kms-key", "cdn.com",
            1024 * 1024L, "eu-central-1", 15, new int[]{150, 300},
            Set.of("image/jpeg", "image/png"), 1000, "images"
        );
        validator = new ImageFileValidator(imageProperties);
        validator.initialize(null);
    }

    @Test
    void isValid_ShouldReturnTrue_WhenValidImageProvided() {
        // Create a file with valid JPEG signature
        byte[] jpegSignature = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46};
        MockMultipartFile file = new MockMultipartFile(
            "image", "test.jpg", "image/jpeg", jpegSignature
        );

        boolean result = validator.isValid(file, context);

        assertThat(result).isTrue();
    }

    @Test
    void isValid_ShouldReturnFalse_WhenFileIsEmpty() {
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[0]);

        boolean result = validator.isValid(file, context);

        assertThat(result).isFalse();
    }

    @Test
    void isValid_ShouldReturnFalse_WhenFileTooLarge() {
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        byte[] largeContent = new byte[2 * 1024 * 1024]; // 2MB > 1MB limit
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", largeContent);

        boolean result = validator.isValid(file, context);

        assertThat(result).isFalse();
    }

    @Test
    void isValid_ShouldReturnFalse_WhenInvalidContentType() {
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        MockMultipartFile file = new MockMultipartFile(
            "image", "test.txt", "text/plain", "not-an-image".getBytes()
        );

        boolean result = validator.isValid(file, context);

        assertThat(result).isFalse();
    }

    @Test
    void initialize_ShouldUseDefaults_WhenImagePropertiesIsNull() {
        ImageFileValidator validatorWithoutProps = new ImageFileValidator();
        validatorWithoutProps.initialize(null);

        // Create a file with valid JPEG signature
        byte[] jpegSignature = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46};
        MockMultipartFile file = new MockMultipartFile(
            "image", "test.jpg", "image/jpeg", jpegSignature
        );

        boolean result = validatorWithoutProps.isValid(file, context);

        assertThat(result).isTrue();
    }
}