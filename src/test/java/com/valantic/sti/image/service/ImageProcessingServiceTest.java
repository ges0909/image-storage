package com.valantic.sti.image.service;

import com.valantic.sti.image.ImageProperties;
import com.valantic.sti.image.exception.ImageProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ImageProcessingService - focused on image processing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ImageProcessingService Unit Tests")
class ImageProcessingServiceTest {

    @Mock
    private S3StorageService s3StorageService;

    private ImageProcessingService imageProcessingService;

    // Test-Konstanten für bessere Lesbarkeit
    private static final String TEST_IMAGE_ID = "test-image-id";
    private static final byte[] TEST_IMAGE_DATA = "test-image-data".getBytes();
    private static final String JPEG_CONTENT_TYPE = "image/jpeg";

    @BeforeEach
    void setUp() {
        ImageProperties imageProperties = new ImageProperties(
            "test-bucket", "test-thumbnails", "test-kms-key", "https://cdn.example.com",
            10485760L, "eu-central-1", 15, new int[]{150, 300, 600},
            Set.of("image/jpeg", "image/png"), 1000, "images"
        );

        imageProcessingService = new ImageProcessingService(imageProperties, s3StorageService);
    }

    @Nested
    @DisplayName("Image Dimensions")
    class ImageDimensionsExtraction {

        @Test
        @DisplayName("Should fail to extract dimensions from invalid JPEG")
        void getImageDimensions_ShouldFailWithInvalidJpeg() {
            // Given - Create a minimal JPEG header that's still invalid
            byte[] jpegHeader = createMinimalJpegData();

            // When & Then - Even minimal JPEG fails because it's not complete
            assertThatThrownBy(() -> imageProcessingService.getImageDimensions(jpegHeader))
                .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("Should handle invalid image data with ImageProcessingException")
        void getImageDimensions_ShouldHandleInvalidData() {
            // Given
            byte[] invalidData = "not-an-image".getBytes();

            // When & Then - Service throws ImageProcessingException with "Invalid image format"
            assertThatThrownBy(() -> imageProcessingService.getImageDimensions(invalidData))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid image format or no image data present");
        }

        @Test
        @DisplayName("Should handle null image data with NullPointerException")
        void getImageDimensions_ShouldHandleNullData() {
            // When & Then - ByteArrayInputStream throws NPE for null data
            assertThatThrownBy(() -> imageProcessingService.getImageDimensions(null))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should handle empty image data with ImageProcessingException")
        void getImageDimensions_ShouldHandleEmptyData() {
            // When & Then - Empty data causes "Invalid image format" error
            assertThatThrownBy(() -> imageProcessingService.getImageDimensions(new byte[0]))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid image format or no image data present");
        }
    }

    @Nested
    @DisplayName("Thumbnail Generation")
    class ThumbnailGeneration {

        @Test
        @DisplayName("Should fail with WebP format not supported")
        void generateThumbnails_ShouldFailWithWebPNotSupported() {
            // When & Then - Thumbnailator doesn't support WebP output
            assertThatThrownBy(() -> imageProcessingService.generateThumbnails(TEST_IMAGE_ID, TEST_IMAGE_DATA, JPEG_CONTENT_TYPE))
                .isInstanceOf(ImageProcessingException.class)
                .hasMessage("Invalid image data for thumbnail generation");
        }

        @Test
        @DisplayName("Should handle empty image ID without validation")
        void generateThumbnails_ShouldHandleEmptyImageId() {
            // When & Then - Service doesn't validate, goes to thumbnail generation
            assertThatThrownBy(() -> imageProcessingService.generateThumbnails("", new byte[1], "image/jpeg"))
                .isInstanceOf(ImageProcessingException.class)
                .hasMessage("Invalid image data for thumbnail generation");
        }

        @Test
        @DisplayName("Should handle null image data with NullPointerException")
        void generateThumbnails_ShouldHandleNullImageData() {
            // When & Then - ByteArrayInputStream throws NPE for null data
            assertThatThrownBy(() -> imageProcessingService.generateThumbnails("id", null, "image/jpeg"))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should handle empty content type without validation")
        void generateThumbnails_ShouldHandleEmptyContentType() {
            // When & Then - Service doesn't validate, goes to thumbnail generation
            assertThatThrownBy(() -> imageProcessingService.generateThumbnails("id", new byte[1], ""))
                .isInstanceOf(ImageProcessingException.class)
                .hasMessage("Invalid image data for thumbnail generation");
        }

        @Test
        @DisplayName("Should handle any format with WebP output error")
        void generateThumbnails_ShouldHandleAnyFormat() {
            // Given
            byte[] imageData = "test-data".getBytes();

            // When & Then - All formats fail because WebP output is not supported
            assertThatThrownBy(() -> imageProcessingService.generateThumbnails("id", imageData, "image/bmp"))
                .isInstanceOf(ImageProcessingException.class)
                .hasMessage("Invalid image data for thumbnail generation");
        }
    }

    @Nested
    @DisplayName("Image Format Support")
    class ImageFormatSupport {

        @Test
        @DisplayName("Should support JPEG format")
        void shouldSupportJpegFormat() {
            // This would test format detection logic
            assertThat(imageProcessingService.isValidImageFormat("image/jpeg")).isTrue();
        }

        @Test
        @DisplayName("Should support PNG format")
        void shouldSupportPngFormat() {
            assertThat(imageProcessingService.isValidImageFormat("image/png")).isTrue();
        }

        @Test
        @DisplayName("Should not support unsupported format")
        void shouldNotSupportUnsupportedFormat() {
            assertThat(imageProcessingService.isValidImageFormat("image/bmp")).isFalse();
        }
    }

    // Helper Methods
    private byte[] createMinimalJpegData() {
        // Create minimal JPEG header for testing
        return new byte[]{
            (byte) 0xFF, (byte) 0xD8, // JPEG SOI marker
            (byte) 0xFF, (byte) 0xE0, // JFIF marker
            0x00, 0x10, // Length
            'J', 'F', 'I', 'F', 0x00, // JFIF identifier
            0x01, 0x01, // Version
            0x01, // Units
            0x00, 0x48, 0x00, 0x48, // X and Y density
            0x00, 0x00, // Thumbnail dimensions
            (byte) 0xFF, (byte) 0xD9 // JPEG EOI marker
        };
    }
}
