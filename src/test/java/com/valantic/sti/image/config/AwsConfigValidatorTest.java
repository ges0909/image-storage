package com.valantic.sti.image.config;

import com.valantic.sti.image.ImageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AwsConfigValidatorTest {

    @Mock
    private S3Client s3Client;

    private AwsConfigValidator awsConfigValidator;

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
            java.util.Set.of("image/jpeg", "image/png"),
            1000,
            "images"
        );
        awsConfigValidator = new AwsConfigValidator(s3Client, imageProperties);
    }

    @Test
    void validateAwsConfiguration_ShouldThrowException_WhenBucketNotAccessible() {
        // Arrange
        S3Exception s3Exception = (S3Exception) S3Exception.builder()
            .message("Access denied")
            .statusCode(403)
            .build();
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
            .thenThrow(s3Exception);

        // Act & Assert
        assertThatThrownBy(() -> awsConfigValidator.validateAwsConfiguration())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Invalid AWS configuration");
    }
}
