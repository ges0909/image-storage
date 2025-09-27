package com.valantic.sti.image.service;

import com.valantic.sti.image.ImageProperties;
import com.valantic.sti.image.model.ImageSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static com.valantic.sti.image.testutil.TestConstants.TEST_IMAGE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private MultipartFile multipartFile;

    private ImageService imageService;
    private ImageProperties imageProperties;

    @BeforeEach
    void setUp() {
        imageProperties = new ImageProperties(
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
    void uploadImage_ShouldThrowException_WhenImageProcessingFails() throws Exception {
        // Arrange
        byte[] invalidImageData = "not-an-image".getBytes(StandardCharsets.UTF_8);
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getBytes()).thenReturn(invalidImageData);

        // Act & Assert
        assertThatThrownBy(() -> imageService.uploadImage(multipartFile, "Test Image", "Description", List.of("tag1")))
            .isInstanceOf(Exception.class);
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
    void generateSignedUrl_ShouldReturnUrl_WhenValidRequest() throws Exception {
        // Arrange
        String imageId = TEST_IMAGE_ID;
        ImageSize size = ImageSize.ORIGINAL;
        Duration expiration = Duration.ofMinutes(10);

        when(s3Client.headObject(any(HeadObjectRequest.class)))
            .thenReturn(HeadObjectResponse.builder().build());

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(URI.create("https://example.com/signed-url").toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
            .thenReturn(presignedRequest);

        // Act
        String result = imageService.generateSignedUrl(imageId, size, expiration);

        // Assert
        assertThat(result).isEqualTo("https://example.com/signed-url");
        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void deleteImage_ShouldDeleteAllVersions_WhenImageExists() {
        // Arrange
        String imageId = TEST_IMAGE_ID;
        when(s3Client.headObject(any(HeadObjectRequest.class)))
            .thenReturn(HeadObjectResponse.builder().build());
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class)))
            .thenReturn(DeleteObjectsResponse.builder().build());

        // Act
        imageService.deleteImage(imageId);

        // Assert
        verify(s3Client).deleteObjects(any(DeleteObjectsRequest.class));
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
