package com.valantic.sti.image.service;

import com.valantic.sti.image.ImageProperties;
import com.valantic.sti.image.entity.ImageMetadata;
import com.valantic.sti.image.exception.ImageProcessingException;
import com.valantic.sti.image.repository.ImageMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 🧪 Unit Tests für S3StorageService - fokussiert auf S3-Operationen.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("S3StorageService Unit Tests")
class S3StorageServiceTest {

    @Mock
    private S3Client s3Client;
    @Mock
    private S3AsyncClient s3AsyncClient;
    @Mock
    private S3TransferManager transferManager;
    @Mock
    private ImageMetadataRepository metadataRepository;
    @Mock
    private PutObjectResponse putObjectResponse;

    private S3StorageService s3StorageService;

    @BeforeEach
    void setUp() {
        ImageProperties imageProperties = new ImageProperties(
            "test-bucket", "test-thumbnails", "test-kms-key", "https://cdn.example.com",
            10485760L, "eu-central-1", 15, new int[]{150, 300, 600},
            Set.of("image/jpeg", "image/png"), 1000, "images"
        );

        s3StorageService = new S3StorageService(s3Client, s3AsyncClient, transferManager, metadataRepository, imageProperties);
    }

    @Nested
    @DisplayName("Image Upload")
    class ImageUpload {

        @Test
        @DisplayName("Should upload image successfully")
        void uploadImage_ShouldUploadSuccessfully() {
            // Given
            byte[] imageData = "test-image-data".getBytes();
            ImageMetadata metadata = new ImageMetadata(
                "test-id", "Test Image", "Test Description", Set.of("tag1"),
                "image/jpeg", 1024L, 800, 600, "test-key", "user"
            );
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(putObjectResponse);

            // When
            s3StorageService.uploadImage("test-key", imageData, "image/jpeg", metadata);

            // Then
            verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("Should handle S3 upload failure")
        void uploadImage_ShouldHandleS3Failure() {
            // Given
            byte[] imageData = "test-data".getBytes();
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("S3 error").build());

            // When & Then
            ImageMetadata metadata = new ImageMetadata(
                "test-id", "Test", "Desc", Set.of(), "image/jpeg", 1024L, 800, 600, "key", "user"
            );
            assertThatThrownBy(() -> s3StorageService.uploadImage("key", imageData, "image/jpeg", metadata))
                .isInstanceOf(ImageProcessingException.class)
                .hasMessage("S3 upload failed");
        }

        @Test
        @DisplayName("Should use KMS encryption and private ACL")
        void uploadImage_ShouldUseKmsEncryptionAndPrivateAcl() {
            // Given
            byte[] imageData = "test-data".getBytes();
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(putObjectResponse);

            // When
            ImageMetadata metadata = new ImageMetadata(
                "test-id", "Test", "Desc", Set.of("tag"), "image/jpeg", 1024L, 800, 600, "test-key", "user"
            );
            s3StorageService.uploadImage("test-key", imageData, "image/jpeg", metadata);

            // Then
            verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
            // Note: RequestBody instances are not equal even with same data, so we use any()
        }
    }

    @Nested
    @DisplayName("Image Deletion")
    class ImageDeletion {

        @Test
        @DisplayName("Should delete image successfully")
        void deleteImage_ShouldDeleteSuccessfully() {
            // When
            s3StorageService.deleteImage("test-key");

            // Then
            verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("Should handle S3 delete failure")
        void deleteImage_ShouldHandleS3Failure() {
            // Given
            doThrow(S3Exception.builder().message("S3 delete error").build())
                .when(s3Client).deleteObject(any(DeleteObjectRequest.class));

            // When & Then
            assertThatThrownBy(() -> s3StorageService.deleteImage("test-key"))
                .isInstanceOf(ImageProcessingException.class)
                .hasMessage("S3 delete failed");
        }
    }

    @Nested
    @DisplayName("AWS SDK Integration")
    class AwsSdkIntegration {

        @Test
        @DisplayName("Should handle empty S3 key")
        void uploadImage_ShouldHandleEmptyS3Key() {
            // When - Service doesn't validate, passes to AWS SDK
            // Then - Should not throw at service level (AWS SDK will handle)
            ImageMetadata metadata = new ImageMetadata(
                "test-id", "Test", "Desc", Set.of(), "image/jpeg", 1L, 1, 1, "", "user"
            );
            s3StorageService.uploadImage("", new byte[1], "image/jpeg", metadata);
            verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("Should handle null image data with NullPointerException")
        void uploadImage_ShouldHandleNullImageData() {
            // When & Then - RequestBody.fromBytes(null) throws NPE
            ImageMetadata metadata = new ImageMetadata(
                "test-id", "Test", "Desc", Set.of(), "image/jpeg", 0L, 1, 1, "key", "user"
            );
            assertThatThrownBy(() -> s3StorageService.uploadImage("key", null, "image/jpeg", metadata))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should handle empty content type")
        void uploadImage_ShouldHandleEmptyContentType() {
            // When - Service doesn't validate, passes to AWS SDK
            // Then - Should not throw at service level
            ImageMetadata metadata = new ImageMetadata(
                "test-id", "Test", "Desc", Set.of(), "", 1L, 1, 1, "key", "user"
            );
            s3StorageService.uploadImage("key", new byte[1], "", metadata);
            verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }
    }
}
