package com.valantic.sti.image.health;

import com.valantic.sti.image.ImageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3HealthIndicatorTest {

    @Mock
    private S3Client s3Client;

    private S3HealthIndicator healthIndicator;
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
        healthIndicator = new S3HealthIndicator(s3Client, imageProperties);
    }

    @Test
    void health_ShouldReturnUp_WhenS3BucketAccessible() {
        // Arrange
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
            .thenReturn(HeadBucketResponse.builder().build());

        // Act
        Health health = healthIndicator.health();

        // Assert
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("bucket", "test-bucket");
        assertThat(health.getDetails()).containsEntry("status", "accessible");
    }

    @Test
    void health_ShouldReturnDown_WhenS3BucketNotAccessible() {
        // Arrange
        S3Exception s3Exception = (S3Exception) S3Exception.builder()
            .message("Access denied")
            .statusCode(403)
            .build();
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
            .thenThrow(s3Exception);

        // Act
        Health health = healthIndicator.health();

        // Assert
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("bucket", "test-bucket");
        assertThat(health.getDetails()).containsEntry("error", "S3 bucket not accessible");
        assertThat(health.getDetails()).containsEntry("message", "Access denied");
    }

    @Test
    void health_ShouldReturnDown_WhenUnexpectedError() {
        // Arrange
        RuntimeException exception = new RuntimeException("Network error");
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
            .thenThrow(exception);

        // Act
        Health health = healthIndicator.health();

        // Assert
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("bucket", "test-bucket");
        assertThat(health.getDetails()).containsEntry("error", "S3 bucket not accessible");
        assertThat(health.getDetails()).containsEntry("message", "Network error");
    }
}