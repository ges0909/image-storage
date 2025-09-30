package com.valantic.sti.image.validation;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class FileSignatureValidatorTest {

    private static final byte[] JPEG_HEADER = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46};
    private static final byte[] PNG_HEADER = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] WEBP_HEADER = {0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50};

    @Test
    void isValidImageSignature_ShouldReturnTrue_ForValidJpeg() {
        // Arrange
        ByteArrayInputStream inputStream = new ByteArrayInputStream(JPEG_HEADER);

        // Act
        boolean result = FileSignatureValidator.isValidImageSignature(inputStream, "image/jpeg");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isValidImageSignature_ShouldReturnTrue_ForValidPng() {
        // Arrange
        ByteArrayInputStream inputStream = new ByteArrayInputStream(PNG_HEADER);

        // Act
        boolean result = FileSignatureValidator.isValidImageSignature(inputStream, "image/png");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isValidImageSignature_ShouldReturnTrue_ForValidWebp() {
        // Arrange
        ByteArrayInputStream inputStream = new ByteArrayInputStream(WEBP_HEADER);

        // Act
        boolean result = FileSignatureValidator.isValidImageSignature(inputStream, "image/webp");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isValidImageSignature_ShouldReturnFalse_ForInvalidSignature() {
        // Arrange
        byte[] invalidHeader = {0x00, 0x01, 0x02, 0x03};
        ByteArrayInputStream inputStream = new ByteArrayInputStream(invalidHeader);

        // Act
        boolean result = FileSignatureValidator.isValidImageSignature(inputStream, "image/jpeg");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isValidImageSignature_ShouldReturnFalse_ForUnsupportedContentType() {
        // Arrange
        ByteArrayInputStream inputStream = new ByteArrayInputStream(JPEG_HEADER);

        // Act
        boolean result = FileSignatureValidator.isValidImageSignature(inputStream, "image/gif");

        // Assert
        assertThat(result).isFalse();
    }
}