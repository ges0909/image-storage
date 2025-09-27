package com.valantic.sti.image.service;

import com.valantic.sti.image.ImageProperties;
import com.valantic.sti.image.exception.ImageNotFoundException;
import com.valantic.sti.image.model.ImageSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;

import static com.valantic.sti.image.testutil.TestConstants.NON_EXISTENT_ID;
import static com.valantic.sti.image.testutil.TestConstants.TEST_IMAGE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageServiceValidationTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private MultipartFile multipartFile;

    private ImageService imageService;

    @BeforeEach
    void setUp() {
        ImageProperties imageProperties = new ImageProperties(
            "test-bucket",
            "test-thumbnails",
            "test-kms-key",
            "https://cdn.example.com",
            10485760L,
            "eu-central-1",
            15,
            new int[]{150, 300, 600},
            java.util.Set.of("image/jpeg", "image/png", "image/webp"),
            1000,
            "images"
        );
        imageService = new StandardImageService(s3Client, s3Presigner, imageProperties);
    }

    @Test
    void uploadImage_ShouldThrowException_WhenFileEmpty() {
        // Arrange
        when(multipartFile.isEmpty()).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> imageService.uploadImage(multipartFile, "Title", "Desc", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("File cannot be empty");
    }

    @Test
    void uploadImage_ShouldThrowException_WhenInvalidContentType() {
        // Arrange
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("text/plain");

        // Act & Assert
        assertThatThrownBy(() -> imageService.uploadImage(multipartFile, "Title", "Desc", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid image type: text/plain");
    }

    @Test
    void uploadImage_ShouldThrowException_WhenFileTooLarge() {
        // Arrange
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(multipartFile.getSize()).thenReturn(20971520L); // 20MB

        // Act & Assert
        assertThatThrownBy(() -> imageService.uploadImage(multipartFile, "Title", "Desc", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("File too large: 20971520");
    }

    @Test
    void generateSignedUrl_ShouldThrowException_WhenImageNotFound() {
        // Arrange
        String imageId = NON_EXISTENT_ID;
        when(s3Client.headObject(any(HeadObjectRequest.class)))
            .thenThrow(NoSuchKeyException.builder().build());

        // Act & Assert
        assertThatThrownBy(() -> imageService.generateSignedUrl(imageId, ImageSize.ORIGINAL, Duration.ofMinutes(10)))
            .isInstanceOf(ImageNotFoundException.class)
            .hasMessage("Image not found: " + imageId);
    }

    @Test
    void generateSignedUrl_ShouldThrowException_WhenExpirationTooLong() {
        // Arrange
        String imageId = TEST_IMAGE_ID;
        Duration expiration = Duration.ofMinutes(20); // > 15 minutes

        // Act & Assert
        assertThatThrownBy(() -> imageService.generateSignedUrl(imageId, ImageSize.ORIGINAL, expiration))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Expiration cannot exceed 15 minutes for security");
    }

    @Test
    void getThumbnailUrl_ShouldThrowException_WhenOriginalSize() {
        // Arrange
        String imageId = TEST_IMAGE_ID;
        ImageSize size = ImageSize.ORIGINAL;

        // Act & Assert
        assertThatThrownBy(() -> imageService.getThumbnailUrl(imageId, size))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Use generateSignedUrl for original images");
    }

    @Test
    void getThumbnailUrl_ShouldReturnCloudFrontUrl_WhenValidSize() {
        // Arrange
        String imageId = TEST_IMAGE_ID;
        ImageSize size = ImageSize.THUMBNAIL_300;

        // Act
        String result = imageService.getThumbnailUrl(imageId, size);

        // Assert
        assertThat(result).isEqualTo("https://cdn.example.com/images/" + TEST_IMAGE_ID + "/thumbnail_300");
    }
}
