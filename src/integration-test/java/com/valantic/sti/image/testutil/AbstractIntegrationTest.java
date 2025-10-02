package com.valantic.sti.image.testutil;

import com.valantic.sti.image.ImageProperties;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.net.URI;

import static com.valantic.sti.image.testutil.TestConstants.*;

@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    protected static GenericContainer<?> minio = new GenericContainer<>(DockerImageName.parse("minio/minio:latest"))
        .withExposedPorts(9000)
        .withEnv("MINIO_ROOT_USER", "testuser")
        .withEnv("MINIO_ROOT_PASSWORD", "testpassword123")
        .withCommand("server", "/data")
        .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // AWS SDK standard properties
        registry.add("aws.accessKeyId", () -> "testuser");
        registry.add("aws.secretAccessKey", () -> "testpassword123");
        registry.add("aws.region", () -> "us-east-1");

        // MinIO S3 endpoint
        registry.add("aws.s3.endpoint", () -> "http://localhost:" + minio.getMappedPort(9000));

        // Disable AWS config validation for tests
        registry.add("aws.config.validation.enabled", () -> "false");

        // Image service properties
        registry.add("image.bucket-name", () -> TEST_BUCKET);
        registry.add("image.thumbnail-bucket-name", () -> TEST_THUMBNAILS_BUCKET);
        registry.add("image.cloudfront-domain", () -> TEST_CLOUDFRONT_DOMAIN);
        registry.add("image.kms-key-id", () -> TEST_KMS_KEY);
        registry.add("image.key-prefix", () -> TEST_KEY_PREFIX);
    }

    @BeforeAll
    static void createTestBuckets() {
        var credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("testuser", "testpassword123")
        );
        var region = Region.US_EAST_1;

        try (S3Client s3Client = S3Client.builder()
            .endpointOverride(URI.create("http://localhost:" + minio.getMappedPort(9000)))
            .credentialsProvider(credentialsProvider)
            .region(region)
            .forcePathStyle(true)
            .build()) {

            // Create buckets that the application expects
            s3Client.createBucket(CreateBucketRequest.builder().bucket(TEST_BUCKET).build());
            s3Client.createBucket(CreateBucketRequest.builder().bucket(TEST_THUMBNAILS_BUCKET).build());
            s3Client.createBucket(CreateBucketRequest.builder().bucket("dev-images-bucket").build());
        }
    }

    // Shared helper method for creating ImageProperties
    protected ImageProperties createTestImageProperties() {
        return new ImageProperties(
            TEST_BUCKET,
            TEST_THUMBNAILS_BUCKET,
            TEST_KMS_KEY,
            TEST_CLOUDFRONT_DOMAIN,
            MAX_FILE_SIZE,
            "us-east-1",
            URL_EXPIRATION_MINUTES,
            THUMBNAIL_SIZES,
            java.util.Set.of("image/jpeg", "image/png", "image/webp"),
            MAX_RESULTS,
            TEST_KEY_PREFIX
        );
    }
}
