package com.valantic.sti.image.validation;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class FileSignatureValidatorTest {

    private static final byte[] JPEG_HEADER = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46};
    private static final byte[] PNG_HEADER = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] GIF_HEADER = {0x47, 0x49, 0x46, 0x38, 0x39, 0x61};
    private static final byte[] TIFF_HEADER_LE = {0x49, 0x49, 0x2A, 0x00};
    private static final byte[] BMP_HEADER = {0x42, 0x4D, 0x36, 0x00, 0x00, 0x00};
    private static final byte[] HIF_HEADER = {0x00, 0x00, 0x00, 0x20, 0x66, 0x74, 0x79, 0x70, 0x68, 0x65, 0x69, 0x63};
    private static final byte[] DICOM_HEADER = {0x44, 0x49, 0x43, 0x4D, 0x02, 0x00, 0x00, 0x00};

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
    void isValidImageSignature_ShouldReturnTrue_ForValidGif() {
        // Arrange
        ByteArrayInputStream inputStream = new ByteArrayInputStream(GIF_HEADER);

        // Act
        boolean result = FileSignatureValidator.isValidImageSignature(inputStream, "image/gif");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isValidImageSignature_ShouldReturnTrue_ForValidTiff() {
        // Arrange
        ByteArrayInputStream inputStream = new ByteArrayInputStream(TIFF_HEADER_LE);

        // Act
        boolean result = FileSignatureValidator.isValidImageSignature(inputStream, "image/tiff");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isValidImageSignature_ShouldReturnTrue_ForValidBmp() {
        // Arrange
        ByteArrayInputStream inputStream = new ByteArrayInputStream(BMP_HEADER);

        // Act
        boolean result = FileSignatureValidator.isValidImageSignature(inputStream, "image/bmp");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isValidImageSignature_ShouldReturnTrue_ForValidHif() {
        // Arrange
        ByteArrayInputStream inputStream = new ByteArrayInputStream(HIF_HEADER);

        // Act
        boolean result = FileSignatureValidator.isValidImageSignature(inputStream, "image/hif");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isValidImageSignature_ShouldReturnTrue_ForValidDicom() {
        // Arrange
        ByteArrayInputStream inputStream = new ByteArrayInputStream(DICOM_HEADER);

        // Act
        boolean result = FileSignatureValidator.isValidImageSignature(inputStream, "application/dicom");

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
        boolean result = FileSignatureValidator.isValidImageSignature(inputStream, "image/webp");

        // Assert
        assertThat(result).isFalse();
    }
}